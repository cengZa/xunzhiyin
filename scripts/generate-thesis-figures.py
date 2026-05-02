from __future__ import annotations

import hashlib
import math
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs" / "generated" / "figures"

FONT_CANDIDATES = [
    "/System/Library/Fonts/Supplemental/Songti.ttc",
    "/System/Library/Fonts/Hiragino Sans GB.ttc",
    "/System/Library/Fonts/STHeiti Medium.ttc",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
    "C:/Windows/Fonts/msyh.ttc",
    "C:/Windows/Fonts/simsun.ttc",
]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = FONT_CANDIDATES
    if bold:
        candidates = [
            "/System/Library/Fonts/STHeiti Medium.ttc",
            "/System/Library/Fonts/Hiragino Sans GB.ttc",
            *FONT_CANDIDATES,
        ]
    for candidate in candidates:
        path = Path(candidate)
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default(size=size)


def rgb(color: str) -> tuple[int, int, int]:
    named = {
        "white": (255, 255, 255),
        "black": (0, 0, 0),
    }
    if color.lower() in named:
        return named[color.lower()]
    color = color.lstrip("#")
    return tuple(int(color[i : i + 2], 16) for i in (0, 2, 4))


def canvas(width: int, height: int) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (width, height), "white")
    return image, ImageDraw.Draw(image)


def text_size(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.ImageFont) -> tuple[int, int]:
    if not text:
        return 0, 0
    box = draw.textbbox((0, 0), text, font=fnt)
    return box[2] - box[0], box[3] - box[1]


def wrap_text(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.ImageFont, max_width: int) -> list[str]:
    lines: list[str] = []
    for raw_line in str(text).split("\n"):
        raw_line = raw_line.strip()
        if not raw_line:
            lines.append("")
            continue
        current = ""
        for char in raw_line:
            candidate = current + char
            if current and text_size(draw, candidate, fnt)[0] > max_width:
                lines.append(current)
                current = char
            else:
                current = candidate
        if current:
            lines.append(current)
    return lines or [""]


