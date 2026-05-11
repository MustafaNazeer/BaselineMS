#!/usr/bin/env python3
"""
Preprocess the Santos et al. 2022 archive into the layout the Kotlin
SantosReplayTest expects.

The archive ships under `A multi-sensor human gait dataset/`:

    raw_data/userNN/capture_userNN_dayY_NNNN_imu.csv     (full capture, 7 cols, deg/s gyro)
    raw_data/userNN/capture_userNN_dayY_NNNN_qtm.c3d     (full motion capture)
    processed_data/userNN/capture_userNN_dayY_NNNN_imu_walk.csv  (walking segment indices)

For each trial this script writes:

    <output_root>/participant-NN/session-Y/trial-NN/
        smartphone.csv     (timestamp_ns, ax, ay, az, gx, gy, gz) gyro in rad/s
        ground-truth.csv   (cadence_steps_per_minute, mean_stride_length_meters, strides, total_distance_meters)

Notes:
  1. The dataset's accelerometer is in m/s^2 and gravity-included; the Kotlin harness's
     `preprocessTrial` step subtracts the first-second mean to produce gravity-removed
     linear acceleration. To keep that estimate accurate, this script emits the walking
     segment only; the stationary pre-walk frames present in raw_data are dropped. The
     walking-segment mean is approximately gravity (the dynamic acceleration averages
     to near zero over many strides), so the harness's gravity estimate is workable.
  2. The dataset's gyroscope is in degrees per second per direct inspection of the
     value range (median |y| around 48, max around 200 during a walk; physical walking
     gyro magnitudes in rad/s would be 1 to 3 orders of magnitude smaller). This script
     converts to rad/s before writing.
  3. The smartphone Nexus 5 file in `processed_data/userNN/*_nexus_walk.csv` only logs
     accelerometer (3 columns); it has no gyroscope channel. The BaselineMS pipeline
     needs both channels for the Madgwick orientation step, so this script uses the
     custom MCU IMU on the same leg as the smartphone substitute. The validation
     report documents this substitution.
  4. Ground truth is extracted from the QTM C3D file's heel markers; events are local
     minima of the heel marker Z (vertical) coordinate.

Usage:
    source ~/.venvs/santos-tools/bin/activate
    python3 scripts/santos-extract-ground-truth.py \\
        --archive-root "$HOME/datasets/santos-2022/A multi-sensor human gait dataset" \\
        --output-root "$HOME/datasets/santos-2022/normalized"
"""
import argparse
import csv
import math
import re
import sys
from pathlib import Path

import numpy as np
from scipy.signal import find_peaks

try:
    import ezc3d
except ImportError:
    sys.stderr.write("ezc3d not installed. Run: source ~/.venvs/santos-tools/bin/activate\n")
    sys.exit(2)


HEEL_MARKER_CANDIDATES = {
    "right": ["heel_R", "RHEE", "R_HEE", "RHEEL", "R_HEEL", "RFCC", "R_FCC", "R.HEE"],
    "left": ["heel_L", "LHEE", "L_HEE", "LHEEL", "L_HEEL", "LFCC", "L_FCC", "L.HEE"],
}

MIN_STEP_INTERVAL_SECONDS = 0.25
# Heel marker vertical (Z) drops from a swing-phase peak to floor at heel strike. Set the
# prominence floor to 30 mm so stance-phase noise minima are rejected. Heel swing in normal
# walking lifts the heel 100 to 200 mm; even a low-amplitude shuffle is at least 30 mm.
HEEL_STRIKE_PROMINENCE_MM = 30.0
IMU_SAMPLE_RATE_HZ = 100.0
DEG_TO_RAD = math.pi / 180.0

CAPTURE_FILENAME_RE = re.compile(r"capture_user(\d+)_day(\d+)_(\d+)_(\w+)\.(csv|c3d)")


def parse_capture_filename(name):
    """Returns (user_id, day_id, trial_id, kind, ext) or None if no match."""
    m = CAPTURE_FILENAME_RE.match(name)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2)), int(m.group(3)), m.group(4), m.group(5)


