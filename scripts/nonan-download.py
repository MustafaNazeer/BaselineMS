#!/usr/bin/env python3
"""
Download the NONAN GaitPrint figshare collection (Likens et al. 2023,
Sci Data, DOI 10.6084/m9.figshare.c.6415061) into ~/datasets/nonan-gaitprint/raw/.

40 items total: 35 per-participant zips (S001 through S063, with some IDs unused)
plus 5 support files (template_scripts.zip, Spatiotemporal_Variables.zip, NONAN
Gaitprint Code, Gaitprint_Trial_Characteristics, GaitPrint_Subject_Characteristics).
Total roughly 50 GB.

Idempotent: skips any file already present at the expected size. Uses curl's
--continue-at flag to resume partial downloads. Sequential to stay friendly to
figshare's CDN; expect roughly 1 to 2 hours on a typical home connection.
"""
import json
import os
import subprocess
import sys
import time
import urllib.request
from datetime import datetime
from pathlib import Path

COLLECTION_ID = 6415061
DEST = Path.home() / "datasets" / "nonan-gaitprint" / "raw"


def log(msg: str) -> None:
    ts = datetime.now().strftime("%H:%M:%S")
    line = f"[{ts}] {msg}"
    print(line, flush=True)
    with open(DEST / "download.log", "a") as f:
        f.write(line + "\n")


def fetch_json(url: str) -> object:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def download_file(url: str, dest: Path, expected_size: int) -> None:
    if dest.exists() and dest.stat().st_size == expected_size:
        log(f"SKIP {dest.name} ({expected_size} bytes, already complete)")
        return
    log(f"GET  {dest.name} ({expected_size} bytes) <- {url}")
    subprocess.run(
        [
            "curl",
            "-fL",
            "-A", "Mozilla/5.0",
            "--retry", "5",
            "--retry-delay", "10",
            "-C", "-",
            "-o", str(dest),
            url,
        ],
        check=True,
    )
    actual = dest.stat().st_size
    if actual != expected_size:
        raise RuntimeError(f"size mismatch for {dest.name}: got {actual}, expected {expected_size}")


def main() -> None:
    DEST.mkdir(parents=True, exist_ok=True)
    log(f"fetching article list for collection {COLLECTION_ID}")
    articles = fetch_json(
        f"https://api.figshare.com/v2/collections/{COLLECTION_ID}/articles?page_size=100"
    )
    log(f"found {len(articles)} articles in collection")

    total_files = 0
    total_bytes = 0
    failures: list[str] = []

    for i, a in enumerate(articles, start=1):
        article_id = a["id"]
        title = a["title"]
        log(f"[{i}/{len(articles)}] article {article_id}: {title}")
        try:
            article = fetch_json(f"https://api.figshare.com/v2/articles/{article_id}")
        except Exception as e:
            log(f"  FAIL article metadata: {e}")
            failures.append(title)
            continue
        for f in article.get("files", []):
            name = f["name"]
            size = int(f["size"])
            url = f["download_url"]
            try:
                download_file(url, DEST / name, size)
                total_files += 1
                total_bytes += size
            except subprocess.CalledProcessError as e:
                log(f"  FAIL {name}: curl exit {e.returncode}")
                failures.append(name)
            except Exception as e:
                log(f"  FAIL {name}: {e}")
                failures.append(name)
        # Small inter-article pause to stay friendly to the CDN.
        time.sleep(0.5)

    log(f"finished: {total_files} files, {total_bytes / 1e9:.2f} GB")
    if failures:
        log(f"failures ({len(failures)}): {', '.join(failures)}")
        sys.exit(1)


if __name__ == "__main__":
    main()
