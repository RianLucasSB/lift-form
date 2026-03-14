import cv2
import numpy as np
from scipy.signal import savgol_filter
import os
import json

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


def smooth_signal(x, window_size=21, polyorder=3):
    """Savitzky-Golay filter for better signal smoothing while preserving peaks."""
    if len(x) < window_size:
        return x
    # window_size must be odd
    if window_size % 2 == 0:
        window_size += 1
    return savgol_filter(x, window_size, polyorder)


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


def get_leg_angles(lm, w, h, side_left, min_visibility=0.2):
    """
    Extract shoulder, hip, knee, ankle XY for a given side.
    Returns None if any landmark visibility is below min_visibility.
    """
    if side_left:
        shoulder = lm[PoseLandmark.LEFT_SHOULDER]
        hip = lm[PoseLandmark.LEFT_HIP]
        knee = lm[PoseLandmark.LEFT_KNEE]
        ankle = lm[PoseLandmark.LEFT_ANKLE]
    else:
        shoulder = lm[PoseLandmark.RIGHT_SHOULDER]
        hip = lm[PoseLandmark.RIGHT_HIP]
        knee = lm[PoseLandmark.RIGHT_KNEE]
        ankle = lm[PoseLandmark.RIGHT_ANKLE]

    if min(shoulder.visibility, hip.visibility, knee.visibility, ankle.visibility) < min_visibility:
        return None

    shoulder_xy = (shoulder.x * w, shoulder.y * h)
    hip_xy = (hip.x * w, hip.y * h)
    knee_xy = (knee.x * w, knee.y * h)
    ankle_xy = (ankle.x * w, ankle.y * h)

    knee_angle = calculate_angle(hip_xy, knee_xy, ankle_xy)
    hip_angle = calculate_angle(shoulder_xy, hip_xy, knee_xy)
    back_angle = calculate_back_angle(shoulder_xy, hip_xy)

    return knee_angle, hip_angle, back_angle


def interpolate_missing_frames(frame_indices, angles, total_frames):
    """
    Given sparse (frame_index, angle) pairs, interpolate to fill all frames
    from 0 to total_frames-1. Returns (all_frame_indices, interpolated_angles).
    """
    all_frames = np.arange(total_frames)
    interpolated = np.interp(all_frames, frame_indices, angles)
    return all_frames, interpolated


def detect_reps_state_machine(
    knee_angles,
    frame_indices,
    hip_angles,
    back_angles,
    fps=30,
    entry_threshold_pct=0.85,
    exit_threshold_pct=0.92,
    max_bottom_angle=120.0,
    min_rom=20.0,
    min_rep_frames=8,
):
    """
    State-machine based rep detection.

    States:
      - STANDING: waiting for knee angle to drop below entry_threshold
      - SQUATTING: tracking the minimum angle until knee rises above exit_threshold

    Parameters:
      - entry_threshold_pct: fraction of baseline angle to enter squat (e.g. 0.85 = 85% of standing angle)
      - exit_threshold_pct: fraction of baseline angle to return to standing (hysteresis)
      - max_bottom_angle: absolute max knee angle at bottom to count as valid rep
      - min_rom: minimum range of motion (standing angle - bottom angle) in degrees
      - min_rep_frames: minimum number of frames in squat phase to count as valid rep
    """
    if len(knee_angles) == 0:
        return []

    # Estimate standing angle as the upper percentile of all knee angles
    baseline_angle = float(np.percentile(knee_angles, 85))

    entry_threshold = baseline_angle * entry_threshold_pct
    exit_threshold = baseline_angle * exit_threshold_pct

    STANDING = 0
    SQUATTING = 1

    state = STANDING
    current_min_angle = float('inf')
    current_min_idx = -1
    squat_start_idx = -1

    reps = []

    for i, angle in enumerate(knee_angles):
        if state == STANDING:
            if angle < entry_threshold:
                # Transition to squatting
                state = SQUATTING
                current_min_angle = angle
                current_min_idx = i
                squat_start_idx = i
        elif state == SQUATTING:
            # Track minimum
            if angle < current_min_angle:
                current_min_angle = angle
                current_min_idx = i

            if angle > exit_threshold:
                # Transition back to standing — validate and record rep
                squat_duration_frames = i - squat_start_idx
                rom = baseline_angle - current_min_angle

                if (
                    current_min_angle <= max_bottom_angle
                    and rom >= min_rom
                    and squat_duration_frames >= min_rep_frames
                ):
                    reps.append({
                        "squat_start_idx": squat_start_idx,
                        "bottom_array_idx": current_min_idx,
                        "squat_end_idx": i,
                        "frame_index": int(frame_indices[current_min_idx]),
                        "knee_angle": float(knee_angles[current_min_idx]),
                        "hip_angle": float(hip_angles[current_min_idx]),
                        "back_angle": float(back_angles[current_min_idx]),
                        "rom": round(rom, 1),
                        "squat_duration_frames": squat_duration_frames,
                        "squat_duration_sec": round(squat_duration_frames / fps, 2),
                    })

                # Reset
                state = STANDING
                current_min_angle = float('inf')
                current_min_idx = -1
                squat_start_idx = -1

    return reps, baseline_angle


