from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
DOCX = ROOT / "docs" / "generated" / "thesis_word_final_candidate_v11.docx"
RENDER_DIR = ROOT / "docs" / "generated" / "thesis_word_final_candidate_v11_render"
MIN_REFERENCES = 30


def run_step(command: list[str], title: str) -> None:
    print(f"\n== {title} ==")
    print(" ".join(command))
    subprocess.run(command, cwd=ROOT, check=True)


def required_figure_paths() -> list[Path]:
    script = ROOT / "scripts" / "build-thesis-word-draft.py"
    text = script.read_text(encoding="utf-8")
    names = sorted(set(re.findall(r'"figures/([^"]+\.png)"', text)))
    return [ROOT / "docs" / "generated" / "figures" / name for name in names]


def check_figures() -> None:
    missing = [path for path in required_figure_paths() if not path.exists()]
    if missing:
        details = "\n".join(f"- {path.relative_to(ROOT)}" for path in missing)
        raise SystemExit(f"Missing required thesis figures:\n{details}")
    print(f"Required figure check passed: {len(required_figure_paths())} files")


def docx_text(path: Path) -> str:
    ns = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
    with ZipFile(path) as archive:
        root = ET.fromstring(archive.read("word/document.xml"))
    return "".join(t.text or "" for t in root.iter(ns + "t"))


def docx_paragraph_texts(path: Path) -> list[str]:
    ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
    with ZipFile(path) as archive:
        root = ET.fromstring(archive.read("word/document.xml"))
    paragraphs = []
    for paragraph in root.findall(".//w:p", ns):
        text = "".join(node.text or "" for node in paragraph.findall(".//w:t", ns)).strip()
        if text:
            paragraphs.append(text)
    return paragraphs


def chapter_source_paths() -> list[Path]:
    thesis_dir = ROOT / "docs" / "05_thesis"
    return [
        thesis_dir / "chapter1_intro.md",
        thesis_dir / "chapter2_related_work.md",
        thesis_dir / "chapter3_analysis.md",
        thesis_dir / "chapter4_design.md",
        thesis_dir / "chapter5_implementation.md",
        thesis_dir / "chapter6_test.md",
        thesis_dir / "chapter7_conclusion.md",
    ]


def expand_caption_ranges(text: str, kind: str) -> set[str]:
    expanded: set[str] = set()
    pattern = rf"{kind}\s*(\d+)\s*[-－]\s*(\d+)\s*[至到]\s*(?:{kind}\s*)?(\d+)\s*[-－]\s*(\d+)"
    for start_ch, start_no, end_ch, end_no in re.findall(pattern, text):
        if start_ch != end_ch:
            continue
        for number in range(int(start_no), int(end_no) + 1):
            expanded.add(f"{kind} {start_ch}-{number}")
    return expanded


def references_in_body(text: str, kind: str) -> set[str]:
    found = {
        f"{kind} {chapter}-{number}"
        for chapter, number in re.findall(rf"{kind}\s*(\d+)\s*[-－]\s*(\d+)", text)
    }
    return found | expand_caption_ranges(text, kind)


def formula_references_in_body(text: str) -> set[str]:
    found = {
        f"式 {chapter}-{number}"
        for chapter, number in re.findall(r"式\s*（\s*(\d+)\s*[-－]\s*(\d+)\s*）", text)
    }
    return found


