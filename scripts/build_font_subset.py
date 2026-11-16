#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tải bản subset gọn nhẹ của font Noto Sans JP (SIL Open Font License 1.1) bao gồm:
- Toàn bộ Hiragana (U+3040..U+309F)
- Toàn bộ Katakana (U+30A0..U+30FF)
- Dấu câu tiếng Nhật & chữ Hán dùng trong nhãn bài học (行原稿用紙拗音促長小書濁半清)
- Bảng chữ cái ASCII & Tiếng Việt đầy đủ dấu
Lưu vào app/src/main/res/font/noto_sans_jp_regular.ttf và noto_sans_jp_bold.ttf.
"""

import re
import urllib.parse
import urllib.request
from pathlib import Path


def build_charset() -> str:
    chars = set()
    # ASCII printable
    for c in range(0x0020, 0x007F):
        chars.add(chr(c))
    # Tiếng Việt có dấu đầy đủ
    vi_chars = (
        "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ"
        "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ"
        "–—…•°±×÷≥≤≠≈←→↑↓✓✗★☆"
    )
    for ch in vi_chars:
        chars.add(ch)
    # Hiragana (U+3041..U+3096)
    for c in range(0x3041, 0x3097):
        chars.add(chr(c))
    # Katakana (U+30A1..U+30FC)
    for c in range(0x30A1, 0x30FD):
        chars.add(chr(c))
    # Dấu câu Nhật + Chữ Hán dùng trong UI + 80 chữ Kanji JLPT N5
    extra = (
        "、。「」『』（）・〜ー行原稿用紙拗音促長小書濁半清日本語文字画数順練習復習設定漢文法"
        "一二三四五六七八九十百千万日"
        "月火水木金土山川田人子女男父母友先生学校本名語"
        "時分半年今前後上下中外右左北南東西"
        "来食飲見聞読書話買休出入大小高安新古多少白車電駅円"
    )
    for ch in extra:
        chars.add(ch)
    return "".join(sorted(chars))


def download_subset_ttf(weight: int, out_path: Path, text: str) -> None:
    encoded_text = urllib.parse.quote(text)
    css_url = f"https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@{weight}&text={encoded_text}"
    req = urllib.request.Request(
        css_url,
        headers={
            "User-Agent": "Mozilla/5.0 (Linux; U; Android 4.1.1; en-gb; Build/KLP) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30"
        },
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        css = resp.read().decode("utf-8")

    match = re.search(r"url\((https://[^)]+)\)", css)
    if not match:
        raise RuntimeError(f"Không tìm thấy URL font trong CSS:\n{css}")

    font_url = match.group(1)
    print(f"Downloading Noto Sans JP subset (weight={weight}, chars={len(text)}) ...")
    font_req = urllib.request.Request(font_url, headers={"User-Agent": "KanaAndroidBuildScript/1.0"})
    with urllib.request.urlopen(font_req, timeout=30) as f_resp:
        font_bytes = f_resp.read()

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(font_bytes)
    print(f"Saved {out_path} ({len(font_bytes)} bytes)")


def main() -> None:
    repo_root = Path(__file__).resolve().parent.parent
    font_dir = repo_root / "app" / "src" / "main" / "res" / "font"
    charset = build_charset()
    download_subset_ttf(400, font_dir / "noto_sans_jp_regular.ttf", charset)
    download_subset_ttf(700, font_dir / "noto_sans_jp_bold.ttf", charset)


if __name__ == "__main__":
    main()
