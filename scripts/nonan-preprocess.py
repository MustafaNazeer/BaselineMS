#!/usr/bin/env python3
"""
Preprocess the NONAN GaitPrint young adults dataset (Likens et al. 2023,
Scientific Data 10, DOI 10.1038/s41597-023-02704-z) into the layout the
Kotlin NonanReplayTest consumes.

The dataset ships as 35 per-participant figshare zips (S001.zip ... S063.zip).
Each zip extracts to <SID>/<SID>_G01_D0Y_B0B_T0T.csv where S = subject,
G = group (always 01), D = day (1 or 2, used as our session), B = block (1-3),
T = trial (1-3) for 18 trials per subject. Each trial CSV is 174 MB with 321
columns at 200 Hz; we extract only the pelvis accelerometer (mG) and
gyroscope (deg/s) channels, the pelvis being the closest production-analog
mount for the BaselineMS front-pocket smartphone.

Per-stride spatiotemporal ground truth lives in a sibling zip
(Spatiotemporal_Variables.zip) which extracts to one CSV per trial. The
first non-NaN row of each ground truth CSV carries trial-level cadence in
steps/min; trial-level mean stride length is computed by averaging the
(left + right) / 2 per-stride values across all non-NaN strides.

To keep disk usage bounded, this script never fully extracts the per-participant
zips. It opens each zip with zipfile and uses pandas to read only the columns
it needs from each trial entry via the usecols parameter.

Output:
    <output_root>/participant-NN/session-Y/trial-NN/
        smartphone.csv     timestamp_ns, ax, ay, az, gx, gy, gz (gyro rad/s)
        ground-truth.csv   cadence_steps_per_minute, mean_stride_length_meters

Sessions 1 and 2 correspond to days 1 and 2 (one week apart per the dataset
protocol); this is the structure that supports test-retest ICC reporting.
Trials within a session are numbered 1 through 9 corresponding to the
(block, trial) lex-order (B01_T01=1, B01_T02=2, ..., B03_T03=9).
"""
import argparse
import csv
import math
import re
import sys
import zipfile
from io import BytesIO
from pathlib import Path

import numpy as np
import pandas as pd

DEG_TO_RAD = math.pi / 180.0
MG_TO_MS2 = 9.80665e-3  # 1 mG = 9.80665e-3 m/s^2
CM_TO_M = 0.01
NS_PER_S = 1_000_000_000
NONAN_SAMPLE_RATE_HZ = 200.0

PELVIS_ACCEL_X = "Pelvis Accel Sensor X (mG)"
PELVIS_ACCEL_Y = "Pelvis Accel Sensor Y (mG)"
PELVIS_ACCEL_Z = "Pelvis Accel Sensor Z (mG)"
PELVIS_GYRO_X = "Noraxon MyoMotion-Segments-Pelvis-Gyroscope-x (deg/s)"
PELVIS_GYRO_Y = "Noraxon MyoMotion-Segments-Pelvis-Gyroscope-y (deg/s)"
PELVIS_GYRO_Z = "Noraxon MyoMotion-Segments-Pelvis-Gyroscope-z (deg/s)"
USECOLS = [
    PELVIS_ACCEL_X, PELVIS_ACCEL_Y, PELVIS_ACCEL_Z,
    PELVIS_GYRO_X, PELVIS_GYRO_Y, PELVIS_GYRO_Z,
]

CADENCE_COL = "cadence (steps/min)"
LEFT_STRIDE_COL = "left stride length (cm)"
RIGHT_STRIDE_COL = "right stride length (cm)"
GT_USECOLS = [CADENCE_COL, LEFT_STRIDE_COL, RIGHT_STRIDE_COL]

TRIAL_FILENAME_RE = re.compile(r"(S\d+)_G(\d+)_D(\d+)_B(\d+)_T(\d+)\.csv$")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--raw-root", required=True,
                        help="Directory containing per-participant zips and Spatiotemporal_Variables/")
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--limit-participants", type=int, default=0,
                        help="If >0, process only the first N participant zips (smoke test)")
    args = parser.parse_args()

    raw_root = Path(args.raw_root)
    out_root = Path(args.output_root)
    out_root.mkdir(parents=True, exist_ok=True)

    ground_truth_dir = raw_root / "Spatiotemporal_Variables"
    if not ground_truth_dir.is_dir():
        sys.stderr.write(f"missing extracted Spatiotemporal_Variables/ under {raw_root}\n")
        sys.stderr.write(
            f"run: cd {raw_root} && unzip -q -o Spatiotemporal_Variables.zip\n"
        )
        sys.exit(2)

    participant_zips = sorted(
        raw_root.glob("S[0-9][0-9][0-9].zip"),
        key=lambda p: int(p.stem[1:]),
    )
    if args.limit_participants > 0:
        participant_zips = participant_zips[:args.limit_participants]
    if not participant_zips:
        sys.stderr.write(f"no S*.zip per-participant archives under {raw_root}\n")
        sys.exit(2)

    trial_count = 0
    failed = 0
    for zp in participant_zips:
        sid = zp.stem  # 'S001'
        try:
            n = process_participant_zip(zp, sid, ground_truth_dir, out_root)
            trial_count += n
            sys.stderr.write(f"  {sid}: {n} trials\n")
        except Exception as e:
            sys.stderr.write(f"FAIL {sid}: {e}\n")
            failed += 1

    sys.stderr.write(f"preprocessed {trial_count} NONAN trials across {len(participant_zips)} subjects ({failed} subject-level failures)\n")
    sys.exit(0 if trial_count > 0 else 1)


