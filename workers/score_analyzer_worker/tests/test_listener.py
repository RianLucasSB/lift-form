from unittest.mock import MagicMock

import pytest

import listener


def test_build_finished_payload_shapes_success_message():
    predict_result = {
        "score": 0.82,
        "label": "good",
        "feedback": {"depth": {"rating": "good", "detail": "Good depth"}},
        "features_used": {"knee_bottom_avg": 48.0},
    }

    payload = listener.build_finished_payload(42, "squat-v1", predict_result)

    assert payload["videoAnalysisId"] == 42
    assert payload["modelVersion"] == "squat-v1"
    assert payload["overallScore"] == 0.82
    assert payload["rawFeatures"] == {"knee_bottom_avg": 48.0}
    # Feedback carries the predictor's per-dimension breakdown plus the original 5-level label
    assert payload["feedback"]["depth"] == {"rating": "good", "detail": "Good depth"}
    assert payload["feedback"]["overall_label"] == "good"
    # No classification field - that's a domain rule computed server-side in Java
    assert "classification" not in payload


def test_build_pipeline_error_payload_shapes_payload():
    try:
        raise RuntimeError("boom")
    except RuntimeError as exc:
        payload = listener.build_pipeline_error_payload(42, "SCORING", exc)

    assert payload["analysisId"] == 42
    assert payload["stage"] == "SCORING"
    assert payload["errorMessage"] == "boom"
    assert "RuntimeError" in payload["stackTrace"]
    assert "boom" in payload["stackTrace"]


def test_process_message_returns_predictor_result_on_success():
    predictor = MagicMock()
    predictor.predict.return_value = {
        "score": 0.9, "label": "excellent", "feedback": {}, "features_used": {}
    }

    result = listener.process_message({"videoAnalysisId": 1, "features": {"knee_bottom_avg": 48.0}}, predictor)

    predictor.predict.assert_called_once_with({"knee_bottom_avg": 48.0})
    assert result["score"] == 0.9


def test_process_message_raises_when_predictor_reports_no_data():
    predictor = MagicMock()
    predictor.predict.return_value = {"score": None, "label": "no_data", "reason": "Missing: [...]"}

    with pytest.raises(RuntimeError):
        listener.process_message({"videoAnalysisId": 1, "features": {}}, predictor)
