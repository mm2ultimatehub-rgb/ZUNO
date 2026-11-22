#!/usr/bin/env python3
"""Sync the public ZUNO Icon Store catalog into Android launcher resources.

Only public catalog images from the configured Supabase Storage host are fetched.
The Gradle icon generator turns these normalized PNGs into adaptive icons and
activity-alias launcher entries in the next signed APK.
"""
from __future__ import annotations

import hashlib
import io
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

import cairosvg
import requests
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
ICON_DIR = ROOT / "app" / "src" / "main" / "iconpack" / "svg"
METADATA = ROOT / "app" / "src" / "main" / "iconpack" / "metadata.json"
SUPABASE_URL = "https://jlyinisqhkupddyfcdut.supabase.co"
PUBLISHABLE_KEY = "sb_publishable_sjwsqCho2mlpdzBUFQQ_dQ_2uBeSDwl"
PUBLIC_HOST = "jlyinisqhkupddyfcdut.supabase.co"
MAX_ICONS = 24
MAX_FILE_BYTES = 10 * 1024 * 1024
MAX_EDGE = 1024
ALLOWED_MIME = {"image/png", "image/webp", "image/svg+xml"}


def normalize_image(raw: bytes, mime: str) -> bytes:
    if mime == "image/svg+xml":
        raw = cairosvg.svg2png(bytestring=raw, output_width=MAX_EDGE)
    with Image.open(io.BytesIO(raw)) as source:
        image = source.convert("RGBA")
        image.thumbnail((MAX_EDGE, MAX_EDGE), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (MAX_EDGE, MAX_EDGE), (0, 0, 0, 0))
        canvas.alpha_composite(image, ((MAX_EDGE - image.width) // 2, (MAX_EDGE - image.height) // 2))
        output = io.BytesIO()
        canvas.save(output, format="PNG", optimize=True)
        return output.getvalue()


def main() -> int:
    ICON_DIR.mkdir(parents=True, exist_ok=True)
    headers = {"apikey": PUBLISHABLE_KEY, "Accept": "application/json"}
    endpoint = (
        f"{SUPABASE_URL}/rest/v1/zuno_icon_catalog"
        "?select=id,title,username,asset_url,mime_type,file_size,created_at"
        "&order=created_at.desc&limit=120"
    )
    response = requests.get(endpoint, headers=headers, timeout=20)
    response.raise_for_status()
    rows = response.json()
    if not isinstance(rows, list):
        raise RuntimeError("ZUNO Icon Store catalog response is not an array.")

    normalized: list[dict[str, str]] = []
    used_files: set[str] = set()
    session = requests.Session()
    for row in rows:
        if len(normalized) >= MAX_ICONS:
            break
        icon_id = str(row.get("id") or "").strip()
        asset_url = str(row.get("asset_url") or "").strip()
        mime = str(row.get("mime_type") or "").lower().strip()
        parsed = urlparse(asset_url)
        if not icon_id or parsed.scheme != "https" or parsed.hostname != PUBLIC_HOST:
            continue
        if mime not in ALLOWED_MIME:
            continue
        try:
            asset = session.get(asset_url, timeout=25, stream=True)
            asset.raise_for_status()
            content = bytearray()
            for chunk in asset.iter_content(64 * 1024):
                if not chunk:
                    continue
                content.extend(chunk)
                if len(content) > MAX_FILE_BYTES:
                    raise ValueError("icon exceeds 10 MB")
            png = normalize_image(bytes(content), mime)
            filename = f"store_{hashlib.sha256(icon_id.encode()).hexdigest()[:20]}.png"
            (ICON_DIR / filename).write_bytes(png)
            used_files.add(filename)
            title = str(row.get("title") or "ZUNO Icon").strip()[:60]
            author = str(row.get("username") or "ZUNO Creator").strip()[:32]
            normalized.append({
                "Id": icon_id,
                "Name": title or "ZUNO Icon",
                "Author": author or "ZUNO Creator",
                "Source": filename,
                "Link": f"https://mm2ultimatehub-rgb.github.io/ZUNO-Icon-Store/#/icon/{icon_id}",
            })
            print(f"Prepared Store icon: {title} (@{author})")
        except Exception as error:
            print(f"Skipping Store icon {icon_id}: {error}", file=sys.stderr)

    for old in ICON_DIR.glob("store_*.png"):
        if re.fullmatch(r"store_[a-f0-9]{20}\.png", old.name) and old.name not in used_files:
            old.unlink()

    METADATA.write_text(json.dumps(normalized, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Synced {len(normalized)} public Store icons into the next APK.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