def check_source_caption_references() -> None:
    figure_captions: set[str] = set()
    table_captions: set[str] = set()
    formula_markers: set[str] = set()
    body_parts: list[str] = []

    for path in chapter_source_paths():
        raw = path.read_text(encoding="utf-8")
        for chapter, number in re.findall(r"^\s*>?\s*图\s*(\d+)\s*[-－]\s*(\d+)\s+", raw, flags=re.MULTILINE):
            figure_captions.add(f"图 {chapter}-{number}")
        for chapter, number in re.findall(r"^\s*表\s*(\d+)\s*[-－]\s*(\d+)\s+", raw, flags=re.MULTILINE):
            table_captions.add(f"表 {chapter}-{number}")
        for chapter, number in re.findall(r"^\s*(?:公式|式)\s*（\s*(\d+)\s*[-－]\s*(\d+)\s*）\s*$", raw, flags=re.MULTILINE):
            formula_markers.add(f"式 {chapter}-{number}")

        without_captions = re.sub(r"^\s*>?\s*图\s*\d+\s*[-－]\s*\d+\s+.*$", "", raw, flags=re.MULTILINE)
        without_captions = re.sub(r"^\s*表\s*\d+\s*[-－]\s*\d+\s+.*$", "", without_captions, flags=re.MULTILINE)
        without_captions = re.sub(r"^\s*(?:公式|式)\s*（\s*\d+\s*[-－]\s*\d+\s*）\s*$", "", without_captions, flags=re.MULTILINE)
        body_parts.append(without_captions)

    body = "\n".join(body_parts)
    figure_refs = references_in_body(body, "图")
    table_refs = references_in_body(body, "表")
    formula_refs = formula_references_in_body(body)

    missing_figures = sorted(figure_captions - figure_refs)
    missing_tables = sorted(table_captions - table_refs)
    missing_formulas = sorted(formula_markers - formula_refs)

    print("\n== Source figure/table/formula reference check ==")
    print(f"figure captions: {len(figure_captions)}; referenced: {len(figure_captions - set(missing_figures))}")
    print(f"table captions: {len(table_captions)}; referenced: {len(table_captions - set(missing_tables))}")
    print(f"formula markers: {len(formula_markers)}; referenced: {len(formula_markers - set(missing_formulas))}")

    if missing_figures or missing_tables or missing_formulas:
        if missing_figures:
            print("Missing figure references: " + ", ".join(missing_figures))
        if missing_tables:
            print("Missing table references: " + ", ".join(missing_tables))
        if missing_formulas:
            print("Missing formula references: " + ", ".join(missing_formulas))
        raise SystemExit("Source figure/table/formula reference check failed")


def source_formula_marker_count() -> int:
    count = 0
    for path in chapter_source_paths():
        raw = path.read_text(encoding="utf-8")
        count += len(
            re.findall(
                r"^\s*(?:公式|式)\s*（\s*\d+\s*[-－]\s*\d+\s*）\s*$",
                raw,
                flags=re.MULTILINE,
            )
        )
    return count


def check_native_formulas(path: Path) -> None:
    with ZipFile(path) as archive:
        document_xml = archive.read("word/document.xml")

    formula_markers = source_formula_marker_count()
    omath_count = document_xml.count(b"<m:oMath")
    drawing_count = document_xml.count(b"<w:drawing")

    print("\n== Native formula check ==")
    print(f"source formula markers: {formula_markers}")
    print(f"OfficeMath objects: {omath_count}")
    print(f"Word drawing objects: {drawing_count}")

    if omath_count < formula_markers:
        raise SystemExit("Native formula check failed: formula markers must be rendered as OfficeMath objects")
    if drawing_count > len(required_figure_paths()):
        raise SystemExit("Native formula check failed: extra drawing objects suggest formulas may still be images")


def check_table_rows_cant_split(path: Path) -> None:
    ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
    with ZipFile(path) as archive:
        root = ET.fromstring(archive.read("word/document.xml"))

    total_rows = 0
    missing = 0
    for row in root.findall(".//w:tr", ns):
        total_rows += 1
        tr_pr = row.find("w:trPr", ns)
        if tr_pr is None or tr_pr.find("w:cantSplit", ns) is None:
            missing += 1

    print("\n== Table row split check ==")
    print(f"table rows: {total_rows}")
    print(f"rows missing cantSplit: {missing}")
    if missing:
        raise SystemExit("Table row split check failed")


def check_table_spacing(path: Path) -> None:
    ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
    with ZipFile(path) as archive:
        root = ET.fromstring(archive.read("word/document.xml"))

    rows = root.findall(".//w:tbl//w:tr", ns)
    cells = root.findall(".//w:tbl//w:tc", ns)
    rows_without_height = 0
    cells_without_margins = 0

    for row in rows:
        tr_pr = row.find("w:trPr", ns)
        if tr_pr is None or tr_pr.find("w:trHeight", ns) is None:
            rows_without_height += 1

    for cell in cells:
        tc_pr = cell.find("w:tcPr", ns)
        margins = tc_pr.find("w:tcMar", ns) if tc_pr is not None else None
        if margins is None:
            cells_without_margins += 1

    print("\n== Table spacing check ==")
    print(f"table rows: {len(rows)}")
    print(f"rows missing min height: {rows_without_height}")
    print(f"table cells: {len(cells)}")
    print(f"cells missing margins: {cells_without_margins}")

    if rows_without_height or cells_without_margins:
        raise SystemExit("Table spacing check failed")


