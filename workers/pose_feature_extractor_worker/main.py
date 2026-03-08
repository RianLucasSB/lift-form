import cv2
import numpy as np
from scipy.signal import find_peaks
import os

import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision


# Pose landmark indices (new API)
class PoseLandmark:
    LEFT_SHOULDER = 11
    RIGHT_SHOULDER = 12
    LEFT_HIP = 23
    LEFT_KNEE = 25
    LEFT_ANKLE = 27
    RIGHT_HIP = 24
    RIGHT_KNEE = 26
    RIGHT_ANKLE = 28


def calculate_angle(a, b, c):
    """
    Angle (in degrees) at point b given three 2D points a, b, c.
    """
    a = np.array(a)
    b = np.array(b)
    c = np.array(c)

    ba = a - b
    bc = c - b

    cosine_angle = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc))
    cosine_angle = np.clip(cosine_angle, -1.0, 1.0)
    angle = np.degrees(np.arccos(cosine_angle))
    return angle


def calculate_back_angle(shoulder, hip):
    """
    Calcula o ângulo de inclinação do tronco em relação à vertical.
    0° = totalmente ereto, 90° = totalmente inclinado para frente.
    Usa coordenadas de imagem onde Y cresce para baixo.
    """
    shoulder = np.array(shoulder)
    hip = np.array(hip)

    # Vetor do tronco (quadril -> ombro)
    trunk = shoulder - hip

    # Vetor vertical (apontando para cima em coordenadas de imagem, Y cresce para baixo)
    vertical = np.array([0, -1])

    cosine_angle = np.dot(trunk, vertical) / (np.linalg.norm(trunk) * np.linalg.norm(vertical) + 1e-6)
    cosine_angle = np.clip(cosine_angle, -1.0, 1.0)
    angle = np.degrees(np.arccos(cosine_angle))
    return angle


def smooth_signal(x, window_size=5):
    if len(x) < window_size:
        return x
    cumsum = np.cumsum(np.insert(x, 0, 0))
    smoothed = (cumsum[window_size:] - cumsum[:-window_size]) / float(window_size)
    # pad to original length
    pad = [smoothed[0]] * (window_size - 1)
    return np.concatenate([pad, smoothed])


def detect_best_leg(landmarks, w, h):
    """
    Detecta qual perna está mais visível baseado no score de visibilidade.
    Retorna True para esquerda, False para direita.
    """
    left_visibility = (
        landmarks[PoseLandmark.LEFT_HIP].visibility +
        landmarks[PoseLandmark.LEFT_KNEE].visibility +
        landmarks[PoseLandmark.LEFT_ANKLE].visibility
    )
    right_visibility = (
        landmarks[PoseLandmark.RIGHT_HIP].visibility +
        landmarks[PoseLandmark.RIGHT_KNEE].visibility +
        landmarks[PoseLandmark.RIGHT_ANKLE].visibility
    )
    return left_visibility >= right_visibility