def extract_rep_phase_features(rep, knee_angles, hip_angles, back_angles, fps, baseline_angle):
    """
    Extract rich per-rep features from the full eccentric-bottom-concentric phases.
    """
    start = rep["squat_start_idx"]
    bottom = rep["bottom_array_idx"]
    end = rep["squat_end_idx"]

    eccentric = slice(start, bottom + 1)
    concentric = slice(bottom, end + 1)

    # Phase durations
    ecc_frames = bottom - start
    con_frames = end - bottom
    total_frames = end - start

    ecc_duration = ecc_frames / fps if fps > 0 else 0
    con_duration = con_frames / fps if fps > 0 else 0
    total_duration = total_frames / fps if fps > 0 else 0

    # Tempo ratio (eccentric:concentric) — good squats typically ~1.5-2.5:1
    tempo_ratio = ecc_duration / con_duration if con_duration > 0 else 0.0

    # Standing angles (avg of first 3 frames of this rep)
    stand_window = min(3, ecc_frames) if ecc_frames > 0 else 1
    knee_standing = float(np.mean(knee_angles[start:start + stand_window]))
    hip_standing = float(np.mean(hip_angles[start:start + stand_window]))
    back_standing = float(np.mean(back_angles[start:start + stand_window]))

    # Bottom angles
    knee_bottom = float(knee_angles[bottom])
    hip_bottom = float(hip_angles[bottom])
    back_bottom = float(back_angles[bottom])

    # Lockout angles (avg of last 3 frames of rep)
    lock_window = min(3, con_frames) if con_frames > 0 else 1
    knee_lockout = float(np.mean(knee_angles[end - lock_window:end]))
    hip_lockout = float(np.mean(hip_angles[end - lock_window:end]))
    back_lockout = float(np.mean(back_angles[end - lock_window:end]))

    # ROM (range of motion)
    knee_rom = knee_standing - knee_bottom
    hip_rom = hip_standing - hip_bottom

    # Descent/ascent rate (degrees per second)
    descent_rate = knee_rom / ecc_duration if ecc_duration > 0 else 0.0
    ascent_rate = (knee_lockout - knee_bottom) / con_duration if con_duration > 0 else 0.0

    # Depth ratio: how deep relative to baseline
    depth_ratio = knee_bottom / baseline_angle if baseline_angle > 0 else 1.0

    # Back angle change from standing to bottom
    back_delta = back_bottom - back_standing

    # Symmetry: how close lockout is to standing (perfect = 0)
    lockout_deficit_knee = abs(knee_standing - knee_lockout)
    lockout_deficit_hip = abs(hip_standing - hip_lockout)

    # Smoothness: std dev of knee angle velocity during eccentric and concentric
    if ecc_frames > 2:
        ecc_velocity = np.diff(knee_angles[eccentric])
        ecc_smoothness = float(np.std(ecc_velocity))
    else:
        ecc_smoothness = 0.0

    if con_frames > 2:
        con_velocity = np.diff(knee_angles[concentric])
        con_smoothness = float(np.std(con_velocity))
    else:
        con_smoothness = 0.0

    return {
        # Bottom position (original features)
        "knee_bottom": round(knee_bottom, 2),
        "hip_bottom": round(hip_bottom, 2),
        "back_bottom": round(back_bottom, 2),
        # Standing position
        "knee_standing": round(knee_standing, 2),
        "hip_standing": round(hip_standing, 2),
        "back_standing": round(back_standing, 2),
        # Lockout position
        "knee_lockout": round(knee_lockout, 2),
        "hip_lockout": round(hip_lockout, 2),
        "back_lockout": round(back_lockout, 2),
        # ROM
        "knee_rom": round(knee_rom, 2),
        "hip_rom": round(hip_rom, 2),
        # Tempo
        "ecc_duration": round(ecc_duration, 3),
        "con_duration": round(con_duration, 3),
        "total_duration": round(total_duration, 3),
        "tempo_ratio": round(tempo_ratio, 3),
        # Velocity
        "descent_rate": round(descent_rate, 2),
        "ascent_rate": round(ascent_rate, 2),
        # Depth
        "depth_ratio": round(depth_ratio, 4),
        # Back
        "back_delta": round(back_delta, 2),
        # Lockout quality
        "lockout_deficit_knee": round(lockout_deficit_knee, 2),
        "lockout_deficit_hip": round(lockout_deficit_hip, 2),
        # Smoothness
        "ecc_smoothness": round(ecc_smoothness, 4),
        "con_smoothness": round(con_smoothness, 4),
    }