def check_body_paragraph_alignment(path: Path) -> None:
    ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
    w_ns = ns["w"]
    with ZipFile(path) as archive:
        root = ET.fromstring(archive.read("word/document.xml"))

    in_body = False
    body_paragraphs = 0
    missing: list[str] = []
    for paragraph in root.findall(".//w:p", ns):
        text = "".join(node.text or "" for node in paragraph.findall(".//w:t", ns)).strip()
        if text == "引言":
            in_body = True
            continue
        if text == "参考文献":
            in_body = False
        if not in_body or not text:
            continue

        p_pr = paragraph.find("w:pPr", ns)
        if p_pr is None:
            continue
        indent = p_pr.find("w:ind", ns)
        if indent is None or f"{{{w_ns}}}firstLine" not in indent.attrib:
            continue

        body_paragraphs += 1
        jc = p_pr.find("w:jc", ns)
        value = jc.attrib.get(f"{{{w_ns}}}val") if jc is not None else None
        if value != "both":
            missing.append(text[:80])

    print("\n== Body paragraph alignment check ==")
    print(f"body paragraphs with first-line indent: {body_paragraphs}")
    print(f"paragraphs not justified: {len(missing)}")

    if missing:
        for item in missing[:20]:
            print(f"Not justified: {item}")
        raise SystemExit("Body paragraph alignment check failed")


def run_text(run, ns: dict[str, str]) -> str:
    return "".join(node.text or "" for node in run.findall(".//w:t", ns))


def run_font_props(run, ns: dict[str, str]) -> tuple[str | None, str | None]:
    w_ns = ns["w"]
    r_pr = run.find("w:rPr", ns)
    if r_pr is None:
        return None, None
    fonts = r_pr.find("w:rFonts", ns)
    size = r_pr.find("w:sz", ns)
    east_asia = fonts.attrib.get(f"{{{w_ns}}}eastAsia") if fonts is not None else None
    sz = size.attrib.get(f"{{{w_ns}}}val") if size is not None else None
    return east_asia, sz


def check_docx_typography(path: Path) -> None:
    ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
    with ZipFile(path) as archive:
        root = ET.fromstring(archive.read("word/document.xml"))

    parent_map = {child: parent for parent in root.iter() for child in parent}

    def inside_table(node) -> bool:
        cursor = node
        while cursor in parent_map:
            cursor = parent_map[cursor]
            if cursor.tag == f"{{{ns['w']}}}tbl":
                return True
        return False

    def paragraph_style(paragraph) -> str | None:
        p_pr = paragraph.find("w:pPr", ns)
        p_style = p_pr.find("w:pStyle", ns) if p_pr is not None else None
        return p_style.attrib.get(f"{{{ns['w']}}}val") if p_style is not None else None

    heading_requirements = {
        "1": ("黑体", "32", "一级标题"),
        "2": ("黑体", "30", "二级标题"),
        "3": ("黑体", "28", "三级标题"),
    }
    standalone_heading_texts = {"中文摘要", "ABSTRACT", "目    录", "参考文献", "致    谢"}
    heading_checked = 0
    body_checked = 0
    ref_checked = 0
    errors: list[str] = []
    in_body = False
    in_references = False

    for paragraph in root.findall(".//w:p", ns):
        text = "".join(node.text or "" for node in paragraph.findall(".//w:t", ns)).strip()
        if not text:
            continue
        if text == "引言":
            in_body = True
            in_references = False
        elif text == "参考文献":
            in_body = False
            in_references = True
        elif text == "致    谢":
            in_references = False

        style_id = paragraph_style(paragraph)
        if style_id == "af0" and text in standalone_heading_texts:
            expected_font, expected_size, label = "黑体", "32", "非目录标题"
            for run in paragraph.findall("w:r", ns):
                if not run_text(run, ns).strip():
                    continue
                east_asia, sz = run_font_props(run, ns)
                if east_asia != expected_font or sz != expected_size:
                    errors.append(f"{label}字体不符：{text[:40]} eastAsia={east_asia} sz={sz}")
                heading_checked += 1
            continue

        if style_id in heading_requirements:
            expected_font, expected_size, label = heading_requirements[style_id]
            for run in paragraph.findall("w:r", ns):
                if not run_text(run, ns).strip():
                    continue
                east_asia, sz = run_font_props(run, ns)
                if east_asia != expected_font or sz != expected_size:
                    errors.append(f"{label}字体不符：{text[:40]} eastAsia={east_asia} sz={sz}")
                heading_checked += 1
            continue

        if inside_table(paragraph):
            continue
        if text.startswith(("图 ", "表 ", "表名：")):
            continue
        if re.fullmatch(r"（式\d+-\d+）", text):
            continue

        if in_references and text.startswith("["):
            for run in paragraph.findall("w:r", ns):
                if not run_text(run, ns).strip():
                    continue
                east_asia, sz = run_font_props(run, ns)
                if east_asia != "宋体" or sz != "21":
                    errors.append(f"参考文献字体不符：{text[:40]} eastAsia={east_asia} sz={sz}")
                ref_checked += 1
        elif in_body:
            for run in paragraph.findall("w:r", ns):
                if not run_text(run, ns).strip():
                    continue
                east_asia, sz = run_font_props(run, ns)
                if east_asia != "宋体" or sz != "24":
                    errors.append(f"正文字体不符：{text[:40]} eastAsia={east_asia} sz={sz}")
                body_checked += 1

    print("\n== DOCX typography check ==")
    print(f"heading runs checked: {heading_checked}")
    print(f"body runs checked: {body_checked}")
    print(f"reference runs checked: {ref_checked}")
    print(f"typography errors: {len(errors)}")
    if errors:
        for item in errors[:30]:
            print(item)
        raise SystemExit("DOCX typography check failed")


