#!/usr/bin/env python3
"""
Download the Luo et al. 2020 input data zip from figshare into
~/datasets/luo-2020/raw/input_data_SD.zip.

Citation: Luo et al. 2020, "A database of human gait performance on irregular
and uneven surfaces collected by wearable sensors," Scientific Data 7:219,
DOI 10.1038/s41597-020-0563-y. Data DOI 10.6084/m9.figshare.c.4892463.

The zip is ~2 GB and contains per-participant directories (1 to 30), each with
57 trials and 6 Xsens MTw sensor CSVs per trial (12,003 files total).

Idempotent: skips the file if already present at the expected size.
"""
import os
import subprocess
import sys
from pathlib import Path

DEST = Path.home() / "datasets" / "luo-2020" / "raw"
URL = "https://ndownloader.figshare.com/files/21998373"
FILE = "input_data_SD.zip"
EXPECTED_BYTES = 2043183812


def main() -> None:
    DEST.mkdir(parents=True, exist_ok=True)
    out_path = DEST / FILE
    if out_path.exists() and out_path.stat().st_size == EXPECTED_BYTES:
        print(f"SKIP {FILE} (already present at expected size)")
        return
    print(f"GET  {FILE} ({EXPECTED_BYTES} bytes) <- {URL}")
    subprocess.run(
        [
            "curl",
            "-fL",
            "-A", "Mozilla/5.0",
            "--retry", "5",
            "--retry-delay", "10",
            "-C", "-",
            "-o", str(out_path),
            URL,
        ],
        check=True,
    )
    actual = out_path.stat().st_size
    if actual != EXPECTED_BYTES:
        sys.stderr.write(f"size mismatch: got {actual}, expected {EXPECTED_BYTES}\n")
        sys.exit(1)
    print(f"OK   {FILE} ({actual} bytes)")


if __name__ == "__main__":
    main()
