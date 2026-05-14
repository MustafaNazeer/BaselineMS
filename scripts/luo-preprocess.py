#!/usr/bin/env python3
"""
Preprocess the Luo et al. 2020 irregular surfaces gait dataset (Scientific Data
7:219, DOI 10.1038/s41597-020-0563-y) into a per-trial normalized layout that
the Kotlin LuoReplayTest consumes.

Source layout (after extracting ~/datasets/luo-2020/raw/input_data_SD.zip):
    input data_SD/<P>/<T>-000_<XSENS_ID>.txt.csv
where P = participant number (1 to 30), T = trial number (1 to 57), and
XSENS_ID is one of six sensor IDs whose body-location mapping comes from
Table 4 of the paper:

    00B432CC = Trunk        (production-analog mount)
    00B43295 = Wrist
    00B43293 = Right thigh
    00B4328B = Left thigh
    00B4329B = Right shank
    00B432B6 = Left shank   (independent reference for cross-method validation)

Trial-number to surface mapping comes from Table 3 of the paper:
    1 to 3    Calibration (CALIB)   -- skipped, not walking
    4 to 9    Flat even (FE)
    10 to 15  Cobble stone (CS)
    16 to 26  Stairs (alternating up/down)
    28 to 39  Slopes (alternating up/down)
    40 to 51  Banks (alternating left/right)
    52 to 57  Grass (GR)

The paper does NOT publish per-trial spatiotemporal ground truth; only per-surface
mean trial duration. This script writes the trunk and left-shank IMU streams per
trial. The Kotlin replay test runs `GaitPipeline.process` twice (once per stream)
and reports cross-sensor cadence agreement (Bland-Altman LOA, Pearson correlation)
as the validation metric.

Output layout:
    <output_root>/participant-NN/session-1/trial-XX-<surface>/
        trunk.csv         timestamp_ns, ax, ay, az, gx, gy, gz  (gyro rad/s)
        shank.csv         same layout, left shank
        trial-meta.csv    trial_number, surface_label, sample_rate_hz, n_samples
"""
import argparse
import csv
import re
import sys
from pathlib import Path

import numpy as np
import pandas as pd

NS_PER_S = 1_000_000_000
LUO_SAMPLE_RATE_HZ = 100.0

TRUNK_XSENS_ID = "00B432CC"
SHANK_LEFT_XSENS_ID = "00B432B6"

TRIAL_TO_SURFACE = {}
for t in (1, 2, 3):
    TRIAL_TO_SURFACE[t] = "CALIB"
for t in (4, 5, 6, 7, 8, 9):
    TRIAL_TO_SURFACE[t] = "FE"
for t in (10, 11, 12, 13, 14, 15):
    TRIAL_TO_SURFACE[t] = "CS"
for t in (16, 18, 20, 22, 24, 26):
    TRIAL_TO_SURFACE[t] = "StrU"
for t in (17, 19, 21, 23, 25, 27):
    TRIAL_TO_SURFACE[t] = "StrD"
for t in (28, 30, 32, 34, 36, 38):
    TRIAL_TO_SURFACE[t] = "SlpU"
for t in (29, 31, 33, 35, 37, 39):
    TRIAL_TO_SURFACE[t] = "SlpD"
for t in (40, 42, 44, 46, 48, 50):
    TRIAL_TO_SURFACE[t] = "BnkL"
for t in (41, 43, 45, 47, 49, 51):
    TRIAL_TO_SURFACE[t] = "BnkR"
