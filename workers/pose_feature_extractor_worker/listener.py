from dotenv import load_dotenv
load_dotenv()

import pika
import json
import time
import os

from s3 import download_video_from_s3
from worker import get_squat_bottoms_multiple_reps


RABBITMQ_URL = os.getenv("RABBITMQ_URL", "amqp://guest:guest@localhost")

EXCHANGE = "video-analysis.exchange"
QUEUE    = "video-analysis.queue"
ROUTING  = "video-analysis.uploaded"
DLQ      = "video-analysis.dlq"
DLX      = "video-analysis.dlx"

MAX_RETRIES = 3


def get_death_count(properties) -> int:
    headers = properties.headers or {}
    x_death = headers.get("x-death", [])
    if x_death:
        return int(x_death[0].get("count", 0))
    return 0


def process_message(body: dict):
    video_analysis_id = body["videoAnalysisId"]
    s3_key = body["s3Key"]

    print(f"Downloading video for analysis {video_analysis_id} from S3 key: {s3_key}")
    video_path = download_video_from_s3(s3_key)

    try:
        bottoms, features, rep_features_list, *_ = get_squat_bottoms_multiple_reps(video_path)
        print(f"Processed {len(bottoms)} reps for analysis {video_analysis_id}")
        print(f"Features: {json.dumps(features, indent=2)}")
    finally:
        os.unlink(video_path)
        print(f"Cleaned up temp file: {video_path}")


def callback(ch, method, properties, body):
    try:
        data = json.loads(body)
        process_message(data)
        ch.basic_ack(delivery_tag=method.delivery_tag)
        print("Message acknowledged successfully")

    except Exception as e:
        retry_count = get_death_count(properties)
        print(f"Error processing message (attempt {retry_count + 1}/{MAX_RETRIES}): {e}")

        if retry_count >= MAX_RETRIES:
            print("Max retries reached, sending to DLQ")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        else:
            delay = (2 ** retry_count)
            print(f"Retrying in {delay}s...")
            time.sleep(delay)
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)


def main():
    print("Connecting to RabbitMQ...")
    connection = pika.BlockingConnection(pika.URLParameters(RABBITMQ_URL))
    channel = connection.channel()

    channel.exchange_declare(exchange=EXCHANGE, exchange_type="direct", durable=True)
    channel.exchange_declare(exchange=DLX,      exchange_type="direct", durable=True)

    channel.queue_declare(
        queue=QUEUE,
        durable=True,
        arguments={
            "x-dead-letter-exchange":    DLX,
            "x-dead-letter-routing-key": DLQ,
        }
    )
    channel.queue_declare(queue=DLQ, durable=True)

    channel.queue_bind(exchange=EXCHANGE, queue=QUEUE, routing_key=ROUTING)
    channel.queue_bind(exchange=DLX,      queue=DLQ,  routing_key=DLQ)

    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=QUEUE, on_message_callback=callback)

    print("Waiting for messages. To exit press CTRL+C")
    channel.start_consuming()


if __name__ == "__main__":
    main()