def load_walk_frame_range(walk_csv_path):
    """Returns (first_frame, last_frame) inclusive from the processed walk CSV."""
    with walk_csv_path.open() as f:
        reader = csv.DictReader(f)
        rows = list(reader)
    if not rows:
        return None
    first = int(rows[0]["frame"])
    last = int(rows[-1]["frame"])
    return first, last


def write_smartphone_csv(raw_imu_csv, walk_start_frame, walk_end_frame, out_csv):
    """Reads raw IMU CSV, slices to the walking segment, converts gyro deg/s -> rad/s,
    rewrites with a timestamp_ns column relative to the walking start.
    Returns the number of rows written."""
    written = 0
    with raw_imu_csv.open() as src, out_csv.open("w", newline="") as dst:
        reader = csv.DictReader(src)
        writer = csv.writer(dst)
        writer.writerow(["timestamp_ns", "ax", "ay", "az", "gx", "gy", "gz"])
        for row in reader:
            frame = int(row["frame"])
            if frame < walk_start_frame or frame > walk_end_frame:
                continue
            ts_ns = (frame - walk_start_frame) * int(1e9 / IMU_SAMPLE_RATE_HZ)
            ax = float(row["acc x"])
            ay = float(row["acc y"])
            az = float(row["acc z"])
            gx = float(row["gyro x"]) * DEG_TO_RAD
            gy = float(row["gyro y"]) * DEG_TO_RAD
            gz = float(row["gyro z"]) * DEG_TO_RAD
            writer.writerow([ts_ns, f"{ax:.6f}", f"{ay:.6f}", f"{az:.6f}", f"{gx:.6f}", f"{gy:.6f}", f"{gz:.6f}"])
            written += 1
    return written


def resolve_marker(labels, candidates):
    for cand in candidates:
        if cand in labels:
            return labels.index(cand), cand
    return None, None


def detect_heel_strikes(z_signal, sample_rate_hz):
    min_distance_samples = int(MIN_STEP_INTERVAL_SECONDS * sample_rate_hz)
    neg = -z_signal
    peaks, _ = find_peaks(neg, distance=min_distance_samples, prominence=HEEL_STRIKE_PROMINENCE_MM)
    return peaks