def draw_text(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    size: int = 24,
    fill: str = "#111827",
    bold: bool = False,
    align: str = "center",
    valign: str = "center",
) -> None:
    fnt = font(size, bold)
    x, y, w, h = box
    lines = wrap_text(draw, text, fnt, max(20, w - 16))
    line_h = max(text_size(draw, "国", fnt)[1] + 7, int(size * 1.35))
    total_h = line_h * len(lines)
    if valign == "top":
        cursor_y = y + 8
    else:
        cursor_y = y + max(0, (h - total_h) // 2)
    for line in lines:
        tw, _ = text_size(draw, line, fnt)
        if align == "left":
            tx = x + 10
        elif align == "right":
            tx = x + w - tw - 10
        else:
            tx = x + (w - tw) // 2
        draw.text((tx, cursor_y), line, font=fnt, fill=rgb(fill))
        cursor_y += line_h


def draw_fit_single_line(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    size: int = 17,
    fill: str = "#111827",
    bold: bool = True,
    min_size: int = 11,
) -> None:
    x, y, w, h = box
    chosen_size = size
    while chosen_size > min_size:
        fnt = font(chosen_size, bold)
        if text_size(draw, text, fnt)[0] <= max(20, w - 14):
            break
        chosen_size -= 1
    fnt = font(chosen_size, bold)
    tw, th = text_size(draw, text, fnt)
    draw.text((x + (w - tw) // 2, y + (h - th) // 2 - 1), text, font=fnt, fill=rgb(fill))


def title(draw: ImageDraw.ImageDraw, text: str, width: int) -> None:
    draw_text(draw, (0, 24, width, 48), text, 30, "#1f2937", True)


def box(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    w: int,
    h: int,
    text: str,
    fill: str = "#eef6ff",
    stroke: str = "#2f5f9f",
    size: int = 18,
    radius: int = 14,
) -> None:
    draw.rounded_rectangle((x, y, x + w, y + h), radius=radius, fill=rgb(fill), outline=rgb(stroke), width=3)
    draw_text(draw, (x + 6, y + 4, w - 12, h - 8), text, size=size, bold=True)


def plain_box(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    w: int,
    h: int,
    text: str,
    size: int = 17,
    bold: bool = False,
) -> None:
    draw.rectangle((x, y, x + w, y + h), fill="white", outline=rgb("#111111"), width=3)
    draw_text(draw, (x + 6, y + 4, w - 12, h - 8), text, size=size, bold=bold)


def dashed_rect(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int) -> None:
    dash = 14
    gap = 10
    for xx in range(x, x + w, dash + gap):
        draw.line((xx, y, min(xx + dash, x + w), y), fill=rgb("#111111"), width=2)
        draw.line((xx, y + h, min(xx + dash, x + w), y + h), fill=rgb("#111111"), width=2)
    for yy in range(y, y + h, dash + gap):
        draw.line((x, yy, x, min(yy + dash, y + h)), fill=rgb("#111111"), width=2)
        draw.line((x + w, yy, x + w, min(yy + dash, y + h)), fill=rgb("#111111"), width=2)


def arrow(draw: ImageDraw.ImageDraw, x1: int, y1: int, x2: int, y2: int, color: str = "#374151") -> None:
    draw.line((x1, y1, x2, y2), fill=rgb(color), width=3)
    angle = math.atan2(y2 - y1, x2 - x1)
    length = 14
    spread = 0.45
    p1 = (x2 - length * math.cos(angle - spread), y2 - length * math.sin(angle - spread))
    p2 = (x2 - length * math.cos(angle + spread), y2 - length * math.sin(angle + spread))
    draw.polygon([(x2, y2), p1, p2], fill=rgb(color))


def actor(draw: ImageDraw.ImageDraw, x: int, y: int, label: str) -> None:
    color = rgb("#111827")
    draw.ellipse((x + 24, y, x + 50, y + 26), outline=color, width=3)
    draw.line((x + 37, y + 26, x + 37, y + 82), fill=color, width=3)
    draw.line((x + 8, y + 48, x + 66, y + 48), fill=color, width=3)
    draw.line((x + 37, y + 82, x + 14, y + 120), fill=color, width=3)
    draw.line((x + 37, y + 82, x + 60, y + 120), fill=color, width=3)
    draw_text(draw, (x - 20, y + 126, 116, 42), label, 17)


def use_case(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, text: str) -> None:
    draw.ellipse((x, y, x + w, y + h), fill="white", outline=rgb("#111827"), width=3)
    draw_text(draw, (x + 8, y + 4, w - 16, h - 8), text, 18)


def class_box(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, title_text: str, members: list[str]) -> None:
    draw.rectangle((x, y, x + w, y + h), fill="white", outline=rgb("#111827"), width=3)
    draw.rectangle((x, y, x + w, y + 42), fill=rgb("#e8f0fb"), outline=rgb("#111827"), width=1)
    draw_fit_single_line(draw, (x + 4, y, w - 8, 42), title_text, 17, bold=True)
    content_x, content_y = x + 20, y + 52
    content_w, content_h = w - 34, h - 62
    chosen_size = 15
    chosen_lines: list[str] = []
    chosen_line_h = 0
    for candidate_size in range(15, 9, -1):
        fnt = font(candidate_size)
        lines = wrap_text(draw, "\n".join(members), fnt, max(20, content_w))
        line_h = max(text_size(draw, "国", fnt)[1] + 4, int(candidate_size * 1.25))
        if len(lines) * line_h <= content_h:
            chosen_size = candidate_size
            chosen_lines = lines
            chosen_line_h = line_h
            break
    if not chosen_lines:
        fnt = font(chosen_size)
        chosen_lines = wrap_text(draw, "\n".join(members), fnt, max(20, content_w))
        chosen_line_h = max(text_size(draw, "国", fnt)[1] + 4, int(chosen_size * 1.25))
    fnt = font(chosen_size)
    cursor_y = content_y + 2
    for line in chosen_lines:
        draw.text((content_x, cursor_y), line, font=fnt, fill=rgb("#111827"))
        cursor_y += chosen_line_h


def lane(draw: ImageDraw.ImageDraw, x: int, top: int, bottom: int, label: str) -> None:
    draw.line((x, top, x, bottom), fill=rgb("#9ca3af"), width=3)
    draw.rounded_rectangle((x - 90, top - 60, x + 90, top - 14), radius=14, fill=rgb("#f8fafc"), outline=rgb("#64748b"), width=3)
    draw_fit_single_line(draw, (x - 84, top - 60, 168, 46), label, 15, "#111827", True, min_size=10)


def message(draw: ImageDraw.ImageDraw, x1: int, x2: int, y: int, text: str) -> None:
    arrow(draw, x1, y, x2, y)
    left = min(x1, x2)
    draw_text(draw, (left, y - 34, abs(x2 - x1), 28), text, 15)


def save(image: Image.Image, name: str) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    bbox = image.getbbox()
    if bbox:
        pixels = image.load()
        min_x, min_y = image.width, image.height
        max_x, max_y = 0, 0
        for y in range(image.height):
            for x in range(image.width):
                r, g, b = pixels[x, y]
                if r < 248 or g < 248 or b < 248:
                    min_x = min(min_x, x)
                    min_y = min(min_y, y)
                    max_x = max(max_x, x)
                    max_y = max(max_y, y)
        pad = 28
        if max_x > min_x and max_y > min_y:
            image = image.crop(
                (
                    max(0, min_x - pad),
                    max(0, min_y - pad),
                    min(image.width, max_x + pad),
                    min(image.height, max_y + pad),
                )
            )
    out = OUT_DIR / name
    image.save(out)
    print(out)


def table_image(name: str, headers: list[str], rows: list[list[str]], col_widths: list[int] | None = None) -> None:
    if col_widths is None:
        base = {2: [360, 900], 3: [280, 520, 580], 4: [180, 300, 470, 470], 5: [120, 300, 220, 260, 520]}
        col_widths = base.get(len(headers), [260] * len(headers))
    row_heights = [58]
    probe, probe_draw = canvas(10, 10)
    for row in rows:
        wrapped = 1
        for i, cell in enumerate(row):
            lines = wrap_text(probe_draw, cell, font(16), max(60, col_widths[i] - 18))
            wrapped = max(wrapped, len(lines))
        row_heights.append(max(58, 24 + wrapped * 28))
    width = sum(col_widths) + 160
    height = sum(row_heights) + 90
    image, draw = canvas(width, height)
    x, y = 80, 40
    table_w = sum(col_widths)
    table_h = sum(row_heights)
    draw.rectangle((x, y, x + table_w, y + row_heights[0]), fill=rgb("#e8f0fb"))
    draw.line((x, y, x + table_w, y), fill=rgb("#111111"), width=3)
    draw.line((x, y + row_heights[0], x + table_w, y + row_heights[0]), fill=rgb("#111111"), width=3)
    draw.line((x, y + table_h, x + table_w, y + table_h), fill=rgb("#111111"), width=3)
    cy = y
    for r_idx, row in enumerate([headers] + rows):
        cx = x
        h = row_heights[r_idx]
        if r_idx > 1:
            draw.line((x, cy, x + table_w, cy), fill=rgb("#111111"), width=1)
        for c_idx, cell in enumerate(row):
            draw.line((cx, cy, cx, cy + h), fill=rgb("#111111"), width=1)
            align = "center" if r_idx == 0 or c_idx == 0 else "left"
            draw_text(draw, (cx + 5, cy + 4, col_widths[c_idx] - 10, h - 8), cell, 16, bold=r_idx == 0, align=align)
            cx += col_widths[c_idx]
        draw.line((x + table_w, cy, x + table_w, cy + h), fill=rgb("#111111"), width=1)
        cy += h
    save(image, name)


def split_markdown_row(line: str) -> list[str]:
    line = line.strip()
    if line.startswith("|"):
        line = line[1:]
    if line.endswith("|"):
        line = line[:-1]
    return [cell.strip().replace("`", "").replace("<br/>", "\n") for cell in line.split("|")]


def generate_markdown_tables() -> None:
    paths = [
        ROOT / "docs" / "05_thesis" / "chapter2_related_work.md",
        ROOT / "docs" / "05_thesis" / "chapter3_analysis.md",
        ROOT / "docs" / "05_thesis" / "chapter4_design.md",
        ROOT / "docs" / "05_thesis" / "chapter5_implementation.md",
        ROOT / "docs" / "05_thesis" / "chapter6_test.md",
    ]
    for path in paths:
        lines = path.read_text(encoding="utf-8").splitlines()
        i = 0
        while i < len(lines):
            if (
                lines[i].strip().startswith("|")
                and i + 1 < len(lines)
                and re.match(r"^\s*\|?\s*:?-{3,}", lines[i + 1])
            ):
                table_lines: list[str] = []
                while i < len(lines) and lines[i].strip().startswith("|"):
                    table_lines.append(lines[i].rstrip())
                    i += 1
                headers = split_markdown_row(table_lines[0])
                rows = [split_markdown_row(line) for line in table_lines[2:]]
                key = hashlib.md5("\n".join(table_lines).encode("utf-8")).hexdigest()[:12]
                table_image(f"table-{key}.png", headers, rows)
                continue
            i += 1


def fig_31() -> None:
    image, draw = canvas(1450, 700)
    title(draw, "用户与标签维护用例图", 1450)
    draw.rectangle((330, 105, 1090, 605), outline=rgb("#111827"), width=3)
    draw_text(draw, (500, 122, 430, 36), "校园社交匹配推荐系统", 20, bold=True)
    actor(draw, 120, 210, "校园用户")
    actor(draw, 1180, 210, "系统管理员")
    for args in [
        (475, 180, 220, 70, "维护个人资料"),
        (475, 310, 220, 70, "维护兴趣标签"),
        (735, 180, 220, 70, "维护标签体系"),
        (735, 310, 220, 70, "维护基础用户数据"),
        (600, 450, 230, 70, "保存用户标签关系"),
    ]:
        use_case(draw, *args)
    for args in [(190, 285, 475, 215), (190, 285, 475, 345), (1180, 285, 955, 215), (1180, 285, 955, 345), (585, 380, 660, 450), (845, 380, 760, 450)]:
        arrow(draw, *args)
    save(image, "ch3-1-user-tag-usecase.png")


def fig_32() -> None:
    image, draw = canvas(1500, 760)
    title(draw, "画像构建与推荐生成用例图", 1500)
    draw.rectangle((330, 110, 1160, 650), outline=rgb("#111827"), width=3)
    draw_text(draw, (555, 128, 410, 36), "校园社交匹配推荐系统", 20, bold=True)
    actor(draw, 105, 255, "校园用户")
    actor(draw, 1240, 255, "系统管理员")
    use_cases = [
        (455, 175, 250, 72, "请求推荐结果"),
        (455, 315, 250, 72, "获取或构建用户画像"),
        (790, 235, 250, 72, "候选用户召回"),
        (790, 385, 250, 72, "排序与规则重排"),
        (560, 520, 280, 72, "返回 Top-K 推荐结果"),
        (885, 520, 230, 72, "重建倒排索引"),
    ]
    for args in use_cases:
        use_case(draw, *args)

    # Connectors end at the oval boundaries to avoid lines cutting into use-case text.
    for args in [
        (185, 330, 455, 211),
        (580, 247, 580, 315),
        (705, 350, 790, 271),
        (915, 307, 915, 385),
        (915, 457, 760, 520),
        (1240, 330, 1115, 556),
    ]:
        arrow(draw, *args)
    save(image, "ch3-2-recommendation-usecase.png")


def fig_33() -> None:
    image, draw = canvas(1400, 660)
    title(draw, "推荐解释用例图", 1400)
    draw.rectangle((340, 115, 1060, 565), outline=rgb("#111827"), width=3)
    draw_text(draw, (500, 132, 430, 36), "校园社交匹配推荐系统", 20, bold=True)
    actor(draw, 120, 235, "校园用户")
    actor(draw, 1160, 235, "外部大语言\n模型服务")
    for args in [
        (450, 190, 240, 72, "查看推荐解释"),
        (710, 190, 240, 72, "抽取解释证据"),
        (450, 350, 240, 72, "生成规则解释"),
        (710, 350, 240, 72, "改写解释文本"),
    ]:
        use_case(draw, *args)
    for args in [(190, 310, 450, 226), (690, 226, 710, 226), (570, 262, 570, 350), (690, 386, 710, 386), (1160, 310, 950, 386)]:
        arrow(draw, *args)
    save(image, "ch3-3-explanation-usecase.png")


def fig_34() -> None:
    image, draw = canvas(1400, 680)
    title(draw, "用户反馈与画像更新用例图", 1400)
    draw.rectangle((350, 115, 1070, 575), outline=rgb("#111827"), width=3)
    draw_text(draw, (510, 132, 430, 36), "校园社交匹配推荐系统", 20, bold=True)
    actor(draw, 130, 250, "校园用户")
    for args in [
        (455, 185, 240, 72, "提交关注/忽略反馈"),
        (745, 185, 220, 72, "保存反馈记录"),
        (455, 355, 240, 72, "读取推荐证据标签"),
        (745, 355, 220, 72, "重建用户画像"),
    ]:
        use_case(draw, *args)
    for args in [(200, 325, 455, 221), (695, 221, 745, 221), (855, 257, 575, 355), (695, 391, 745, 391)]:
        arrow(draw, *args)
    save(image, "ch3-4-feedback-usecase.png")


def fig_41() -> None:
    image, draw = canvas(1650, 1180)

    # Reference style: the Liu engineering sample uses a monochrome system boundary,
    # external systems on both sides, internal service groups, and data components below.
    for y, label in [
        (90, "校园用户"),
        (290, "系统管理员"),
        (490, "前端页面"),
        (690, "测试/演示入口"),
    ]:
        plain_box(draw, 70, y, 150, 150, label, 18, True)
        arrow(draw, 220, y + 75, 345, y + 75, "black")

    draw.rectangle((350, 40, 1290, 1090), outline=rgb("#111111"), width=3)
    draw_text(draw, (650, 58, 340, 36), "校园社交匹配推荐系统", 20)

    plain_box(draw, 410, 110, 820, 76, "前端展示层", 18)
    plain_box(draw, 445, 128, 750, 34, "画像展示  推荐列表  透明链路  解释反馈  评估对比", 16)

    arrow(draw, 820, 190, 820, 245, "black")
    draw_text(draw, (842, 198, 80, 30), "HTTP", 15)

    draw.rectangle((410, 250, 1230, 815), outline=rgb("#111111"), width=3)
    draw_text(draw, (760, 262, 130, 34), "服务端", 18)

    dashed_rect(draw, 455, 320, 730, 130)
    for i, label in enumerate(["用户服务", "标签服务", "画像服务", "推荐服务"]):
        plain_box(draw, 485 + i * 170, 360, 125, 50, label, 15)

    arrow(draw, 820, 452, 820, 505, "black")
    draw_text(draw, (842, 462, 80, 30), "调用", 15)

    dashed_rect(draw, 455, 510, 730, 130)
    for i, label in enumerate(["召回服务", "排序服务", "重排服务", "解释服务"]):
        plain_box(draw, 485 + i * 170, 550, 125, 50, label, 15)

    arrow(draw, 820, 642, 820, 695, "black")
    draw_text(draw, (842, 652, 80, 30), "调用", 15)

    dashed_rect(draw, 455, 700, 730, 80)
    for i, label in enumerate(["反馈服务", "评估服务", "缓存仓储"]):
        plain_box(draw, 540 + i * 215, 718, 140, 44, label, 15)

    plain_box(draw, 430, 910, 300, 120, "数据存储\nMySQL\n用户/标签/画像/推荐/解释/反馈", 16)
    plain_box(draw, 910, 910, 300, 120, "缓存索引\nRedis\n标签倒排索引/画像缓存/推荐缓存", 16)
    arrow(draw, 610, 815, 610, 910, "black")
    arrow(draw, 1060, 815, 1060, 910, "black")

    plain_box(draw, 1400, 200, 150, 250, "外部大语言\n模型服务", 17, True)
    draw_text(draw, (1312, 320, 78, 30), "调用服务", 15)
    arrow(draw, 1230, 605, 1400, 325, "black")
    arrow(draw, 1400, 360, 1230, 635, "black")

    plain_box(draw, 1400, 590, 150, 270, "后续扩展服务\n\n消息通知\n数据分析", 16, True)
    draw_text(draw, (1310, 700, 82, 30), "预留接口", 15)
    arrow(draw, 1230, 740, 1400, 720, "black")
    save(image, "ch4-1-system-architecture.png")


def fig_42() -> None:
    image, draw = canvas(1700, 920)
    title(draw, "系统功能模块结构图", 1700)
    plain_box(draw, 565, 105, 570, 62, "校园社交匹配推荐系统", 20, True)
    main_y = 255
    child_y = 365
    modules = [
        ("基础数据维护", ["用户资料维护", "标签体系维护", "用户标签绑定"], 130, 230),
        ("用户画像构建模块", ["标签权重计算", "时间衰减处理", "Top-K 标签截取", "画像缓存管理"], 475, 250),
        ("匹配推荐生成模块", ["候选用户召回", "相似度排序", "校园规则重排", "可信连接评分"], 845, 250),
        ("推荐解释与用户反馈模块", ["解释证据抽取", "透明链路展示", "用户反馈更新", "评估结果导出"], 1235, 290),
    ]
    root_bottom = (850, 167)
    trunk_y = 215
    draw.line((root_bottom[0], root_bottom[1], root_bottom[0], trunk_y), fill=rgb("#111111"), width=3)
    draw.line((245, trunk_y, 1380, trunk_y), fill=rgb("#111111"), width=3)
    for heading, items, x, w in modules:
        center = x + w // 2
        draw.line((center, trunk_y, center, main_y), fill=rgb("#111111"), width=3)
        plain_box(draw, x, main_y, w, 62, heading, 16, True)
        rail_x = x + 10
        branch_y = child_y - 34
        draw.line((center, main_y + 62, center, branch_y), fill=rgb("#111111"), width=2)
        draw.line((center, branch_y, rail_x, branch_y), fill=rgb("#111111"), width=2)
        child_centers = []
        for idx, item in enumerate(items):
            y = child_y + idx * 95
            child_centers.append(y + 28)
            plain_box(draw, x + 24, y, w - 48, 56, item, 15, False)
        if child_centers:
            draw.line((rail_x, branch_y, rail_x, child_centers[-1]), fill=rgb("#111111"), width=2)
        for y_center in child_centers:
            draw.line((rail_x, y_center, x + 24, y_center), fill=rgb("#111111"), width=2)
    save(image, "ch4-2-function-structure.png")


def diamond(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, text: str, size: int = 16) -> None:
    points = [(x + w // 2, y), (x + w, y + h // 2), (x + w // 2, y + h), (x, y + h // 2)]
    draw.polygon(points, fill="white", outline=rgb("#111111"))
    draw.line(points + [points[0]], fill=rgb("#111111"), width=3)
    draw_text(draw, (x + 10, y + 10, w - 20, h - 20), text, size=size, bold=True)


def cylinder(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, text: str, size: int = 16) -> None:
    stroke = rgb("#111111")
    top_h = max(34, min(46, h // 2))
    top_mid = y + top_h // 2
    bottom_mid = y + h - top_h // 2
    draw.rectangle((x, top_mid, x + w, bottom_mid), fill="white")
    draw.line((x, top_mid, x, bottom_mid), fill=stroke, width=3)
    draw.line((x + w, top_mid, x + w, bottom_mid), fill=stroke, width=3)
    draw.ellipse((x, y, x + w, y + top_h), fill="white", outline=stroke, width=3)
    draw.arc((x, y + h - top_h, x + w, y + h), start=0, end=180, fill=stroke, width=3)
    draw_text(draw, (x + 12, y + top_h + 4, w - 24, h - top_h - 12), text, size=size, bold=True)


def fig_43() -> None:
    image, draw = canvas(1680, 980)
    title(draw, "系统核心流程图", 1680)
    top = [
        (80, 160, 190, 58, "开始"),
        (340, 160, 240, 58, "维护资料与标签"),
        (650, 160, 240, 58, "构建用户画像"),
        (960, 160, 240, 58, "候选用户召回"),
        (1270, 160, 260, 58, "排序与规则重排"),
    ]
    bottom = [
        (1270, 525, 260, 58, "生成推荐解释"),
        (960, 525, 240, 58, "展示推荐链路"),
        (650, 525, 240, 58, "记录用户反馈"),
        (340, 525, 240, 58, "更新用户画像"),
        (80, 525, 190, 58, "结束"),
    ]
    for x, y, w, h, text in top + bottom:
        if text in {"开始", "结束"}:
            draw.rounded_rectangle((x, y, x + w, y + h), radius=28, fill="white", outline=rgb("#111111"), width=3)
            draw_text(draw, (x, y, w, h), text, 18, bold=True)
        else:
            plain_box(draw, x, y, w, h, text, 17, True)
    for idx in range(len(top) - 1):
        x, y, w, h, _ = top[idx]
        nx, ny, nw, nh, _ = top[idx + 1]
        arrow(draw, x + w, y + h // 2, nx, ny + nh // 2, "black")
    arrow(draw, 1400, 218, 1400, 525, "black")
    for idx in range(len(bottom) - 1):
        x, y, w, h, _ = bottom[idx]
        nx, ny, nw, nh, _ = bottom[idx + 1]
        arrow(draw, x, y + h // 2, nx + nw, ny + nh // 2, "black")

    cylinder(draw, 620, 300, 300, 110, "MySQL\n用户/标签/画像", 15)
    arrow(draw, 770, 300, 770, 218, "black")
    plain_box(draw, 960, 300, 260, 82, "Redis\n标签倒排索引", 15)
    arrow(draw, 1090, 300, 1090, 218, "black")
    plain_box(draw, 1265, 300, 270, 82, "规则重排\n专业/年级/社团", 15)
    arrow(draw, 1400, 300, 1400, 218, "black")
    diamond(draw, 640, 680, 260, 84, "反馈类型", 16)
    arrow(draw, 770, 583, 770, 680, "black")
    arrow(draw, 640, 722, 580, 555, "black")
    draw_text(draw, (535, 620, 72, 30), "关注", 14)
    arrow(draw, 900, 722, 960, 555, "black")
    draw_text(draw, (930, 620, 72, 30), "忽略", 14)
    save(image, "ch4-3-core-flow.png")


def fig_44() -> None:
    image, draw = canvas(1860, 1120)

    def entity(x: int, y: int, w: int, h: int, text: str) -> None:
        plain_box(draw, x, y, w, h, text, 20, True)

    def attr(cx: int, cy: int, text: str, w: int = 150) -> None:
        h = 54
        draw.ellipse((cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2), fill="white", outline=rgb("#111111"), width=2)
        draw_text(draw, (cx - w // 2 + 8, cy - h // 2 + 5, w - 16, h - 10), text, 15)

    def relation(x: int, y: int, w: int, h: int, text: str) -> None:
        diamond(draw, x, y, w, h, text, 16)

    def connector(points: list[tuple[int, int]], width: int = 3) -> None:
        draw.line(points, fill=rgb("#111111"), width=width)

    entities = {
        "用户": (70, 265, 190, 68),
        "标签": (515, 265, 190, 68),
        "用户画像": (965, 265, 220, 68),
        "推荐结果": (1445, 265, 230, 68),
        "推荐解释": (1445, 650, 230, 68),
        "用户反馈": (900, 650, 220, 68),
    }

    rels = {
        "选择": (310, 250, 150, 98),
        "生成": (760, 250, 150, 98),
        "产生": (1245, 250, 150, 98),
        "对应": (1490, 455, 140, 94),
        "反馈": (1200, 640, 150, 94),
        "更新": (720, 520, 150, 94),
    }
    attrs = [
        (105, 180, "id", 110, (130, 207), (120, 265)),
        (250, 190, "学号", 120, (230, 217), (215, 265)),
        (165, 430, "年级/专业", 170, (165, 403), (165, 333)),
        (610, 180, "tag_name", 180, (610, 207), (610, 265)),
        (610, 430, "tag_type", 160, (610, 403), (610, 333)),
        (1075, 180, "画像版本", 170, (1075, 207), (1075, 265)),
        (1160, 430, "核心标签", 160, (1160, 403), (1120, 333)),
        (1560, 180, "final_score", 180, (1560, 207), (1560, 265)),
        (1770, 300, "trace_id", 150, (1695, 300), (1675, 299)),
        (1770, 685, "reason_text", 180, (1680, 685), (1675, 684)),
        (1560, 830, "evidence_json", 200, (1560, 803), (1560, 718)),
        (1010, 830, "feedback_type", 200, (1010, 803), (1010, 718)),
        (750, 800, "feedback_time", 200, (850, 800), (900, 700)),
    ]

    for points in [
        [(260, 299), (310, 299)],
        [(460, 299), (515, 299)],
        [(705, 299), (760, 299)],
        [(910, 299), (965, 299)],
        [(1185, 299), (1245, 299)],
        [(1395, 299), (1445, 299)],
        [(1560, 333), (1560, 455)],
        [(1560, 549), (1560, 650)],
        [(1505, 333), (1275, 640)],
        [(1200, 687), (1120, 684)],
        [(900, 684), (795, 614)],
        [(795, 520), (1075, 333)],
    ]:
        connector(points)
    for _, _, _, _, start, end in attrs:
        connector([start, end], width=2)

    for text, args in entities.items():
        entity(*args, text)
    for text, args in rels.items():
        relation(*args, text)
    for cx, cy, text, w, _, _ in attrs:
        attr(cx, cy, text, w)

    for x, y, label in [
        (275, 265, "1"),
        (470, 265, "n"),
        (720, 265, "n"),
        (920, 265, "1"),
        (1205, 265, "1"),
        (1410, 265, "n"),
        (1530, 355, "1"),
        (1530, 620, "1"),
    ]:
        draw_text(draw, (x, y, 34, 26), label, 15)
    save(image, "ch4-4-conceptual-model.png")


def fig_45() -> None:
    image, draw = canvas(1240, 1040)
    title(draw, "数据存储物理模型示意图", 1240)
    tables = [
        (60, 125, 270, 150, "user", ["PK id", "student_no", "nickname", "grade/major"]),
        (470, 125, 300, 150, "user_tag_relation", ["PK id", "IDX user_id", "IDX tag_id", "selected_at"]),
        (910, 125, 270, 145, "tag", ["PK id", "tag_name", "tag_type", "status"]),
        (60, 430, 300, 155, "user_profile", ["PK id", "IDX user_id", "profile_json", "topk_json"]),
        (455, 430, 345, 165, "recommendation_result", ["PK id", "IDX request_user_id", "IDX target_user_id", "final_score", "trace_id"]),
        (880, 430, 320, 155, "recommendation_explanation", ["PK id", "IDX recommendation_id", "reason_text", "evidence_json"]),
        (455, 740, 345, 155, "user_feedback", ["PK id", "IDX request_user_id", "IDX recommendation_id", "feedback_type"]),
    ]
    for args in tables:
        class_box(draw, *args)
    for args in [
        (330, 200, 470, 200),
        (770, 200, 910, 198),
        (210, 275, 210, 430),
        (360, 508, 455, 508),
        (800, 508, 880, 508),
        (625, 595, 625, 740),
    ]:
        arrow(draw, *args)
    draw_text(draw, (390, 325, 500, 54), "完整字段与类型见表 4-1 至表 4-7", 22, bold=True)
    save(image, "ch4-5-physical-model.png")


def fig_51() -> None:
    image, draw = canvas(1500, 650)
    title(draw, "用户画像构建模块业务流程图", 1500)
    steps = [
        (70, 150, 190, 60, "开始"),
        (325, 150, 220, 60, "接收画像构建请求"),
        (610, 150, 220, 60, "读取用户标签关系"),
        (895, 150, 220, 60, "计算标签权重"),
        (1180, 150, 220, 60, "截取 Top-K 标签"),
        (895, 370, 220, 60, "保存画像结果"),
        (610, 370, 220, 60, "写入 Redis 缓存"),
        (325, 370, 220, 60, "返回画像结果"),
    ]
    for x, y, w, h, text in steps:
        if text in {"开始", "返回画像结果"}:
            draw.rounded_rectangle((x, y, x + w, y + h), radius=26, fill="white", outline=rgb("#111111"), width=3)
            draw_text(draw, (x, y, w, h), text, 18, bold=True)
        else:
            plain_box(draw, x, y, w, h, text, 17, True)
    for idx in range(4):
        x, y, w, h, _ = steps[idx]
        nx, ny, nw, nh, _ = steps[idx + 1]
        arrow(draw, x + w, y + h // 2, nx, ny + nh // 2, "black")
    arrow(draw, 1290, 210, 1005, 370, "black")
    for idx in range(5, 7):
        x, y, w, h, _ = steps[idx]
        nx, ny, nw, nh, _ = steps[idx + 1]
        arrow(draw, x, y + h // 2, nx + nw, ny + nh // 2, "black")
    cylinder(draw, 590, 270, 260, 90, "MySQL\n用户/标签关系", 14)
    arrow(draw, 720, 270, 720, 210, "black")
    plain_box(draw, 905, 270, 250, 78, "权重计算策略\nTF-IDF/时间衰减", 14)
    arrow(draw, 1010, 270, 1010, 210, "black")
    cylinder(draw, 1180, 355, 240, 88, "MySQL\nuser_profile", 14)
    arrow(draw, 1180, 398, 1115, 398, "black")
    plain_box(draw, 600, 485, 240, 78, "Redis\n画像缓存", 14)
    arrow(draw, 720, 485, 720, 430, "black")
    save(image, "ch5-1-profile-business-flow.png")


def fig_52() -> None:
    image, draw = canvas(1700, 900)
    title(draw, "用户画像构建模块类图", 1700)
    for args in [
        (90, 150, 260, 130, "ProfileController", ["+ buildProfile()", "+ getProfile()"]),
        (470, 130, 300, 210, "ProfileServiceImpl", ["- weightCalculator", "- userTagRelationMapper", "- userProfileMapper", "- profileCacheRepository", "+ buildProfile()", "+ getProfile()"]),
        (900, 130, 320, 140, "ProfileWeightCalculator", ["+ calculate(userId, relations)"]),
        (900, 340, 320, 150, "ImprovedTfIdfProfileWeightCalculator", ["+ calculate()", "- applyTimeDecay()", "- selectTopK()"]),
        (120, 560, 280, 130, "UserTagRelationMapper", ["+ selectList()"]),
        (510, 560, 260, 130, "UserProfileMapper", ["+ insert()", "+ selectOne()"]),
        (900, 560, 300, 130, "ProfileCacheRepository", ["+ get()", "+ save()"]),
    ]:
        class_box(draw, *args)
    for args in [(350, 215, 470, 215), (770, 215, 900, 200), (1060, 270, 1060, 340), (610, 340, 260, 560), (630, 340, 640, 560), (680, 340, 1030, 560)]:
        arrow(draw, *args)
    save(image, "ch5-2-profile-class-diagram.png")


def fig_53() -> None:
    image, draw = canvas(1700, 980)
    title(draw, "用户画像生成时序图", 1700)
    lanes = [180, 430, 680, 930, 1180, 1430]
    labels = ["用户", "ProfileController", "ProfileService", "标签关系Mapper", "权重计算器", "画像存储/缓存"]
    for x, label in zip(lanes, labels):
        lane(draw, x, 130, 900, label)
    for args in [
        (180, 430, 190, "请求生成/刷新画像"),
        (430, 680, 280, "buildProfile(userId)"),
        (680, 930, 370, "查询用户标签关系"),
        (930, 680, 460, "返回标签、时间、权重种子"),
        (680, 1180, 550, "计算 TF-IDF、时间衰减、Top-K"),
        (1180, 680, 640, "返回画像权重向量"),
        (680, 1430, 730, "保存画像并写入缓存"),
        (680, 430, 820, "返回画像结果"),
    ]:
        message(draw, *args)
    save(image, "ch5-3-profile-sequence.png")


def fig_55() -> None:
    image, draw = canvas(1540, 670)
    title(draw, "匹配推荐生成模块业务流程图", 1540)
    steps = [
        (60, 150, 180, 60, "开始"),
        (295, 150, 220, 60, "接收推荐请求"),
        (570, 150, 220, 60, "读取目标画像"),
        (845, 150, 230, 60, "召回候选用户"),
        (1135, 150, 250, 60, "计算基础相似度"),
        (1135, 385, 250, 60, "执行校园规则重排"),
        (845, 385, 230, 60, "保存结果与证据"),
        (570, 385, 220, 60, "返回推荐详情"),
    ]
    for x, y, w, h, text in steps:
        if text in {"开始", "返回推荐详情"}:
            draw.rounded_rectangle((x, y, x + w, y + h), radius=26, fill="white", outline=rgb("#111111"), width=3)
            draw_text(draw, (x, y, w, h), text, 18, bold=True)
        else:
            plain_box(draw, x, y, w, h, text, 17, True)
    for idx in range(4):
        x, y, w, h, _ = steps[idx]
        nx, ny, nw, nh, _ = steps[idx + 1]
        arrow(draw, x + w, y + h // 2, nx, ny + nh // 2, "black")
    arrow(draw, 1260, 210, 1260, 385, "black")
    for idx in range(5, 7):
        x, y, w, h, _ = steps[idx]
        nx, ny, nw, nh, _ = steps[idx + 1]
        arrow(draw, x, y + h // 2, nx + nw, ny + nh // 2, "black")
    plain_box(draw, 830, 270, 260, 76, "Redis\n标签倒排索引", 14)
    arrow(draw, 960, 270, 960, 210, "black")
    plain_box(draw, 1135, 270, 250, 76, "排序策略\n余弦相似度/标签贡献", 14)
    arrow(draw, 1260, 270, 1260, 210, "black")
    plain_box(draw, 1135, 500, 250, 82, "重排规则\n专业/年级/社团/探索", 14)
    arrow(draw, 1260, 500, 1260, 445, "black")
    cylinder(draw, 820, 500, 280, 88, "MySQL\nrecommendation_result", 13)
    arrow(draw, 960, 500, 960, 445, "black")
    save(image, "ch5-5-recommendation-business-flow.png")


def fig_56() -> None:
    image, draw = canvas(1720, 860)
    title(draw, "匹配推荐生成模块类图", 1720)
    class_box(draw, 70, 120, 280, 120, "RecommendationController", ["+ recommend()", "+ detail()"])
    class_box(
        draw,
        700,
        90,
        340,
        190,
        "RecommendationServiceImpl",
        ["- profileService", "- recallService", "- rankingService", "- rerankService", "- explanationService", "+ recommend()"],
    )
    top_classes = [
        ("ProfileService", 60, 410),
        ("RecallService", 360, 410),
        ("RankingService", 660, 410),
        ("RerankService", 960, 410),
        ("TrustScoreService", 1260, 410),
    ]
    bottom_classes = [
        ("ExplorationService", 210, 640),
        ("RecallIndexRepository", 510, 640),
        ("RerankRule", 810, 640),
        ("RecommendationResultMapper", 1110, 640),
        ("ExplanationService", 1410, 640),
    ]
    for name, x, y in top_classes + bottom_classes:
        class_box(draw, x, y, 230, 105, name, ["+ execute()"])

    arrow(draw, 350, 180, 700, 185)
    service_center_x = 870
    bus_y = 345
    draw.line((service_center_x, 280, service_center_x, bus_y), fill=rgb("#6b7280"), width=3)
    draw.line((175, bus_y, 1525, bus_y), fill=rgb("#6b7280"), width=3)
    for _, x, y in top_classes + bottom_classes:
        arrow(draw, x + 115, bus_y, x + 115, y, "#6b7280")
    save(image, "ch5-6-recommendation-class-diagram.png")


def fig_57() -> None:
    image, draw = canvas(1700, 960)
    title(draw, "匹配推荐生成时序图", 1700)
    lanes = [150, 380, 610, 840, 1070, 1300, 1530]
    labels = ["用户", "RecommendationController", "RecommendationService", "ProfileService", "RecallService", "RankingService", "RerankService"]
    for x, label in zip(lanes, labels):
        lane(draw, x, 130, 900, label)
    for args in [
        (150, 380, 190, "请求 Top-K 推荐"),
        (380, 610, 280, "recommend(userId, topK)"),
        (610, 840, 370, "获取目标用户画像"),
        (610, 1070, 460, "按 Top-K 标签召回候选"),
        (610, 1300, 550, "计算余弦相似度"),
        (610, 1530, 640, "执行校园规则重排"),
        (1530, 610, 730, "返回最终排序"),
        (610, 380, 820, "返回推荐详情"),
    ]:
        message(draw, *args)
    save(image, "ch5-7-recommendation-sequence.png")


def fig_59() -> None:
    image, draw = canvas(1540, 690)
    title(draw, "推荐解释与用户反馈模块业务流程图", 1540)
    steps = [
        (55, 150, 180, 60, "开始"),
        (295, 150, 250, 60, "读取结果与证据"),
        (605, 150, 250, 60, "抽取贡献与规则"),
        (915, 150, 250, 60, "生成规则解释"),
        (1225, 150, 250, 60, "按需改写解释"),
        (915, 385, 250, 60, "展示解释与链路"),
        (605, 385, 250, 60, "保存用户反馈"),
        (295, 385, 250, 60, "触发画像更新"),
    ]
    for x, y, w, h, text in steps:
        if text == "开始":
            draw.rounded_rectangle((x, y, x + w, y + h), radius=26, fill="white", outline=rgb("#111111"), width=3)
            draw_text(draw, (x, y, w, h), text, 18, bold=True)
        else:
            plain_box(draw, x, y, w, h, text, 17, True)
    for idx in range(4):
        x, y, w, h, _ = steps[idx]
        nx, ny, nw, nh, _ = steps[idx + 1]
        arrow(draw, x + w, y + h // 2, nx, ny + nh // 2, "black")
    arrow(draw, 1350, 210, 1040, 385, "black")
    for idx in range(5, 7):
        x, y, w, h, _ = steps[idx]
        nx, ny, nw, nh, _ = steps[idx + 1]
        arrow(draw, x, y + h // 2, nx + nw, ny + nh // 2, "black")
    cylinder(draw, 285, 270, 280, 88, "MySQL\n推荐结果/解释证据", 13)
    arrow(draw, 425, 270, 425, 210, "black")
    plain_box(draw, 1225, 270, 250, 82, "外部大语言模型服务\n仅改写解释文本", 13)
    arrow(draw, 1350, 270, 1350, 210, "black")
    plain_box(draw, 915, 500, 250, 82, "前端页面\n透明链路/解释/反馈", 13)
    arrow(draw, 1040, 500, 1040, 445, "black")
    cylinder(draw, 605, 500, 250, 88, "MySQL\nuser_feedback", 13)
    arrow(draw, 730, 500, 730, 445, "black")
    draw.rounded_rectangle((55, 385, 180, 445), radius=26, fill="white", outline=rgb("#111111"), width=3)
    draw_text(draw, (55, 385, 125, 60), "结束", 18, bold=True)
    arrow(draw, 295, 415, 180, 415, "black")
    save(image, "ch5-9-explanation-feedback-business-flow.png")


def fig_510() -> None:
    image, draw = canvas(1700, 950)
    title(draw, "推荐解释与用户反馈模块类图", 1700)
    for args in [
        (80, 110, 280, 130, "RecommendationController", ["+ getExplanation()"]),
        (80, 640, 280, 130, "FeedbackController", ["+ submitFeedback()"]),
        (470, 80, 340, 220, "ExplanationServiceImpl", ["- evidenceExtractor", "- templateBuilder", "- aiClient", "- explanationMapper", "+ generate()", "+ getByRecommendationId()"]),
        (470, 610, 340, 170, "FeedbackServiceImpl", ["- feedbackMapper", "- profileService", "+ submitFeedback()", "+ applyFeedbackUpdate()"]),
        (930, 60, 430, 118, "ExplanationEvidenceExtractor", ["+ extract()"]),
        (930, 200, 430, 118, "ExplanationTemplateBuilder", ["+ build()"]),
        (930, 340, 430, 118, "AiExplanationClient", ["+ generateExplanation()"]),
        (930, 480, 430, 118, "RecommendationExplanationMapper", ["+ insert()", "+ selectOne()"]),
        (930, 640, 330, 130, "UserFeedbackMapper", ["+ insert()", "+ selectList()"]),
    ]:
        class_box(draw, *args)
    for args in [
        (360, 175, 470, 190),
        (360, 705, 470, 695),
        (810, 130, 930, 119),
        (810, 180, 930, 259),
        (810, 225, 930, 399),
        (810, 270, 930, 539),
        (810, 695, 930, 705),
    ]:
        arrow(draw, *args)
    save(image, "ch5-10-explanation-feedback-class-diagram.png")


def fig_511() -> None:
    image, draw = canvas(1700, 940)
    title(draw, "用户反馈更新时序图", 1700)
    lanes = [170, 430, 690, 950, 1210, 1470]
    labels = ["用户", "FeedbackController", "FeedbackService", "UserFeedbackMapper", "RecommendationResultMapper", "ProfileService"]
    for x, label in zip(lanes, labels):
        lane(draw, x, 130, 880, label)
    for args in [
        (170, 430, 200, "提交关注/忽略反馈"),
        (430, 690, 300, "submitFeedback()"),
        (690, 950, 400, "保存反馈记录"),
        (690, 1210, 500, "读取推荐证据标签"),
        (690, 690, 600, "调整相关标签权重"),
        (690, 1470, 700, "重建用户画像"),
        (690, 430, 800, "返回处理结果"),
    ]:
        message(draw, *args)
    save(image, "ch5-11-feedback-sequence.png")


def screenshot_placeholder(name: str, heading: str, body: str) -> None:
    image, draw = canvas(1600, 920)
    draw.rectangle((0, 0, 1600, 76), fill=rgb("#f3f4f6"))
    draw_text(draw, (60, 14, 680, 48), heading, 24, "#111827", True, "left")
    draw.rectangle((120, 150, 1480, 790), fill="white", outline=rgb("#111827"), width=4)
    draw.line((120, 150, 1480, 790), fill=rgb("#d1d5db"), width=3)
    draw.line((1480, 150, 120, 790), fill=rgb("#d1d5db"), width=3)
    draw_text(draw, (250, 330, 1100, 120), "界面效果图占位", 36, "#111827", True)
    draw_text(draw, (330, 470, 940, 150), body, 24, "#374151", False)
    save(image, name)


def use_real_screenshot(name: str, source_name: str) -> bool:
    source = ROOT / "docs" / "generated" / "screenshots" / source_name
    if not source.exists():
        return False
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as image:
        image.convert("RGB").save(OUT_DIR / name)
    return True


def screenshot_home() -> None:
    if use_real_screenshot("ch5-4-home-screenshot.png", "real_home_recommendation_section.png"):
        return
    screenshot_placeholder(
        "ch5-4-home-screenshot.png",
        "系统首页画像与推荐展示界面",
        "后续替换为系统实际运行截图：展示目标用户画像、核心标签、推荐列表和推荐理由入口。",
    )


def screenshot_pipeline() -> None:
    if use_real_screenshot("ch5-8-pipeline-screenshot.png", "real_pipeline_final_stage.png"):
        return
    screenshot_placeholder(
        "ch5-8-pipeline-screenshot.png",
        "透明链路页面推荐生成阶段界面",
        "后续替换为系统实际运行截图：展示输入标签、画像构建、候选召回、排序重排和解释证据。",
    )


def screenshot_feedback() -> None:
    if use_real_screenshot("ch5-12-feedback-screenshot.png", "real_pipeline_explanation_stage.png"):
        return
    screenshot_placeholder(
        "ch5-12-feedback-screenshot.png",
        "推荐解释与反馈展示界面",
        "后续替换为系统实际运行截图：展示推荐解释、证据明细、关注或忽略反馈及画像更新结果。",
    )


def special_tables() -> None:
    table_image(
        "ch5-table-1-core-mechanisms.png",
        ["机制", "作用", "实现要点"],
        [
            ["候选召回", "缩小排序计算范围", "根据 Top-K 标签读取倒排集合并合并候选用户"],
            ["基础排序", "计算兴趣相似度", "使用画像向量余弦相似度，并记录标签贡献"],
            ["场景重排", "适配校园匹配场景", "按学习、社团、兴趣模式调整规则权重"],
            ["可信连接分", "抑制证据不足的推荐对象", "根据资料完整度和历史反馈生成可信原因"],
            ["轻量探索", "保留少量非最高相似度对象", "在不破坏主排序的前提下增加推荐多样性"],
        ],
        [250, 480, 650],
    )
    table_image(
        "ch5-table-2-explanation-feedback-data.png",
        ["数据项", "含义", "用途"],
        [
            ["标签贡献", "共同标签及其对相似度的贡献", "支撑推荐解释和画像更新"],
            ["规则命中", "年级、专业、社团等校园规则命中情况", "支撑场景重排和解释生成"],
            ["可信连接原因", "资料完整度、反馈记录等可信依据", "支撑可信连接分和解释展示"],
            ["反馈类型", "关注或忽略", "决定画像权重调整方向"],
            ["画像版本", "用户画像更新后的版本号", "支撑反馈前后结果对比"],
        ],
        [280, 570, 500],
    )


def main() -> None:
    for fn in [
        fig_31,
        fig_32,
        fig_33,
        fig_34,
        fig_41,
        fig_42,
        fig_43,
        fig_44,
        fig_45,
        fig_51,
        fig_52,
        fig_53,
        screenshot_home,
        fig_55,
        fig_56,
        fig_57,
        screenshot_pipeline,
        fig_59,
        fig_510,
        fig_511,
        screenshot_feedback,
    ]:
        fn()


if __name__ == "__main__":
    main()
