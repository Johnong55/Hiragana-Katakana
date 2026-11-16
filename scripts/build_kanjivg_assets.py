#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script build dữ liệu nét chữ Hiragana & Katakana từ KanjiVG (CC BY-SA 3.0).
- Tải (nếu chưa có trong scripts/kanjivg_raw/) các file SVG theo mã Unicode 5 chữ số hex từ KanjiVG.
- Parse thẻ <path> của từng nét theo đúng thứ tự kvg:...-s1, s2, ...
- Nội suy đường cong Cubic/Quadratic Bezier và đoạn thẳng thành chuỗi điểm chuẩn hoá trong [0.0, 1.0] (từ viewBox 0..109).
- Resample mỗi nét thành N điểm cách đều nhau (mặc định 32 điểm) để vừa dùng cho render/animate mượt, vừa dùng làm mẫu chấm điểm.
- Xuất JSON ra app/src/main/assets/strokes/kana_strokes.json.
"""

import json
import math
import os
import re
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import List, Tuple, Dict, Any

VIEWBOX_SIZE = 109.0
RESAMPLE_POINTS = 32

# 46 Hiragana cơ bản theo hàng gojūon
HIRAGANA_BASIC = [
    # あ行
    ("あ", "a", "あ行"), ("い", "i", "あ行"), ("う", "u", "あ行"), ("え", "e", "あ行"), ("お", "o", "あ行"),
    # か行
    ("か", "ka", "か行"), ("き", "ki", "か行"), ("く", "ku", "か行"), ("け", "ke", "か行"), ("こ", "ko", "か行"),
    # さ行
    ("さ", "sa", "さ行"), ("し", "shi", "さ行"), ("す", "su", "さ行"), ("せ", "se", "さ行"), ("そ", "so", "さ行"),
    # た行
    ("た", "ta", "た行"), ("ち", "chi", "た行"), ("つ", "tsu", "た行"), ("て", "te", "た行"), ("と", "to", "た行"),
    # な行
    ("な", "na", "な行"), ("に", "ni", "な行"), ("ぬ", "nu", "な行"), ("ね", "ne", "な行"), ("の", "no", "な行"),
    # は行
    ("は", "ha", "は行"), ("ひ", "hi", "は行"), ("ふ", "fu", "は行"), ("へ", "he", "は行"), ("ほ", "ho", "は行"),
    # ま行
    ("ま", "ma", "ま行"), ("み", "mi", "ま行"), ("む", "mu", "ま行"), ("め", "me", "ま行"), ("も", "mo", "ま行"),
    # や行
    ("や", "ya", "や行"), ("ゆ", "yu", "や行"), ("よ", "yo", "や行"),
    # ら行
    ("ら", "ra", "ら行"), ("り", "ri", "ら行"), ("る", "ru", "ら行"), ("れ", "re", "ら行"), ("ろ", "ro", "ら行"),
    # わ行 + ん
    ("わ", "wa", "わ行"), ("を", "wo", "わ行"), ("ん", "n", "わ行"),
]

# Hiragana dakuten / handakuten
HIRAGANA_DAKUTEN = [
    ("が", "ga", "が行"), ("ぎ", "gi", "が行"), ("ぐ", "gu", "が行"), ("げ", "ge", "が行"), ("ご", "go", "が行"),
    ("ざ", "za", "ざ行"), ("じ", "ji", "ざ行"), ("ず", "zu", "ざ行"), ("ぜ", "ze", "ざ行"), ("ぞ", "zo", "ざ行"),
    ("だ", "da", "だ行"), ("ぢ", "ji_di", "だ行"), ("づ", "zu_du", "だ行"), ("で", "de", "だ行"), ("ど", "do", "だ行"),
    ("ば", "ba", "ば行"), ("び", "bi", "ば行"), ("ぶ", "bu", "ば行"), ("べ", "be", "ば行"), ("ぼ", "bo", "ば行"),
    ("ぱ", "pa", "ぱ行"), ("ぴ", "pi", "ぱ行"), ("ぷ", "pu", "ぱ行"), ("ぺ", "pe", "ぱ行"), ("ぽ", "po", "ぱ行"),
]

# Hiragana chữ nhỏ (yōon / sokuon)
HIRAGANA_SMALL = [
    ("ゃ", "xya", "拗音"), ("ゅ", "xyu", "拗音"), ("ょ", "xyo", "拗音"),
    ("っ", "xtsu", "促音"),
    ("ぁ", "xa", "小書き"), ("ぃ", "xi", "小書き"), ("ぅ", "xu", "小書き"), ("ぇ", "xe", "小書き"), ("ぉ", "xo", "小書き"),
]

# 46 Katakana cơ bản theo hàng gojūon
KATAKANA_BASIC = [
    # ア行
    ("ア", "a", "ア行"), ("イ", "i", "ア行"), ("ウ", "u", "ア行"), ("エ", "e", "ア行"), ("オ", "o", "ア行"),
    # カ行
    ("カ", "ka", "カ行"), ("キ", "ki", "カ行"), ("ク", "ku", "カ行"), ("ケ", "ke", "カ行"), ("コ", "ko", "カ行"),
    # サ行
    ("サ", "sa", "サ行"), ("シ", "shi", "サ行"), ("ス", "su", "サ行"), ("セ", "se", "サ行"), ("ソ", "so", "サ行"),
    # タ行
    ("タ", "ta", "タ行"), ("チ", "chi", "タ行"), ("ツ", "tsu", "タ行"), ("テ", "te", "タ行"), ("ト", "to", "タ行"),
    # ナ行
    ("ナ", "na", "ナ行"), ("ニ", "ni", "ナ行"), ("ヌ", "nu", "ナ行"), ("ネ", "ne", "ナ行"), ("ノ", "no", "ナ行"),
    # ハ行
    ("ハ", "ha", "ハ行"), ("ヒ", "hi", "ハ行"), ("フ", "fu", "ハ行"), ("ヘ", "he", "ハ行"), ("ホ", "ho", "ハ行"),
    # マ行
    ("マ", "ma", "マ行"), ("ミ", "mi", "マ行"), ("ム", "mu", "マ行"), ("メ", "me", "マ行"), ("モ", "mo", "マ行"),
    # ヤ行
    ("ヤ", "ya", "ヤ行"), ("ユ", "yu", "ヤ行"), ("ヨ", "yo", "ヤ行"),
    # ラ行
    ("ラ", "ra", "ラ行"), ("リ", "ri", "ラ行"), ("ル", "ru", "ラ行"), ("レ", "re", "ラ行"), ("ロ", "ro", "ラ行"),
    # ワ行 + ン
    ("ワ", "wa", "ワ行"), ("ヲ", "wo", "ワ行"), ("ン", "n", "ワ行"),
]

# Katakana dakuten / handakuten
KATAKANA_DAKUTEN = [
    ("ガ", "ga", "ガ行"), ("ギ", "gi", "ガ行"), ("グ", "gu", "ガ行"), ("ゲ", "ge", "ガ行"), ("ゴ", "go", "ガ行"),
    ("ザ", "za", "ザ行"), ("ジ", "ji", "ザ行"), ("ズ", "zu", "ザ行"), ("ゼ", "ze", "ザ行"), ("ゾ", "zo", "ザ行"),
    ("ダ", "da", "ダ行"), ("ヂ", "ji_di", "ダ行"), ("ヅ", "zu_du", "ダ行"), ("デ", "de", "ダ行"), ("ド", "do", "ダ行"),
    ("バ", "ba", "バ行"), ("ビ", "bi", "バ行"), ("ブ", "bu", "バ行"), ("ベ", "be", "バ行"), ("ボ", "bo", "バ行"),
    ("パ", "pa", "パ行"), ("ピ", "pi", "パ行"), ("プ", "pu", "パ行"), ("ペ", "pe", "パ行"), ("ポ", "po", "パ行"),
]

# Katakana chữ nhỏ + trường âm
KATAKANA_SMALL = [
    ("ャ", "xya", "拗音"), ("ュ", "xyu", "拗音"), ("ョ", "xyo", "拗音"),
    ("ッ", "xtsu", "促音"),
    ("ァ", "xa", "小書き"), ("ィ", "xi", "小書き"), ("ゥ", "xu", "小書き"), ("ェ", "xe", "小書き"), ("ォ", "xo", "小書き"),
    ("ー", "choon", "長音"),
]


def tokenize_svg_path(d: str) -> List[str]:
    """Tách chuỗi thuộc tính d của SVG path thành danh sách lệnh và số."""
    token_re = re.compile(r"([MmLlHhVvCcSsQqTtZz])|([-+]?(?:\d*\.\d+|\d+)(?:[eE][-+]?\d+)?)")
    tokens = []
    for match in token_re.finditer(d):
        cmd, num = match.groups()
        tokens.append(cmd if cmd else num)
    return tokens


def sample_cubic_bezier(
    p0: Tuple[float, float],
    p1: Tuple[float, float],
    p2: Tuple[float, float],
    p3: Tuple[float, float],
    steps: int = 24
) -> List[Tuple[float, float]]:
    pts = []
    for i in range(1, steps + 1):
        t = i / steps
        mt = 1.0 - t
        x = (mt ** 3) * p0[0] + 3 * (mt ** 2) * t * p1[0] + 3 * mt * (t ** 2) * p2[0] + (t ** 3) * p3[0]
        y = (mt ** 3) * p0[1] + 3 * (mt ** 2) * t * p1[1] + 3 * mt * (t ** 2) * p2[1] + (t ** 3) * p3[1]
        pts.append((x, y))
    return pts


def sample_quad_bezier(
    p0: Tuple[float, float],
    p1: Tuple[float, float],
    p2: Tuple[float, float],
    steps: int = 16
) -> List[Tuple[float, float]]:
    pts = []
    for i in range(1, steps + 1):
        t = i / steps
        mt = 1.0 - t
        x = (mt ** 2) * p0[0] + 2 * mt * t * p1[0] + (t ** 2) * p2[0]
        y = (mt ** 2) * p0[1] + 2 * mt * t * p1[1] + (t ** 2) * p2[1]
        pts.append((x, y))
    return pts


def parse_svg_path_to_points(d: str) -> List[Tuple[float, float]]:
    """Chuyển đổi SVG path `d` thành danh sách điểm dày đặc trong hệ toạ độ gốc (0..109)."""
    tokens = tokenize_svg_path(d)
    idx = 0
    cur_x, cur_y = 0.0, 0.0
    start_x, start_y = 0.0, 0.0
    prev_ctrl_x, prev_ctrl_y = None, None
    prev_cmd = None
    points: List[Tuple[float, float]] = []

    def next_float() -> float:
        nonlocal idx
        val = float(tokens[idx])
        idx += 1
        return val

    def is_number(tok: str) -> bool:
        return tok not in "MmLlHhVvCcSsQqTtZz"

    cmd = None
    while idx < len(tokens):
        if not is_number(tokens[idx]):
            cmd = tokens[idx]
            idx += 1
        elif cmd is None:
            raise ValueError(f"Path không bắt đầu bằng lệnh: {d}")

        if cmd == "M":
            cur_x, cur_y = next_float(), next_float()
            start_x, start_y = cur_x, cur_y
            points.append((cur_x, cur_y))
            cmd = "L"
            prev_ctrl_x, prev_ctrl_y = None, None
        elif cmd == "m":
            cur_x += next_float()
            cur_y += next_float()
            start_x, start_y = cur_x, cur_y
            points.append((cur_x, cur_y))
            cmd = "l"
            prev_ctrl_x, prev_ctrl_y = None, None
        elif cmd == "L":
            nx, ny = next_float(), next_float()
            for s in range(1, 9):
                t = s / 8.0
                points.append((cur_x + (nx - cur_x) * t, cur_y + (ny - cur_y) * t))
            cur_x, cur_y = nx, ny
            prev_ctrl_x, prev_ctrl_y = None, None
        elif cmd == "l":
            dx, dy = next_float(), next_float()
            nx, ny = cur_x + dx, cur_y + dy
            for s in range(1, 9):
                t = s / 8.0
                points.append((cur_x + dx * t, cur_y + dy * t))
            cur_x, cur_y = nx, ny
            prev_ctrl_x, prev_ctrl_y = None, None
        elif cmd == "H":
            nx = next_float()
            for s in range(1, 9):
                t = s / 8.0
                points.append((cur_x + (nx - cur_x) * t, cur_y))
            cur_x = nx
            prev_ctrl_x, prev_ctrl_y = None, None
        elif cmd == "h":
            dx = next_float()
            nx = cur_x + dx
            for s in range(1, 9):
                t = s / 8.0
                points.append((cur_x + dx * t, cur_y))
            cur_x = nx
            prev_ctrl_x, prev_ctrl_y = None, None
        elif cmd == "V":
            ny = next_float()
            for s in range(1, 9):
                t = s / 8.0
                points.append((cur_x, cur_y + (ny - cur_y) * t))
            cur_y = ny
            prev_ctrl_x, prev_ctrl_y = None, None
        elif cmd == "v":
            dy = next_float()
            ny = cur_y + dy
            for s in range(1, 9):
                t = s / 8.0
                points.append((cur_x, cur_y + dy * t))
            cur_y = ny
            prev_ctrl_x, prev_ctrl_y = None, None
        elif cmd == "C":
            x1, y1 = next_float(), next_float()
            x2, y2 = next_float(), next_float()
            x3, y3 = next_float(), next_float()
            points.extend(sample_cubic_bezier((cur_x, cur_y), (x1, y1), (x2, y2), (x3, y3)))
            cur_x, cur_y = x3, y3
            prev_ctrl_x, prev_ctrl_y = x2, y2
        elif cmd == "c":
            x1, y1 = cur_x + next_float(), cur_y + next_float()
            x2, y2 = cur_x + next_float(), cur_y + next_float()
            x3, y3 = cur_x + next_float(), cur_y + next_float()
            points.extend(sample_cubic_bezier((cur_x, cur_y), (x1, y1), (x2, y2), (x3, y3)))
            cur_x, cur_y = x3, y3
            prev_ctrl_x, prev_ctrl_y = x2, y2
        elif cmd == "S":
            if prev_cmd in ("C", "c", "S", "s") and prev_ctrl_x is not None:
                x1, y1 = 2 * cur_x - prev_ctrl_x, 2 * cur_y - prev_ctrl_y
            else:
                x1, y1 = cur_x, cur_y
            x2, y2 = next_float(), next_float()
            x3, y3 = next_float(), next_float()
            points.extend(sample_cubic_bezier((cur_x, cur_y), (x1, y1), (x2, y2), (x3, y3)))
            cur_x, cur_y = x3, y3
            prev_ctrl_x, prev_ctrl_y = x2, y2
        elif cmd == "s":
            if prev_cmd in ("C", "c", "S", "s") and prev_ctrl_x is not None:
                x1, y1 = 2 * cur_x - prev_ctrl_x, 2 * cur_y - prev_ctrl_y
            else:
                x1, y1 = cur_x, cur_y
            x2, y2 = cur_x + next_float(), cur_y + next_float()
            x3, y3 = cur_x + next_float(), cur_y + next_float()
            points.extend(sample_cubic_bezier((cur_x, cur_y), (x1, y1), (x2, y2), (x3, y3)))
            cur_x, cur_y = x3, y3
            prev_ctrl_x, prev_ctrl_y = x2, y2
        elif cmd == "Q":
            x1, y1 = next_float(), next_float()
            x2, y2 = next_float(), next_float()
            points.extend(sample_quad_bezier((cur_x, cur_y), (x1, y1), (x2, y2)))
            cur_x, cur_y = x2, y2
            prev_ctrl_x, prev_ctrl_y = x1, y1
        elif cmd == "q":
            x1, y1 = cur_x + next_float(), cur_y + next_float()
            x2, y2 = cur_x + next_float(), cur_y + next_float()
            points.extend(sample_quad_bezier((cur_x, cur_y), (x1, y1), (x2, y2)))
            cur_x, cur_y = x2, y2
            prev_ctrl_x, prev_ctrl_y = x1, y1
        elif cmd == "T":
            if prev_cmd in ("Q", "q", "T", "t") and prev_ctrl_x is not None:
                x1, y1 = 2 * cur_x - prev_ctrl_x, 2 * cur_y - prev_ctrl_y
            else:
                x1, y1 = cur_x, cur_y
            x2, y2 = next_float(), next_float()
            points.extend(sample_quad_bezier((cur_x, cur_y), (x1, y1), (x2, y2)))
            cur_x, cur_y = x2, y2
            prev_ctrl_x, prev_ctrl_y = x1, y1
        elif cmd == "t":
            if prev_cmd in ("Q", "q", "T", "t") and prev_ctrl_x is not None:
                x1, y1 = 2 * cur_x - prev_ctrl_x, 2 * cur_y - prev_ctrl_y
            else:
                x1, y1 = cur_x, cur_y
            x2, y2 = cur_x + next_float(), cur_y + next_float()
            points.extend(sample_quad_bezier((cur_x, cur_y), (x1, y1), (x2, y2)))
            cur_x, cur_y = x2, y2
            prev_ctrl_x, prev_ctrl_y = x1, y1
        elif cmd in ("Z", "z"):
            cur_x, cur_y = start_x, start_y
            points.append((cur_x, cur_y))
            prev_ctrl_x, prev_ctrl_y = None, None
        else:
            raise ValueError(f"Lệnh SVG chưa hỗ trợ: {cmd}")

        prev_cmd = cmd

    return points


def resample_polyline(points: List[Tuple[float, float]], n: int = RESAMPLE_POINTS) -> List[Tuple[float, float]]:
    """Resample chuỗi điểm thành đúng n điểm cách đều nhau theo độ dài đường cong."""
    if not points:
        return [(0.5, 0.5)] * n
    if len(points) == 1:
        return [points[0]] * n

    cum_dist = [0.0]
    for i in range(1, len(points)):
        dx = points[i][0] - points[i - 1][0]
        dy = points[i][1] - points[i - 1][1]
        cum_dist.append(cum_dist[-1] + math.hypot(dx, dy))

    total_len = cum_dist[-1]
    if total_len < 1e-6:
        return [points[0]] * n

    resampled = [points[0]]
    step = total_len / (n - 1)
    seg_idx = 0

    for k in range(1, n - 1):
        target = k * step
        while seg_idx < len(cum_dist) - 2 and cum_dist[seg_idx + 1] < target:
            seg_idx += 1
        seg_start = cum_dist[seg_idx]
        seg_end = cum_dist[seg_idx + 1]
        seg_len = seg_end - seg_start
        if seg_len < 1e-9:
            resampled.append(points[seg_idx])
        else:
            ratio = (target - seg_start) / seg_len
            x = points[seg_idx][0] + ratio * (points[seg_idx + 1][0] - points[seg_idx][0])
            y = points[seg_idx][1] + ratio * (points[seg_idx + 1][1] - points[seg_idx][1])
            resampled.append((x, y))

    resampled.append(points[-1])
    return resampled


def ensure_kanjivg_svg(char: str, raw_dir: Path) -> Path:
    code_hex = f"{ord(char):05x}"
    svg_path = raw_dir / f"{code_hex}.svg"
    if svg_path.exists() and svg_path.stat().st_size > 100:
        return svg_path

    url = f"https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji/{code_hex}.svg"
    print(f"Downloading KanjiVG SVG for {char} ({code_hex}) from {url} ...")
    req = urllib.request.Request(url, headers={"User-Agent": "KanaAndroidBuildScript/1.0"})
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = resp.read()
    svg_path.write_bytes(data)
    return svg_path


def extract_strokes_from_svg(svg_path: Path) -> List[List[List[float]]]:
    content = svg_path.read_text(encoding="utf-8", errors="ignore")
    # Tìm các thẻ <path ... id="kvg:...-s<N>" ... d="..." />
    path_pattern = re.compile(r'<path[^>]*\bid="[^"]*-s(\d+)"[^>]*\bd="([^"]+)"', re.DOTALL)
    matches = path_pattern.findall(content)
    if not matches:
        # Thử chiều ngược lại: d trước id
        path_pattern_alt = re.compile(r'<path[^>]*\bd="([^"]+)"[^>]*\bid="[^"]*-s(\d+)"', re.DOTALL)
        alt_matches = path_pattern_alt.findall(content)
        matches = [(num, d) for (d, num) in alt_matches]

    # Sắp xếp theo thứ tự nét s1, s2, ...
    sorted_strokes = sorted(matches, key=lambda item: int(item[0]))
    strokes_normalized: List[List[List[float]]] = []

    for _, d_attr in sorted_strokes:
        raw_pts = parse_svg_path_to_points(d_attr)
        resampled = resample_polyline(raw_pts, RESAMPLE_POINTS)
        norm_pts = [
            [
                round(max(0.0, min(1.0, pt[0] / VIEWBOX_SIZE)), 4),
                round(max(0.0, min(1.0, pt[1] / VIEWBOX_SIZE)), 4),
            ]
            for pt in resampled
        ]
        strokes_normalized.append(norm_pts)

    return strokes_normalized


def build_entry(char: str, romaji: str, row: str, script_type: str, category: str, raw_dir: Path) -> Dict[str, Any]:
    svg_path = ensure_kanjivg_svg(char, raw_dir)
    strokes = extract_strokes_from_svg(svg_path)
    if not strokes:
        raise RuntimeError(f"Không tìm thấy nét nào trong {svg_path} cho chữ {char}")
    return {
        "char": char,
        "unicode": f"U+{ord(char):04X}",
        "romaji": romaji,
        "row": row,
        "script": script_type,      # "hiragana" | "katakana"
        "category": category,       # "basic" | "dakuten" | "small"
        "strokeCount": len(strokes),
        "strokes": strokes,
    }


def main() -> None:
    repo_root = Path(__file__).resolve().parent.parent
    raw_dir = repo_root / "scripts" / "kanjivg_raw"
    raw_dir.mkdir(parents=True, exist_ok=True)

    output_path = repo_root / "app" / "src" / "main" / "assets" / "strokes" / "kana_strokes.json"
    output_path.parent.mkdir(parents=True, exist_ok=True)

    characters: List[Dict[str, Any]] = []

    datasets = [
        (HIRAGANA_BASIC, "hiragana", "basic"),
        (HIRAGANA_DAKUTEN, "hiragana", "dakuten"),
        (HIRAGANA_SMALL, "hiragana", "small"),
        (KATAKANA_BASIC, "katakana", "basic"),
        (KATAKANA_DAKUTEN, "katakana", "dakuten"),
        (KATAKANA_SMALL, "katakana", "small"),
    ]

    for entries, script_type, category in datasets:
        for char, romaji, row in entries:
            entry = build_entry(char, romaji, row, script_type, category, raw_dir)
            characters.append(entry)

    payload = {
        "source": "KanjiVG (https://kanjivg.tagaini.net)",
        "license": "CC BY-SA 3.0 (Creative Commons Attribution-ShareAlike 3.0 Unported)",
        "copyright": "© 2009-2023 Ulrich Apel",
        "pointsPerStroke": RESAMPLE_POINTS,
        "coordinateRange": [0.0, 1.0],
        "totalCharacters": len(characters),
        "characters": characters,
    }

    output_path.write_text(
        json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"Đã xuất {len(characters)} chữ Kana vào {output_path} ({output_path.stat().st_size} bytes).")


if __name__ == "__main__":
    main()
