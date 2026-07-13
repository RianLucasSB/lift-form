from unittest.mock import patch

import pytest

import listener


def test_build_extracted_event_shapes_payload():
    event = listener.build_extracted_event(42, {"knee_bottom_avg": 48.0})

    assert event == {"videoAnalysisId": 42, "features": {"knee_bottom_avg": 48.0}}


def test_build_pipeline_error_payload_shapes_payload():
    try:
        raise RuntimeError("boom")
    except RuntimeError as exc:
        payload = listener.build_pipeline_error_payload(42, "EXTRACTION", exc)

    assert payload["analysisId"] == 42
    assert payload["stage"] == "EXTRACTION"
    assert payload["errorMessage"] == "boom"
    assert "RuntimeError" in payload["stackTrace"]
    assert "boom" in payload["stackTrace"]


@patch("listener.os.unlink")
@patch("listener.get_squat_bottoms_multiple_reps")
@patch("listener.download_video_from_s3")
def test_process_message_raises_when_no_reps_detected(mock_download, mock_extract, mock_unlink):
    mock_download.return_value = "/tmp/fake-video.mp4"
    mock_extract.return_value = ([], None, [], None, None, None, None)

    with pytest.raises(RuntimeError):
        listener.process_message({"videoAnalysisId": 1, "s3Key": "videos/squat/user/video.mp4"})

    mock_unlink.assert_called_once_with("/tmp/fake-video.mp4")


@patch("listener.os.unlink")
@patch("listener.get_squat_bottoms_multiple_reps")
@patch("listener.download_video_from_s3")
def test_process_message_returns_features_on_success(mock_download, mock_extract, mock_unlink):
    mock_download.return_value = "/tmp/fake-video.mp4"
    features = {"knee_bottom_avg": 48.0}
    mock_extract.return_value = ([{"rep": 1}], features, [{"rep": 1}], None, None, None, None)

    result = listener.process_message({"videoAnalysisId": 1, "s3Key": "videos/squat/user/video.mp4"})

    assert result == features
    mock_unlink.assert_called_once_with("/tmp/fake-video.mp4")