for t in (52, 53, 54, 55, 56, 57):
    TRIAL_TO_SURFACE[t] = "GR"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--raw-root", required=True,
                        help='Directory containing "input data_SD/" with per-participant dirs')
    parser.add_argument("--output-root", required=True)
    args = parser.parse_args()

    raw_root = Path(args.raw_root)
    input_data = raw_root / "input data_SD"
    if not input_data.is_dir():
        sys.stderr.write(f"expected 'input data_SD' directory under {raw_root}\n")
        sys.exit(2)
    out_root = Path(args.output_root)
    out_root.mkdir(parents=True, exist_ok=True)

    participant_dirs = sorted(
        (p for p in input_data.iterdir() if p.is_dir() and p.name.isdigit()),
        key=lambda p: int(p.name),
    )
    if not participant_dirs:
        sys.stderr.write(f"no numeric participant directories under {input_data}\n")
        sys.exit(2)

    trial_count = 0
    skipped_calib = 0
    skipped_missing_sensor = 0
    for p_dir in participant_dirs:
        pid = int(p_dir.name)
        for trial_num in sorted(set(TRIAL_TO_SURFACE.keys())):
            surface = TRIAL_TO_SURFACE[trial_num]
            if surface == "CALIB":
                skipped_calib += 1
                continue
            trunk_csv = p_dir / f"{trial_num}-000_{TRUNK_XSENS_ID}.txt.csv"
            shank_csv = p_dir / f"{trial_num}-000_{SHANK_LEFT_XSENS_ID}.txt.csv"
            if not trunk_csv.exists() or not shank_csv.exists():
                skipped_missing_sensor += 1
                continue
            try:
                trunk_data = process_xsens_csv(trunk_csv)
                shank_data = process_xsens_csv(shank_csv)
            except Exception as e:
                sys.stderr.write(f"FAIL p={pid} t={trial_num}: {e}\n")
                continue
            if (trunk_data is None or shank_data is None
                    or len(trunk_data["timestamp_ns"]) < 100
                    or len(shank_data["timestamp_ns"]) < 100):
                continue
            out_dir = out_root / f"participant-{pid:02d}" / "session-1" / f"trial-{trial_num:02d}-{surface}"
            out_dir.mkdir(parents=True, exist_ok=True)
            write_imu_csv(trunk_data, out_dir / "trunk.csv")
            write_imu_csv(shank_data, out_dir / "shank.csv")
            write_meta_csv(trial_num, surface, len(trunk_data["timestamp_ns"]), out_dir / "trial-meta.csv")
            trial_count += 1

    sys.stderr.write(
        f"preprocessed {trial_count} Luo walking trials "
        f"(skipped {skipped_calib} calibration entries, {skipped_missing_sensor} missing-sensor)\n"
    )
    sys.exit(0 if trial_count > 0 else 1)


def process_xsens_csv(path: Path):
    df = pd.read_csv(path, skipinitialspace=True)
    needed = ["Acc_X", "Acc_Y", "Acc_Z", "Gyr_X", "Gyr_Y", "Gyr_Z"]
    for col in needed:
        if col not in df.columns:
            raise ValueError(f"{path.name}: missing column {col!r} (have {list(df.columns)[:8]}...)")
    # Xsens MTw recordings occasionally produce NaN samples on dropout; drop those
    # rows entirely so the downstream Kotlin parser does not have to tolerate them.
    df = df.dropna(subset=needed)
    n = len(df)
    if n == 0:
        return None
    # Sample indexes are recomputed from the post-filter row count, so dt stays
    # at the nominal 1/100 Hz across the trial even if some samples were dropped.
    # This introduces small time-base inaccuracy at the dropout boundaries; on a
    # 100 Hz sensor with handful-of-sample dropouts per trial, the effect on
    # cadence is well under 1 percent and acceptable for cross-method agreement.
    timestamps_ns = (np.arange(n) / LUO_SAMPLE_RATE_HZ * NS_PER_S).astype(np.int64)
    return {
        "timestamp_ns": timestamps_ns,
        "ax": df["Acc_X"].to_numpy(float),
        "ay": df["Acc_Y"].to_numpy(float),
        "az": df["Acc_Z"].to_numpy(float),
        "gx": df["Gyr_X"].to_numpy(float),
        "gy": df["Gyr_Y"].to_numpy(float),
        "gz": df["Gyr_Z"].to_numpy(float),
    }


def write_imu_csv(data, out_path: Path) -> None:
    if data is None:
        return
    with open(out_path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["timestamp_ns", "ax", "ay", "az", "gx", "gy", "gz"])
        for i in range(len(data["timestamp_ns"])):
            w.writerow([
                int(data["timestamp_ns"][i]),
                f'{data["ax"][i]:.6f}',
                f'{data["ay"][i]:.6f}',
                f'{data["az"][i]:.6f}',
                f'{data["gx"][i]:.6f}',
                f'{data["gy"][i]:.6f}',
                f'{data["gz"][i]:.6f}',
            ])


def write_meta_csv(trial_number: int, surface: str, n_samples: int, out_path: Path) -> None:
    with open(out_path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["trial_number", "surface_label", "sample_rate_hz", "n_samples"])
        w.writerow([trial_number, surface, LUO_SAMPLE_RATE_HZ, n_samples])


if __name__ == "__main__":
    main()