def check_module_consistency() -> None:
    modules = [
        "用户画像构建模块",
        "匹配推荐生成模块",
        "推荐解释与用户反馈模块",
    ]
    files = [
        ROOT / "docs" / "05_thesis" / "chapter3_analysis.md",
        ROOT / "docs" / "05_thesis" / "chapter4_design.md",
        ROOT / "docs" / "05_thesis" / "chapter5_implementation.md",
    ]

    print("\n== Chapter 3-5 module consistency check ==")
    for path in files:
        text = path.read_text(encoding="utf-8")
        positions = [text.find(module) for module in modules]
        print(f"{path.name}: " + ", ".join(str(pos) for pos in positions))
        if any(pos < 0 for pos in positions) or positions != sorted(positions):
            raise SystemExit(f"Module consistency check failed: {path.relative_to(ROOT)}")


def check_caption_integrity(path: Path) -> None:
    paragraphs = docx_paragraph_texts(path)
    figures = [item for item in paragraphs if re.fullmatch(r"图\s+\d+-\d+\s+.+", item)]
    tables = [item for item in paragraphs if re.fullmatch(r"表\s+\d+-\d+\s+.+", item)]
    figure_numbers = [re.match(r"图\s+\d+-\d+", item).group(0) for item in figures]
    table_numbers = [re.match(r"表\s+\d+-\d+", item).group(0) for item in tables]

    duplicate_titles = [
        title
        for title, count in Counter(figures + tables).items()
        if count > 1
    ]
    duplicate_numbers = [
        number
        for number, count in Counter(figure_numbers + table_numbers).items()
        if count > 1
    ]
    print("\n== Caption integrity check ==")
    print(f"figures: {len(figures)}")
    print(f"tables: {len(tables)}")
    print(f"duplicate caption titles: {len(duplicate_titles)}")
    print(f"duplicate caption numbers: {len(duplicate_numbers)}")

    if duplicate_titles or duplicate_numbers:
        for item in duplicate_titles[:20]:
            print(f"Duplicate title: {item}")
        for item in duplicate_numbers[:20]:
            print(f"Duplicate number: {item}")
        raise SystemExit("Caption integrity check failed")


def scan_docx(path: Path, strict: bool) -> None:
    text = docx_text(path)
    terms = ["图表占位", "表格占位", "待补", "TODO", "FIXME"]
    counts = {term: text.count(term) for term in terms}
    print("\n== DOCX scan ==")
    print(f"{path.relative_to(ROOT)} characters: {len(text)}")
    for term, count in counts.items():
        print(f"{term}: {count}")
    placeholders = re.findall(r"【[^】]*(?:占位|待补|TODO|FIXME)[^】]*】", text)
    if placeholders:
        print("Placeholder snippets:")
        for item in placeholders[:20]:
            print(f"- {item[:180]}")
    if strict and any(counts.values()):
        raise SystemExit("DOCX scan failed in strict mode")
    check_caption_integrity(path)


