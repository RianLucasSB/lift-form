import boto3
import os
import tempfile


def get_s3_client():
    endpoint_url = os.getenv("AWS_ENDPOINT_URL")
    return boto3.client(
        "s3",
        region_name=os.getenv("AWS_REGION", "us-east-1"),
        endpoint_url=endpoint_url,
        aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID", None),
        aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY", None),
    )


def download_video_from_s3(s3_key: str) -> str:
    bucket = os.getenv("AWS_S3_BUCKET", "liftform-bucket")
    client = get_s3_client()

    tmp = tempfile.NamedTemporaryFile(suffix=".mp4", delete=False)
    client.download_fileobj(bucket, s3_key, tmp)
    tmp.close()
    return tmp.name