def aggregate_rep_features(rep_features_list):
    """
    Aggregate per-rep features into a single feature vector for scoring.
    Uses mean across reps plus consistency (std) for key metrics.
    """
    if not rep_features_list:
        return None

    keys = rep_features_list[0].keys()
    agg = {}

    for key in keys:
        values = [r[key] for r in rep_features_list]
        agg[f"{key}_avg"] = round(float(np.mean(values)), 4)

    # Add consistency metrics (std across reps) for critical angles
    for key in ["knee_bottom", "hip_bottom", "back_bottom", "knee_rom", "tempo_ratio"]:
        values = [r[key] for r in rep_features_list]
        agg[f"{key}_std"] = round(float(np.std(values)), 4) if len(values) > 1 else 0.0

    agg["num_reps"] = len(rep_features_list)

    return agg


def get_squat_bottoms_multiple_reps(
    video_path,
    model_path="pose_landmarker.task",
    use_left_leg=None,
    max_bottom_angle=120.0,
    min_rom=20.0,
    min_rep_frames=8,
    save_frames=True,
    output_dir="."
):
    cap = cv2.VideoCapture(video_path)
    print(video_path)
    if not cap.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")

    frame_angles = []  # list of (frame_index, knee_angle, hip_angle, back_angle)
    auto_detected_side = None

    # Configure PoseLandmarker with lower confidence thresholds
    base_options = python.BaseOptions(model_asset_path=model_path)
    options = vision.PoseLandmarkerOptions(
        base_options=base_options,
        running_mode=vision.RunningMode.VIDEO,
        num_poses=1,
        min_pose_detection_confidence=0.3,
        min_tracking_confidence=0.3,
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

                # Auto-detect preferred side on first frame with pose
                if use_left_leg is None and auto_detected_side is None:
                    auto_detected_side = detect_best_leg(lm, w, h)
                    side_name = "left" if auto_detected_side else "right"
                    print(f"Auto-detect : chosen {side_name} leg ")

                preferred_side = use_left_leg if use_left_leg is not None else auto_detected_side

                # Try both legs and average when both are visible
                left_angles = get_leg_angles(lm, w, h, side_left=True, min_visibility=0.2)
                right_angles = get_leg_angles(lm, w, h, side_left=False, min_visibility=0.2)

                if left_angles is not None and right_angles is not None:
                    # Both legs visible — average them
                    knee_angle = (left_angles[0] + right_angles[0]) / 2.0
                    hip_angle = (left_angles[1] + right_angles[1]) / 2.0
                    back_angle = (left_angles[2] + right_angles[2]) / 2.0
                elif left_angles is not None:
                    knee_angle, hip_angle, back_angle = left_angles
                elif right_angles is not None:
                    knee_angle, hip_angle, back_angle = right_angles
                else:
                    # Neither leg has sufficient visibility — skip frame
                    frame_index += 1
                    continue

                frame_angles.append((frame_index, knee_angle, hip_angle, back_angle))

            frame_index += 1

    total_frames = frame_index
    cap.release()

    if not frame_angles:
        raise RuntimeError("No pose detected in any frame.")

    # Separate indices and angles
    raw_frame_indices = np.array([fa[0] for fa in frame_angles])
    knee_angles_raw = np.array([fa[1] for fa in frame_angles])
    hip_angles_raw = np.array([fa[2] for fa in frame_angles])
    back_angles_raw = np.array([fa[3] for fa in frame_angles])

    # Interpolate missing frames to get a continuous signal
    frame_indices, knee_angles_interp = interpolate_missing_frames(
        raw_frame_indices, knee_angles_raw, total_frames
    )
    _, hip_angles_interp = interpolate_missing_frames(
        raw_frame_indices, hip_angles_raw, total_frames
    )
    _, back_angles_interp = interpolate_missing_frames(
        raw_frame_indices, back_angles_raw, total_frames
    )

    detected_pct = len(raw_frame_indices) / total_frames * 100 if total_frames > 0 else 0
    print(f"Pose detected in {len(raw_frame_indices)}/{total_frames} frames ({detected_pct:.1f}%), interpolated the rest.")

    # Smooth with Savitzky-Golay (aggressive smoothing with window_size=21)
    knee_angles = smooth_signal(knee_angles_interp, window_size=21, polyorder=3)
    hip_angles = smooth_signal(hip_angles_interp, window_size=21, polyorder=3)
    back_angles = smooth_signal(back_angles_interp, window_size=21, polyorder=3)

    # Detect reps using state machine
    bottoms, baseline_angle = detect_reps_state_machine(
        knee_angles=knee_angles,
        frame_indices=frame_indices,
        hip_angles=hip_angles,
        back_angles=back_angles,
        fps=fps,
        max_bottom_angle=max_bottom_angle,
        min_rom=min_rom,
        min_rep_frames=min_rep_frames,
    )

    # Extract per-rep phase features
    rep_features_list = []
    for i, b in enumerate(bottoms):
        b["rep"] = i + 1
        rep_feat = extract_rep_phase_features(b, knee_angles, hip_angles, back_angles, fps, baseline_angle)
        rep_features_list.append(rep_feat)

    # Aggregate across reps
    features = aggregate_rep_features(rep_features_list)
    if features:
        features["baseline_standing_angle"] = round(baseline_angle, 1)

    print(f"\nBaseline standing angle: {baseline_angle:.1f}°")
    print(f"Detected {len(bottoms)} reps:")
    for i, (b, rf) in enumerate(zip(bottoms, rep_features_list)):
        print(
            f"  Rep {b['rep']}: frame={b['frame_index']}, "
            f"knee_bottom={rf['knee_bottom']:.1f}°, knee_standing={rf['knee_standing']:.1f}°, "
            f"ROM={rf['knee_rom']:.1f}°, tempo={rf['tempo_ratio']:.2f}, "
            f"ecc={rf['ecc_duration']:.2f}s, con={rf['con_duration']:.2f}s"
        )

    if features:
        print(f"\nAggregated features ({len(features)} keys):")
        print(json.dumps(features, indent=2))

    # Optionally save frames
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

    return bottoms, features, rep_features_list, frame_indices, knee_angles, hip_angles, back_angles


if __name__ == "__main__":
    video_path = "2.mp4"
    bottoms, features, rep_features_list, frame_indices, knee_angles, hip_angles, back_angles = (
        get_squat_bottoms_multiple_reps(video_path)
    )