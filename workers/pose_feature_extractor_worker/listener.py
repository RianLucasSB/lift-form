from dotenv import load_dotenv
load_dotenv()

import pika
import json
import time
import os
import traceback

from s3 import download_video_from_s3
from worker import get_squat_bottoms_multiple_reps


RABBITMQ_URL = os.getenv("RABBITMQ_URL", "amqp://guest:guest@localhost")

EXCHANGE = "video-analysis.exchange"

EXTRACTION_QUEUE = "video-analysis.queue"
EXTRACTION_ROUTING_KEY = "video-analysis.uploaded"
EXTRACTION_DLX = "video-analysis.dlx"
EXTRACTION_DLQ = "video-analysis.dlq"
EXTRACTION_DLQ_ROUTING_KEY = "video-analysis.dlq"

# Scoring leg: this worker only publishes into it, the scorer worker owns/consumes it.
# Declared here too (defensively) so a message published before the scorer has started
# isn't silently dropped by the exchange.
SCORING_QUEUE = "video-analysis.scoring.queue"
SCORING_ROUTING_KEY = "video-analysis.extracted"
SCORING_DLX = "video-analysis.scoring.dlx"
SCORING_DLQ = "video-analysis.scoring.dlq"
SCORING_DLQ_ROUTING_KEY = "video-analysis.scoring.dlq"

MAX_RETRIES = 3

RECONNECT_DELAY = 2
MAX_RECONNECT_DELAY = 30


def build_extracted_event(analysis_id, features):
    return {"videoAnalysisId": analysis_id, "features": features}


def build_pipeline_error_payload(analysis_id, stage, exc):
    return {
        "analysisId": analysis_id,
        "stage": stage,
        "errorMessage": str(exc),
        "stackTrace": "".join(traceback.format_exception(type(exc), exc, exc.__traceback__)),
    }


def process_message(body: dict):
    video_analysis_id = body["videoAnalysisId"]
    s3_key = body["s3Key"]

    print(f"Downloading video for analysis {video_analysis_id} from S3 key: {s3_key}")
    video_path = download_video_from_s3(s3_key)

    try:
        bottoms, features, rep_features_list, *_ = get_squat_bottoms_multiple_reps(video_path)
        if features is None:
            raise RuntimeError(f"No valid reps detected for analysis {video_analysis_id}")
        print(f"Processed {len(bottoms)} reps for analysis {video_analysis_id}")
        print(f"Features: {json.dumps(features, indent=2)}")
        return features
    finally:
        os.unlink(video_path)
        print(f"Cleaned up temp file: {video_path}")


def publish_extracted_event(channel, analysis_id, features):
    channel.basic_publish(
        exchange=EXCHANGE,
        routing_key=SCORING_ROUTING_KEY,
        body=json.dumps(build_extracted_event(analysis_id, features)),
        properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
    )


def publish_pipeline_error(channel, analysis_id, exc):
    channel.basic_publish(
        exchange=EXTRACTION_DLX,
        routing_key=EXTRACTION_DLQ_ROUTING_KEY,
        body=json.dumps(build_pipeline_error_payload(analysis_id, "EXTRACTION", exc)),
        properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
    )


def callback(ch, method, properties, body):
    try:
        data = json.loads(body)
        video_analysis_id = data["videoAnalysisId"]
    except (json.JSONDecodeError, KeyError, TypeError) as e:
        print(f"Dropping malformed message, cannot attribute it to an analysis: {e}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        return

    for attempt in range(1, MAX_RETRIES + 1):
        try:
            features = process_message(data)
            publish_extracted_event(ch, video_analysis_id, features)
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print("Message acknowledged successfully")
            return
        except Exception as e:
            print(f"Error processing message (attempt {attempt}/{MAX_RETRIES}): {e}")

            if attempt >= MAX_RETRIES:
                print("Max retries reached, publishing structured error to DLQ")
                publish_pipeline_error(ch, video_analysis_id, e)
                ch.basic_ack(delivery_tag=method.delivery_tag)
                return

            delay = 2 ** (attempt - 1)
            print(f"Retrying in {delay}s...")
            time.sleep(delay)


def declare_topology(channel):
    channel.exchange_declare(exchange=EXCHANGE, exchange_type="direct", durable=True)
    channel.exchange_declare(exchange=EXTRACTION_DLX, exchange_type="direct", durable=True)

    channel.queue_declare(
        queue=EXTRACTION_QUEUE,
        durable=True,
        arguments={
            "x-dead-letter-exchange":    EXTRACTION_DLX,
            "x-dead-letter-routing-key": EXTRACTION_DLQ_ROUTING_KEY,
        }
    )
    channel.queue_declare(queue=EXTRACTION_DLQ, durable=True)

    channel.queue_bind(exchange=EXCHANGE, queue=EXTRACTION_QUEUE, routing_key=EXTRACTION_ROUTING_KEY)
    channel.queue_bind(exchange=EXTRACTION_DLX, queue=EXTRACTION_DLQ, routing_key=EXTRACTION_DLQ_ROUTING_KEY)

    channel.exchange_declare(exchange=SCORING_DLX, exchange_type="direct", durable=True)
    channel.queue_declare(
        queue=SCORING_QUEUE,
        durable=True,
        arguments={
            "x-dead-letter-exchange":    SCORING_DLX,
            "x-dead-letter-routing-key": SCORING_DLQ_ROUTING_KEY,
        }
    )
    channel.queue_declare(queue=SCORING_DLQ, durable=True)
    channel.queue_bind(exchange=EXCHANGE, queue=SCORING_QUEUE, routing_key=SCORING_ROUTING_KEY)
    channel.queue_bind(exchange=SCORING_DLX, queue=SCORING_DLQ, routing_key=SCORING_DLQ_ROUTING_KEY)


def main():
    delay = RECONNECT_DELAY

    while True:
        try:
            print("Connecting to RabbitMQ URL: " + RABBITMQ_URL)
            connection = pika.BlockingConnection(pika.URLParameters(RABBITMQ_URL))
        except (pika.exceptions.AMQPConnectionError, OSError) as e:
            print(f"Could not connect to RabbitMQ ({e}). Retrying in {delay}s...")
            time.sleep(delay)
            delay = min(delay * 2, MAX_RECONNECT_DELAY)
            continue

        delay = RECONNECT_DELAY

        try:
            channel = connection.channel()
            declare_topology(channel)
            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(queue=EXTRACTION_QUEUE, on_message_callback=callback)

            print("Waiting for messages. To exit press CTRL+C")
            channel.start_consuming()
            break
        except (pika.exceptions.AMQPConnectionError, OSError) as e:
            print(f"Lost connection to RabbitMQ ({e}). Reconnecting in {delay}s...")
            time.sleep(delay)
            delay = min(delay * 2, MAX_RECONNECT_DELAY)
        except KeyboardInterrupt:
            print("Interrupted, shutting down")
            break
        finally:
            if connection.is_open:
                connection.close()


if __name__ == "__main__":
    main()