def check_references() -> None:
    references_path = ROOT / "docs" / "05_thesis" / "references.md"
    source_paths = chapter_source_paths()
    references = references_path.read_text(encoding="utf-8")
    ref_nums = [int(item) for item in re.findall(r"^\[(\d+)\]", references, flags=re.MULTILINE)]
    body = "\n".join(path.read_text(encoding="utf-8") for path in source_paths)
    citations = [int(item) for item in re.findall(r"\[(\d+)\]", body)]

    print("\n== Reference check ==")
    print(f"references: {len(ref_nums)}")
    print(f"citations: {len(citations)}")

    if len(ref_nums) < MIN_REFERENCES:
        raise SystemExit(f"Reference check failed: expected at least {MIN_REFERENCES} references")
    expected = list(range(1, len(ref_nums) + 1))
    if ref_nums != expected:
        raise SystemExit("Reference check failed: references.md numbering must be continuous from 1")
    missing = sorted(set(citations) - set(ref_nums))
    uncited = sorted(set(ref_nums) - set(citations))
    if missing:
        raise SystemExit(f"Reference check failed: missing reference entries for citations {missing}")
    if uncited:
        raise SystemExit(f"Reference check failed: uncited reference entries {uncited}")
    if any(left > right for left, right in zip(citations, citations[1:])):
        raise SystemExit("Reference check failed: citation numbers must follow first-appearance order")
    forbidden = ["使用说明", "不要在正文中引用未列入本页", "当前仓库未保存"]
    leaked = [term for term in forbidden if term in references]
    if leaked:
        raise SystemExit(f"Reference check failed: non-bibliography notes leaked into references.md: {leaked}")
    print("Reference check passed")


def find_soffice() -> Path | None:
    for name in ("soffice", "libreoffice"):
        found = shutil.which(name)
        if found:
            return Path(found)
    app_path = Path("/Applications/LibreOffice.app/Contents/MacOS/soffice")
    if app_path.exists():
        return app_path
    return None


def find_renderer() -> Path | None:
    env = os.environ.get("DOCX_RENDERER")
    candidates = []
    if env:
        candidates.append(Path(env).expanduser())
    documents_cache = (
        Path.home()
        / ".codex"
        / "plugins"
        / "cache"
        / "openai-primary-runtime"
        / "documents"
    )
    if documents_cache.exists():
        candidates.extend(
            sorted(
                documents_cache.glob("*/skills/documents/render_docx.py"),
                key=lambda item: item.parent.parent.parent.name,
                reverse=True,
            )
        )
    for candidate in candidates:
        if candidate.exists():
            return candidate
    return None


def render_docx(force: bool) -> None:
    soffice = find_soffice()
    renderer = find_renderer()
    print("\n== DOCX render ==")
    if not soffice:
        message = "LibreOffice/soffice not found; visual render QA skipped"
        if force:
            raise SystemExit(message)
        print(message)
        return
    if not renderer:
        message = "render_docx.py not found; visual render QA skipped"
        if force:
            raise SystemExit(message)
        print(message)
        return
    RENDER_DIR.mkdir(parents=True, exist_ok=True)
    stale_pdf = RENDER_DIR / f"{DOCX.stem}.pdf"
    if stale_pdf.exists():
        stale_pdf.unlink()
    try:
        run_step(
            [sys.executable, str(renderer), str(DOCX), "--output_dir", str(RENDER_DIR)],
            "Render DOCX pages without PDF export",
        )
    except subprocess.CalledProcessError:
        message = "DOCX render failed. In Codex sandbox on macOS, rerun this command outside the sandbox or use --render to fail hard."
        if force:
            raise
        print(message)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate thesis figures, rebuild Word draft, and run structural checks.")
    parser.add_argument("--render", action="store_true", help="Require DOCX page rendering through LibreOffice.")
    parser.add_argument("--no-render", action="store_true", help="Skip render probing entirely.")
    parser.add_argument("--no-strict", action="store_true", help="Do not fail on placeholder markers in generated DOCX.")
    args = parser.parse_args()

    run_step([sys.executable, "scripts/generate-thesis-figures.py"], "Generate thesis figures")
    check_figures()
    check_references()
    check_source_caption_references()
    check_module_consistency()
    run_step([sys.executable, "scripts/build-thesis-word-draft.py"], "Build thesis Word candidate")
    scan_docx(DOCX, strict=not args.no_strict)
    check_native_formulas(DOCX)
    check_table_rows_cant_split(DOCX)
    check_table_spacing(DOCX)
    check_body_paragraph_alignment(DOCX)
    check_docx_typography(DOCX)
    if not args.no_render:
        render_docx(force=args.render)


if __name__ == "__main__":
    main()