def compute_ground_truth(c3d_path):
    """Returns dict or None."""
    c3d = ezc3d.c3d(str(c3d_path))
    point_data = c3d["data"]["points"]
    labels = list(c3d["parameters"]["POINT"]["LABELS"]["value"])
    point_rate = float(c3d["parameters"]["POINT"]["RATE"]["value"][0])

    r_idx, _ = resolve_marker(labels, HEEL_MARKER_CANDIDATES["right"])
    l_idx, _ = resolve_marker(labels, HEEL_MARKER_CANDIDATES["left"])
    if r_idx is None and l_idx is None:
        return None, f"no heel markers; labels: {labels[:30]}"

    strides_m = []
    # Stride time is the interval between consecutive heel strikes of the SAME foot. Step
    # time is the interval between consecutive heel strikes of opposite feet; step time is
    # approximately half a stride time. Cadence in "steps per minute" follows the step time
    # convention, so cadence = 60 / step_time = 120 / stride_time.
    stride_times = []

    for side_idx in (r_idx, l_idx):
        if side_idx is None:
            continue
        x = point_data[0, side_idx, :]
        y = point_data[1, side_idx, :]
        z = point_data[2, side_idx, :]
        valid = ~(np.isnan(x) | np.isnan(y) | np.isnan(z))
        if valid.sum() < 100:
            continue
        first_v = np.argmax(valid)
        last_v = len(valid) - 1 - np.argmax(valid[::-1])
        sx = x[first_v:last_v + 1]
        sy = y[first_v:last_v + 1]
        sz = z[first_v:last_v + 1]

        strikes = detect_heel_strikes(sz, point_rate)
        if len(strikes) < 2:
            continue

        for i in range(1, len(strikes)):
            dt = (strikes[i] - strikes[i - 1]) / point_rate
            stride_times.append(dt)
            dx = sx[strikes[i]] - sx[strikes[i - 1]]
            dy = sy[strikes[i]] - sy[strikes[i - 1]]
            stride_mm = math.sqrt(dx * dx + dy * dy)
            strides_m.append(stride_mm / 1000.0)

    if len(stride_times) < 3 or len(strides_m) < 2:
        return None, f"too few events; strides={len(strides_m)}"

    mean_stride_time = float(np.mean(stride_times))
    return {
        "cadence_steps_per_minute": 120.0 / mean_stride_time,
        "mean_stride_length_meters": float(np.mean(strides_m)),
        "strides": len(strides_m),
        "total_distance_meters": float(np.sum(strides_m)),
    }, None


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    ap.add_argument("--archive-root", type=Path, required=True,
                    help='Path to the unzipped "A multi-sensor human gait dataset" directory')
    ap.add_argument("--output-root", type=Path, required=True,
                    help="Where to write the participant-NN/session-Y/trial-NN/ layout")
    ap.add_argument("--limit", type=int, default=0, help="Process at most this many trials (0=all)")
    args = ap.parse_args()

    raw_root = args.archive_root / "raw_data"
    processed_root = args.archive_root / "processed_data"
    if not raw_root.is_dir():
        sys.stderr.write(f"not a directory: {raw_root}\n")
        sys.exit(2)

    failed_log = args.output_root / "_failed-trials.csv"
    args.output_root.mkdir(parents=True, exist_ok=True)
    failed_rows = []
    processed_count = 0

    for user_dir in sorted(raw_root.iterdir()):
        if not user_dir.is_dir():
            continue
        user_match = re.match(r"user(\d+)", user_dir.name)
        if not user_match:
            continue
        user_id = int(user_match.group(1))

        for imu_csv in sorted(user_dir.glob("capture_user*_day*_*_imu.csv")):
            parsed = parse_capture_filename(imu_csv.name)
            if not parsed:
                continue
            uid, day, trial, kind, ext = parsed
            if uid != user_id:
                continue

            c3d_path = imu_csv.with_name(imu_csv.name.replace("_imu.csv", "_qtm.c3d"))
            walk_csv = processed_root / user_dir.name / imu_csv.name.replace("_imu.csv", "_imu_walk.csv")
            if not c3d_path.exists():
                failed_rows.append([imu_csv.name, "missing qtm.c3d"])
                continue
            if not walk_csv.exists():
                failed_rows.append([imu_csv.name, "missing imu_walk.csv"])
                continue

            walk_range = load_walk_frame_range(walk_csv)
            if walk_range is None:
                failed_rows.append([imu_csv.name, "empty walk segment"])
                continue
            walk_start, walk_end = walk_range

            gt, gt_err = compute_ground_truth(c3d_path)
            if gt is None:
                failed_rows.append([imu_csv.name, f"ground truth failed: {gt_err}"])
                continue

            out_dir = args.output_root / f"participant-{uid:02d}" / f"session-{day}" / f"trial-{trial:02d}"
            out_dir.mkdir(parents=True, exist_ok=True)

            smartphone_csv = out_dir / "smartphone.csv"
            rows = write_smartphone_csv(imu_csv, walk_start, walk_end, smartphone_csv)
            if rows == 0:
                failed_rows.append([imu_csv.name, "no IMU rows in walk segment"])
                continue

            gt_csv = out_dir / "ground-truth.csv"
            with gt_csv.open("w", newline="") as fh:
                w = csv.writer(fh)
                w.writerow(["cadence_steps_per_minute", "mean_stride_length_meters", "strides", "total_distance_meters"])
                w.writerow([
                    f"{gt['cadence_steps_per_minute']:.4f}",
                    f"{gt['mean_stride_length_meters']:.4f}",
                    gt["strides"],
                    f"{gt['total_distance_meters']:.4f}",
                ])

            processed_count += 1
            if processed_count % 25 == 0:
                print(f"  processed {processed_count} trials")
            if args.limit and processed_count >= args.limit:
                break
        if args.limit and processed_count >= args.limit:
            break

    if failed_rows:
        with failed_log.open("w", newline="") as fh:
            w = csv.writer(fh)
            w.writerow(["file", "reason"])
            w.writerows(failed_rows)

    print(f"done. processed={processed_count} failed={len(failed_rows)}"
          + (f" (see {failed_log})" if failed_rows else ""))


if __name__ == "__main__":
    main()
