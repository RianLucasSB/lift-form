from dotenv import load_dotenv
load_dotenv()

import pika
import json
import time
import os
import traceback

from inference.squat_predictor import SquatScorePredictor


RABBITMQ_URL = os.getenv("RABBITMQ_URL", "amqp://guest:guest@localhost")
MODEL_VERSION = os.getenv("MODEL_VERSION", "squat-v1")

EXCHANGE = "video-analysis.exchange"

SCORING_QUEUE = "video-analysis.scoring.queue"
SCORING_ROUTING_KEY = "video-analysis.extracted"
SCORING_DLX = "video-analysis.scoring.dlx"
SCORING_DLQ = "video-analysis.scoring.dlq"
SCORING_DLQ_ROUTING_KEY = "video-analysis.scoring.dlq"

# Results leg: this worker only publishes into it, the API owns/consumes it. Declared
# here too (defensively) so a message published before the API listener has started
# isn't silently dropped by the exchange.
RESULTS_QUEUE = "video-analysis.results.queue"
RESULTS_ROUTING_KEY = "video-analysis.finished"
RESULTS_DLX = "video-analysis.results.dlx"
RESULTS_DLQ = "video-analysis.results.dlq"
RESULTS_DLQ_ROUTING_KEY = "video-analysis.results.dlq"

MAX_RETRIES = 3

RECONNECT_DELAY = 2
MAX_RECONNECT_DELAY = 30


def build_finished_payload(analysis_id, model_version, predict_result):
    return {
        "videoAnalysisId": analysis_id,
        "modelVersion": model_version,
        "overallScore": predict_result["score"],
        "feedback": {
            **predict_result["feedback"],
            "overall_label": predict_result["label"],
        },
        "rawFeatures": predict_result["features_used"],
    }


def build_pipeline_error_payload(analysis_id, stage, exc):
    return {
        "analysisId": analysis_id,
        "stage": stage,
        "errorMessage": str(exc),
        "stackTrace": "".join(traceback.format_exception(type(exc), exc, exc.__traceback__)),
    }


def process_message(body: dict, predictor: SquatScorePredictor):
    analysis_id = body["videoAnalysisId"]
    features = body["features"]

    result = predictor.predict(features)
    if result.get("score") is None:
        raise RuntimeError(f"Scoring failed for analysis {analysis_id}: {result.get('reason')}")

    return result


def publish_finished_event(channel, analysis_id, predict_result):
    channel.basic_publish(
        exchange=EXCHANGE,
        routing_key=RESULTS_ROUTING_KEY,
        body=json.dumps(build_finished_payload(analysis_id, MODEL_VERSION, predict_result)),
        properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
    )


def publish_pipeline_error(channel, analysis_id, exc):
    channel.basic_publish(
        exchange=SCORING_DLX,
        routing_key=SCORING_DLQ_ROUTING_KEY,
        body=json.dumps(build_pipeline_error_payload(analysis_id, "SCORING", exc)),
        properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
    )


def make_callback(predictor: SquatScorePredictor):
    def callback(ch, method, properties, body):
        try:
            data = json.loads(body)
            analysis_id = data["videoAnalysisId"]
        except (json.JSONDecodeError, KeyError, TypeError) as e:
            print(f"Dropping malformed message, cannot attribute it to an analysis: {e}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            return

        for attempt in range(1, MAX_RETRIES + 1):
            try:
                result = process_message(data, predictor)
                publish_finished_event(ch, analysis_id, result)
                ch.basic_ack(delivery_tag=method.delivery_tag)
                print("Message acknowledged successfully")
                return
            except Exception as e:
                print(f"Error processing message (attempt {attempt}/{MAX_RETRIES}): {e}")

                if attempt >= MAX_RETRIES:
                    print("Max retries reached, publishing structured error to DLQ")
                    publish_pipeline_error(ch, analysis_id, e)
                    ch.basic_ack(delivery_tag=method.delivery_tag)
                    return

                delay = 2 ** (attempt - 1)
                print(f"Retrying in {delay}s...")
                time.sleep(delay)

    return callback


def declare_topology(channel):
    channel.exchange_declare(exchange=EXCHANGE, exchange_type="direct", durable=True)

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

    channel.exchange_declare(exchange=RESULTS_DLX, exchange_type="direct", durable=True)
    channel.queue_declare(
        queue=RESULTS_QUEUE,
        durable=True,
        arguments={
            "x-dead-letter-exchange":    RESULTS_DLX,
            "x-dead-letter-routing-key": RESULTS_DLQ_ROUTING_KEY,
        }
    )
    channel.queue_declare(queue=RESULTS_DLQ, durable=True)
    channel.queue_bind(exchange=EXCHANGE, queue=RESULTS_QUEUE, routing_key=RESULTS_ROUTING_KEY)
    channel.queue_bind(exchange=RESULTS_DLX, queue=RESULTS_DLQ, routing_key=RESULTS_DLQ_ROUTING_KEY)


def main():
    predictor = SquatScorePredictor()
    callback = make_callback(predictor)

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
            channel.basic_consume(queue=SCORING_QUEUE, on_message_callback=callback)

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