def process_participant_zip(zip_path: Path, sid: str, gt_dir: Path, out_root: Path) -> int:
    pid_num = int(re.findall(r"\d+", sid)[0])
    n = 0
    with zipfile.ZipFile(zip_path) as zf:
        names = sorted([n for n in zf.namelist() if n.endswith(".csv")])
        for entry in names:
            base = entry.split("/")[-1]
            m = TRIAL_FILENAME_RE.match(base)
            if m is None:
                continue
            entry_sid, group, day, block, trial = m.groups()
            day = int(day); block = int(block); trial = int(trial)
            if entry_sid != sid:
                continue
            session = day  # NONAN day 1 / day 2 -> session 1 / 2
            trial_index = (block - 1) * 3 + trial  # B01_T01=1, B01_T02=2, ..., B03_T03=9

            # Load just the columns we need from the trial CSV.
            try:
                with zf.open(entry) as f:
                    df = pd.read_csv(f, usecols=USECOLS)
            except ValueError as e:
                sys.stderr.write(f"  skip {base}: column read error: {e}\n")
                continue
            if df.empty:
                continue
            # Look up trial-level ground truth.
            gt_path = gt_dir / base
            if not gt_path.exists():
                sys.stderr.write(f"  skip {base}: ground truth CSV not found at {gt_path}\n")
                continue
            cadence, stride_length_m = read_ground_truth(gt_path)
            if cadence is None or stride_length_m is None:
                sys.stderr.write(f"  skip {base}: incomplete ground truth\n")
                continue

            out_dir = out_root / f"participant-{pid_num:02d}" / f"session-{session}" / f"trial-{trial_index:02d}"
            out_dir.mkdir(parents=True, exist_ok=True)
            write_imu_csv(df, out_dir / "smartphone.csv")
            write_ground_truth_csv(cadence, stride_length_m, out_dir / "ground-truth.csv")
            n += 1
    return n


def read_ground_truth(path: Path):
    df = pd.read_csv(path, usecols=GT_USECOLS)
    if df.empty:
        return None, None
    cadence = df[CADENCE_COL].dropna()
    if cadence.empty:
        return None, None
    cadence_val = float(cadence.iloc[0])
    left = df[LEFT_STRIDE_COL].dropna().to_numpy()
    right = df[RIGHT_STRIDE_COL].dropna().to_numpy()
    if left.size == 0 or right.size == 0:
        return cadence_val, None
    n = min(len(left), len(right))
    mean_stride_cm = float(np.mean((left[:n] + right[:n]) / 2.0))
    return cadence_val, mean_stride_cm * CM_TO_M


def write_imu_csv(df: pd.DataFrame, out_path: Path) -> None:
    # NONAN occasionally has stray NaN samples from sensor synchronization gaps;
    # drop them so the downstream Kotlin pipeline sees only finite values.
    df = df.dropna()
    n = len(df)
    if n == 0:
        return
    timestamps_ns = (np.arange(n) / NONAN_SAMPLE_RATE_HZ * NS_PER_S).astype(np.int64)
    ax = df[PELVIS_ACCEL_X].to_numpy() * MG_TO_MS2
    ay = df[PELVIS_ACCEL_Y].to_numpy() * MG_TO_MS2
    az = df[PELVIS_ACCEL_Z].to_numpy() * MG_TO_MS2
    gx = df[PELVIS_GYRO_X].to_numpy() * DEG_TO_RAD
    gy = df[PELVIS_GYRO_Y].to_numpy() * DEG_TO_RAD
    gz = df[PELVIS_GYRO_Z].to_numpy() * DEG_TO_RAD
    with open(out_path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["timestamp_ns", "ax", "ay", "az", "gx", "gy", "gz"])
        for i in range(n):
            w.writerow([
                int(timestamps_ns[i]),
                f"{ax[i]:.6f}",
                f"{ay[i]:.6f}",
                f"{az[i]:.6f}",
                f"{gx[i]:.6f}",
                f"{gy[i]:.6f}",
                f"{gz[i]:.6f}",
            ])


def write_ground_truth_csv(cadence: float, stride_length_m: float, out_path: Path) -> None:
    with open(out_path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["cadence_steps_per_minute", "mean_stride_length_meters"])
        w.writerow([cadence, stride_length_m])


if __name__ == "__main__":
    main()