def get_squat_bottoms_multiple_reps(
    video_path,
    model_path="pose_landmarker.task",
    use_left_leg=None,
    max_knee_angle=140.0,
    min_depth=15.0,
    save_frames=True,
    output_dir="."
):
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")

    frame_angles = []  # list of (frame_index, knee_angle, hip_angle, back_angle)
    auto_detected_side = None

    # Configure PoseLandmarker
    base_options = python.BaseOptions(model_asset_path=model_path)
    options = vision.PoseLandmarkerOptions(
        base_options=base_options,
        running_mode=vision.RunningMode.VIDEO,
        num_poses=1,
        min_pose_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    )

    with vision.PoseLandmarker.create_from_options(options) as landmarker:
        frame_index = 0
        fps = cap.get(cv2.CAP_PROP_FPS)
        if fps <= 0:
            fps = 30  # fallback

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            h, w, _ = frame.shape

            # Convert BGR to RGB
            image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=image_rgb)

            # Calculate timestamp in milliseconds
            timestamp_ms = int(frame_index * 1000 / fps)

            # Detect pose
            results = landmarker.detect_for_video(mp_image, timestamp_ms)

            if results.pose_landmarks and len(results.pose_landmarks) > 0:
                lm = results.pose_landmarks[0]

                # Auto-detectar lado no primeiro frame com pose detectada
                if use_left_leg is None and auto_detected_side is None:
                    auto_detected_side = detect_best_leg(lm, w, h)
                    side_name = "esquerda" if auto_detected_side else "direita"
                    print(f"Auto-detectado: usando perna {side_name}")

                # Usa o lado especificado ou o auto-detectado
                current_side = use_left_leg if use_left_leg is not None else auto_detected_side

                if current_side:
                    shoulder = lm[PoseLandmark.LEFT_SHOULDER]
                    hip = lm[PoseLandmark.LEFT_HIP]
                    knee = lm[PoseLandmark.LEFT_KNEE]
                    ankle = lm[PoseLandmark.LEFT_ANKLE]
                else:
                    shoulder = lm[PoseLandmark.RIGHT_SHOULDER]
                    hip = lm[PoseLandmark.RIGHT_HIP]
                    knee = lm[PoseLandmark.RIGHT_KNEE]
                    ankle = lm[PoseLandmark.RIGHT_ANKLE]

                shoulder_xy = (shoulder.x * w, shoulder.y * h)
                hip_xy = (hip.x * w, hip.y * h)
                knee_xy = (knee.x * w, knee.y * h)
                ankle_xy = (ankle.x * w, ankle.y * h)

                # Ângulo do joelho: quadril → joelho → tornozelo
                knee_angle = calculate_angle(hip_xy, knee_xy, ankle_xy)

                # Ângulo do quadril: ombro → quadril → joelho
                hip_angle = calculate_angle(shoulder_xy, hip_xy, knee_xy)

                # Ângulo da lombar: inclinação do tronco em relação à vertical
                back_angle = calculate_back_angle(shoulder_xy, hip_xy)

                frame_angles.append((frame_index, knee_angle, hip_angle, back_angle))

            frame_index += 1

    cap.release()

    if not frame_angles:
        raise RuntimeError("No pose detected in any frame.")

    # Separate indices and angles
    frame_indices = np.array([fa[0] for fa in frame_angles])
    knee_angles = np.array([fa[1] for fa in frame_angles])
    hip_angles = np.array([fa[2] for fa in frame_angles])
    back_angles = np.array([fa[3] for fa in frame_angles])

    # Smooth the angle signals to reduce noise
    smoothed_knee = smooth_signal(knee_angles, window_size=7)
    smoothed_hip = smooth_signal(hip_angles, window_size=7)
    smoothed_back = smooth_signal(back_angles, window_size=7)

    # We want local MINIMA of knee angle (deepest squat).
    inverted = -smoothed_knee

    bottoms_indices, props = find_peaks(
        inverted,
        distance=10,
        prominence=5
    )

    # Compute a baseline "standing" angle (e.g., 90th percentile)
    baseline_angle = float(np.percentile(knee_angles, 90))

    # Filter bottoms: by absolute angle and by depth relative to baseline
    filtered_idxs = []
    for idx in bottoms_indices:
        angle_at_bottom = float(knee_angles[idx])
        depth = baseline_angle - angle_at_bottom
        if angle_at_bottom <= max_knee_angle and depth >= min_depth:
            filtered_idxs.append(idx)

    bottoms = []
    for i, idx in enumerate(filtered_idxs):
        frame_idx = int(frame_indices[idx])
        bottoms.append({
            "rep": i + 1,
            "frame_index": frame_idx,
            "knee_angle": float(knee_angles[idx]),
            "hip_angle": float(hip_angles[idx]),
            "back_angle": float(back_angles[idx]),
        })

    # Compute aggregated features only from the filtered bottom positions
    if filtered_idxs:
        kneeps = knee_angles[filtered_idxs]
        hips = hip_angles[filtered_idxs]
        backs = back_angles[filtered_idxs]

        features = {
            "knee_min": round(float(np.min(kneeps)), 1),
            "knee_avg": round(float(np.mean(kneeps)), 1),
            "knee_max": round(float(np.max(kneeps)), 1),
            "hip_min": round(float(np.min(hips)), 1),
            "hip_avg": round(float(np.mean(hips)), 1),
            "hip_max": round(float(np.max(hips)), 1),
            "back_min": round(float(np.min(backs)), 1),
            "back_avg": round(float(np.mean(backs)), 1),
            "back_max": round(float(np.max(backs)), 1),
        }
    else:
        features = {
            "knee_min": None,
            "knee_avg": None,
            "knee_max": None,
            "hip_min": None,
            "hip_avg": None,
            "hip_max": None,
            "back_min": None,
            "back_avg": None,
            "back_max": None,
        }

    print(f"\nDetected {len(bottoms)} filtered reps (baseline={baseline_angle:.1f}°):")
    for b in bottoms:
        print(
            f"  Rep {b['rep']}: frame={b['frame_index']}, "
            f"knee={b['knee_angle']:.1f}°, "
            f"hip={b['hip_angle']:.1f}°, "
            f"back={b['back_angle']:.1f}°"
        )

    print(f"\nFeatures:")
    for k, v in features.items():
        print(f"  {k}: {v}")

    # Optionally save frames to inspect false positives
    if save_frames and bottoms:
        os.makedirs(output_dir, exist_ok=True)
        cap_save = cv2.VideoCapture(video_path)
        for b in bottoms:
            fi = b["frame_index"]
            cap_save.set(cv2.CAP_PROP_POS_FRAMES, fi)
            ret, frame = cap_save.read()
            if ret:
                fname = os.path.join(output_dir, f"rep_{b['rep']}_frame_{fi}.jpg")
                cv2.imwrite(fname, frame)
                print(f"Saved frame for rep {b['rep']} -> {fname}")
        cap_save.release()

    return bottoms, features, frame_indices, knee_angles, hip_angles, back_angles


if __name__ == "__main__":
    video_path = "test.mp4"
    bottoms, features, frame_indices, knee_angles, hip_angles, back_angles = (
        get_squat_bottoms_multiple_reps(video_path)
    )