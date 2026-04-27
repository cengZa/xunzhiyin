from __future__ import annotations

import hashlib
import re
from pathlib import Path

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, Twips
from docx.table import Table
from docx.text.paragraph import Paragraph


ROOT = Path(__file__).resolve().parents[1]
TEMPLATE = ROOT / "docs" / "本科毕设论文模板-论文主体.docx"
OUT = ROOT / "docs" / "generated" / "thesis_word_final_candidate_v8.docx"

SOURCES = [
    ("abstract_cn", ROOT / "docs" / "05_thesis" / "abstract_cn.md"),
    ("abstract_en", ROOT / "docs" / "05_thesis" / "abstract_en.md"),
    ("chapter", ROOT / "docs" / "05_thesis" / "chapter1_intro.md"),
    ("chapter", ROOT / "docs" / "05_thesis" / "chapter2_related_work.md"),
    ("chapter", ROOT / "docs" / "05_thesis" / "chapter3_analysis.md"),
    ("chapter", ROOT / "docs" / "05_thesis" / "chapter4_design.md"),
    ("chapter", ROOT / "docs" / "05_thesis" / "chapter5_implementation.md"),
    ("chapter", ROOT / "docs" / "05_thesis" / "chapter6_test.md"),
    ("chapter", ROOT / "docs" / "05_thesis" / "chapter7_conclusion.md"),
    ("references", ROOT / "docs" / "05_thesis" / "references.md"),
]

TOC_ENTRIES = [
    ("中文摘要", "i"),
    ("ABSTRACT", "ii"),
    ("目    录", "iii"),
    ("1 引言", "1"),
    ("2 相关理论及技术综述", "4"),
    ("3 校园社交匹配推荐系统需求分析", "8"),
    ("4 校园社交匹配推荐系统概要设计", "13"),
    ("5 校园社交匹配推荐系统详细设计与实现", "18"),
    ("6 系统测试", "26"),
    ("7 总结与展望", "31"),
    ("参考文献", "34"),
    ("致谢", "36"),
]

FIGURE_IMAGES = {
    "图 1-1": ("figures/ch1-1-technical-route.png", "图 1-1 系统技术路线图"),
    "图 4-1": ("figures/ch4-1-system-architecture.png", "图 4-1 系统总体架构图"),
    "图 4-2": ("figures/ch4-2-main-process.png", "图 4-2 推荐主流程概要图"),
    "图 4-3": ("figures/ch4-3-er-diagram.png", "图 4-3 数据库 ER 图"),
    "图 5-1": ("figures/ch5-1-recommendation-pipeline.png", "图 5-1 推荐主链路流程图"),
    "图 5-2": ("figures/ch5-2-core-class-diagram.png", "图 5-2 系统核心类图"),
    "图 5-4": ("figures/ch5-4-profile-sequence.png", "图 5-4 用户画像生成时序图"),
    "图 5-5": ("figures/ch5-5-recall-ranking-sequence.png", "图 5-5 候选召回与相似度排序时序图"),
    "图 5-6": ("figures/ch5-6-rerank-flow.png", "图 5-6 校园规则重排流程图"),
    "图 5-7": ("figures/ch5-7-explanation-evidence-flow.png", "图 5-7 推荐解释证据流图"),
    "图 5-8": ("figures/ch5-8-feedback-update-sequence.png", "图 5-8 反馈更新时序图"),
}

TABLE_IMAGES = {
    ("机制", "作用", "实现要点"): "figures/ch5-table-1-core-mechanisms.png",
    ("公式", "含义", "用途"): "figures/ch5-table-2-formulas.png",
}


def set_run_font(run, size: float | None = None, bold: bool | None = None, latin: bool = False) -> None:
    if latin:
        run.font.name = "Times New Roman"
        run._element.rPr.rFonts.set(qn("w:ascii"), "Times New Roman")
        run._element.rPr.rFonts.set(qn("w:hAnsi"), "Times New Roman")
    else:
        run.font.name = "宋体"
        run._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold


def insert_paragraph_before(marker: Paragraph, style: str | None = None) -> Paragraph:
    new_p = OxmlElement("w:p")
    marker._p.addprevious(new_p)
    paragraph = Paragraph(new_p, marker._parent)
    if style:
        paragraph.style = style
    return paragraph


def insert_table_before(doc: Document, marker: Paragraph, rows: int, cols: int) -> Table:
    table = doc.add_table(rows=rows, cols=cols)
    marker._p.addprevious(table._tbl)
    return table


def clear_between(start: Paragraph, end: Paragraph) -> None:
    cursor = start._p.getnext()
    while cursor is not None and cursor is not end._p:
        next_cursor = cursor.getnext()
        cursor.getparent().remove(cursor)
        cursor = next_cursor


def clear_after(start: Paragraph, doc: Document) -> None:
    sect_pr = doc._body._element.sectPr
    cursor = start._p.getnext()
    while cursor is not None and cursor is not sect_pr:
        next_cursor = cursor.getnext()
        cursor.getparent().remove(cursor)
        cursor = next_cursor


def set_paragraph_format(paragraph, first_line: bool = True) -> None:
    fmt = paragraph.paragraph_format
    fmt.line_spacing = Pt(20)
    fmt.space_before = Pt(0)
    fmt.space_after = Pt(0)
    if first_line:
        fmt.first_line_indent = Pt(21)


def clear_document_from(doc: Document, first_body_index: int) -> None:
    body = doc._body._element
    children = list(body)
    sect_pr = body.sectPr
    for child in children[first_body_index:]:
        if child is not sect_pr:
            body.remove(child)


def add_page_break(doc: Document, before: Paragraph | None = None) -> None:
    p = insert_paragraph_before(before) if before else doc.add_paragraph()
    p.add_run().add_break(WD_BREAK.PAGE)


def normalize_heading(text: str) -> tuple[int, str] | None:
    if text.startswith("# "):
        title = text[2:].strip()
        m = re.match(r"第\s*(\d+)\s*章\s*(.+)", title)
        if m:
            return 1, m.group(2).strip()
        return 0, title
    if text.startswith("## "):
        return 2, re.sub(r"^\d+(\.\d+)*\s*", "", text[3:].strip())
    if text.startswith("### "):
        return 3, re.sub(r"^\d+(\.\d+)*\s*", "", text[4:].strip())
    return None


def add_heading(doc: Document, level: int, text: str, before: Paragraph | None = None) -> None:
    if level == 0:
        p = insert_paragraph_before(before, "标题名（不入目录）") if before else doc.add_paragraph(style="标题名（不入目录）")
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    elif level == 1:
        p = insert_paragraph_before(before, "1级标题") if before else doc.add_paragraph(style="1级标题")
    elif level == 2:
        p = insert_paragraph_before(before, "2级标题") if before else doc.add_paragraph(style="2级标题")
    else:
        p = insert_paragraph_before(before, "3级标题") if before else doc.add_paragraph(style="3级标题")
    p.text = ""
    run = p.add_run(text)
    if level == 0:
        set_run_font(run, 16, True)


def is_table_start(lines: list[str], idx: int) -> bool:
    if idx + 1 >= len(lines):
        return False
    return lines[idx].strip().startswith("|") and re.match(r"^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$", lines[idx + 1])


def split_table_row(line: str) -> list[str]:
    line = line.strip()
    if line.startswith("|"):
        line = line[1:]
    if line.endswith("|"):
        line = line[:-1]
    return [cell.strip().replace("<br/>", "\n") for cell in line.split("|")]


def display_width(text: str) -> int:
    width = 0
    for char in text:
        width += 1 if ord(char) < 128 else 2
    return width


def table_width_twips(doc: Document) -> int:
    section = doc.sections[-1]
    width = section.page_width.twips - section.left_margin.twips - section.right_margin.twips
    return width if width > 0 else 8500


def column_widths(headers: list[str], rows: list[list[str]], total_width: int) -> list[int]:
    columns = len(headers)
    values = [headers] + rows
    weights: list[int] = []
    for col in range(columns):
        max_width = max((display_width(row[col]) for row in values if col < len(row)), default=8)
        weights.append(max(6, min(max_width, 36)))

    min_width = 1500 if columns <= 3 else 1050 if columns == 4 else 760
    min_width = min(min_width, max(1, total_width // columns))
    remaining = max(0, total_width - min_width * columns)
    weight_sum = sum(weights) or columns
    widths = [min_width + int(remaining * weight / weight_sum) for weight in weights]
    widths[-1] += total_width - sum(widths)
    return widths


def set_table_grid(table, widths: list[int], total_width: int) -> None:
    table.autofit = False
    tbl = table._tbl
    tbl_pr = tbl.tblPr
    tbl_w = tbl_pr.find(qn("w:tblW"))
    if tbl_w is None:
        tbl_w = OxmlElement("w:tblW")
        tbl_pr.append(tbl_w)
    tbl_w.set(qn("w:type"), "dxa")
    tbl_w.set(qn("w:w"), str(total_width))

    layout = tbl_pr.find(qn("w:tblLayout"))
    if layout is None:
        layout = OxmlElement("w:tblLayout")
        tbl_pr.append(layout)
    layout.set(qn("w:type"), "fixed")

    old_grid = tbl.find(qn("w:tblGrid"))
    if old_grid is not None:
        tbl.remove(old_grid)
    grid = OxmlElement("w:tblGrid")
    for width in widths:
        grid_col = OxmlElement("w:gridCol")
        grid_col.set(qn("w:w"), str(width))
        grid.append(grid_col)
    tbl.insert(1, grid)


def set_cell_width(cell, width: int) -> None:
    cell.width = Twips(width)
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_w = tc_pr.find(qn("w:tcW"))
    if tc_w is None:
        tc_w = OxmlElement("w:tcW")
        tc_pr.append(tc_w)
    tc_w.set(qn("w:type"), "dxa")
    tc_w.set(qn("w:w"), str(width))


def style_table(table, doc: Document, widths: list[int]) -> None:
    try:
        table.style = "Table Grid"
    except Exception:
        pass
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_grid(table, widths, table_width_twips(doc))
    for row_idx, row in enumerate(table.rows):
        for col_idx, cell in enumerate(row.cells):
            set_cell_width(cell, widths[col_idx])
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            for p in cell.paragraphs:
                set_paragraph_format(p, first_line=False)
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER if row_idx == 0 or len(row.cells) <= 3 else WD_ALIGN_PARAGRAPH.LEFT
                for run in p.runs:
                    set_run_font(run, 10.5, bold=(row_idx == 0))


def add_table_row_text(doc: Document, text: str, bold_label: bool = False) -> None:
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    fmt = p.paragraph_format
    fmt.line_spacing = Pt(20)
    fmt.space_before = Pt(0)
    fmt.space_after = Pt(0)
    fmt.left_indent = Pt(21)
    p.text = ""
    run = p.add_run(text)
    set_run_font(run, 10.5, bold_label)


def add_table_placeholder_line(doc: Document, text: str, before: Paragraph | None = None, bold: bool = False) -> None:
    p = insert_paragraph_before(before) if before else doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    fmt = p.paragraph_format
    fmt.line_spacing = Pt(18)
    fmt.space_before = Pt(0)
    fmt.space_after = Pt(0)
    fmt.left_indent = Pt(21)
    p.text = ""
    run = p.add_run(text)
    set_run_font(run, 10, bold)


def add_markdown_table(doc: Document, lines: list[str], idx: int, before: Paragraph | None = None) -> int:
    start_idx = idx
    headers = split_table_row(lines[idx])
    rows: list[list[str]] = []
    idx += 2
    while idx < len(lines) and lines[idx].strip().startswith("|"):
        rows.append(split_table_row(lines[idx]))
        idx += 1

    if add_table_image(doc, [normalize_inline_text(h) for h in headers], before=before):
        return idx

    table_lines = [line.rstrip() for line in lines[start_idx:idx]]
    table_key = hashlib.md5("\n".join(table_lines).encode("utf-8")).hexdigest()[:12]
    if add_generated_table_image(doc, table_key, before=before):
        return idx

    # artifact-tool currently renders native Word tables from the school/sample
    # templates as vertical text. Keep the content as a visible Word placeholder;
    # the final manual table should follow the Liu engineering sample table style.
    add_placeholder(
        doc,
        "表格占位：请按 docs/更多参考/本科论文-工程型样例-刘.docx 中表 2-1 或表 3-2 的样式重排；下方保留字段底稿",
        before=before,
    )
    add_table_placeholder_line(doc, "列：" + "；".join(normalize_inline_text(h) for h in headers), before=before, bold=True)
    for row_no, row in enumerate(rows[:6], start=1):
        cells = [normalize_inline_text(cell).replace("\n", " / ") for cell in row[: len(headers)]]
        add_table_placeholder_line(doc, f"{row_no}. " + "；".join(cells), before=before)
    if len(rows) > 6:
        add_table_placeholder_line(doc, f"……其余 {len(rows) - 6} 行请在 Word 表格中补齐。", before=before)
    return idx


def add_placeholder(doc: Document, text: str, before: Paragraph | None = None) -> None:
    p = insert_paragraph_before(before) if before else doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    fmt = p.paragraph_format
    fmt.line_spacing = Pt(20)
    fmt.space_before = Pt(6)
    fmt.space_after = Pt(6)
    p.text = ""
    run = p.add_run(f"【{text}】")
    set_run_font(run, 10.5, True)


def add_figure(doc: Document, figure_ref: str, before: Paragraph | None = None) -> bool:
    image_info = FIGURE_IMAGES.get(figure_ref)
    if not image_info:
        return False
    rel_path, caption = image_info
    image_path = ROOT / "docs" / "generated" / rel_path
    if not image_path.exists():
        return False

    p = insert_paragraph_before(before) if before else doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(3)
    run = p.add_run()
    run.add_picture(str(image_path), width=Inches(6.2))

    add_paragraph_text(doc, caption, before=before)
    return True


def add_table_image(doc: Document, headers: list[str], before: Paragraph | None = None) -> bool:
    rel_path = TABLE_IMAGES.get(tuple(headers))
    if not rel_path:
        return False
    image_path = ROOT / "docs" / "generated" / rel_path
    if not image_path.exists():
        return False
    p = insert_paragraph_before(before) if before else doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(6)
    run = p.add_run()
    run.add_picture(str(image_path), width=Inches(5.8))
    return True


def add_generated_table_image(doc: Document, table_key: str, before: Paragraph | None = None) -> bool:
    image_path = ROOT / "docs" / "generated" / "figures" / f"table-{table_key}.png"
    if not image_path.exists():
        return False
    p = insert_paragraph_before(before) if before else doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(6)
    run = p.add_run()
    run.add_picture(str(image_path), width=Inches(6.1))
    return True


def add_paragraph_text(doc: Document, text: str, style: str | None = None, before: Paragraph | None = None) -> None:
    text = normalize_inline_text(text)
    text = re.sub(r"^(\d+)\.\s+", lambda m: f"（{m.group(1)}）", text)
    if text.startswith("表 5-2"):
        break_p = insert_paragraph_before(before) if before else doc.add_paragraph()
        break_p.add_run().add_break(WD_BREAK.PAGE)
    p = insert_paragraph_before(before, style) if before else (doc.add_paragraph(style=style) if style else doc.add_paragraph())
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER if text.startswith(("表 ", "图 ")) else WD_ALIGN_PARAGRAPH.LEFT
    set_paragraph_format(p, first_line=not text.startswith(("关键词：", "Keywords:", "[", "表 ")))
    if text.startswith("表 "):
        p.paragraph_format.keep_with_next = True
    p.text = ""
    run = p.add_run(text)
    latin = bool(text) and sum(1 for c in text if ord(c) < 128) / max(len(text), 1) > 0.8
    set_run_font(run, 10.5, latin=latin)


def add_static_toc(doc: Document, before: Paragraph | None = None) -> None:
    add_heading(doc, 0, "目    录", before=before)
    for title, page in TOC_ENTRIES:
        p = insert_paragraph_before(before, "toc 1") if before else doc.add_paragraph(style="toc 1")
        p.text = ""
        run = p.add_run(f"{title}\t{page}")
        set_run_font(run, 10.5)


def normalize_inline_text(text: str) -> str:
    return text.replace("`", "")


def figure_reference(text: str) -> str | None:
    match = re.search(r"图\s*(\d+)\s*[-－]\s*(\d+)", text)
    if not match:
        return None
    return f"图 {match.group(1)}-{match.group(2)}"


def render_markdown(doc: Document, path: Path, doc_kind: str, before: Paragraph | None = None) -> None:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    in_code = False
    code_buffer: list[str] = []
    i = 0
    while i < len(lines):
        raw = lines[i]
        line = raw.rstrip()
        if line.startswith("```"):
            if in_code:
                add_placeholder(doc, "图表占位：请在 Word 中按模板绘制流程图或示意图", before=before)
                code_buffer = []
                in_code = False
            else:
                in_code = True
            i += 1
            continue
        if in_code:
            code_buffer.append(line)
            i += 1
            continue
        if not line.strip():
            i += 1
            continue
        heading = normalize_heading(line)
        if heading:
            level, title = heading
            if doc_kind in {"abstract_cn", "abstract_en", "references"} and level == 0:
                add_heading(doc, 0, title, before=before)
            elif doc_kind == "chapter":
                add_heading(doc, level, title, before=before)
            elif doc_kind == "references":
                if title != "使用说明":
                    add_heading(doc, 0, title, before=before)
            i += 1
            continue
        if is_table_start(lines, i):
            i = add_markdown_table(doc, lines, i, before=before)
            continue
        if line.startswith("> 图") or line.startswith("【图"):
            figure_text = line.lstrip("> ").strip("【】")
            ref = figure_reference(figure_text)
            if not ref or not add_figure(doc, ref, before=before):
                add_placeholder(doc, figure_text, before=before)
            i += 1
            continue
        if line.startswith("- "):
            add_paragraph_text(doc, line[2:], before=before)
            i += 1
            continue
        add_paragraph_text(doc, line, before=before)
        i += 1


def update_cover(doc: Document) -> None:
    title_cn = "基于 Spring Boot 的校园社交匹配与可解释推荐系统的设计与实现"
    title_en = "Design and Implementation of Spring Boot-Based Campus Social Matching and Explainable Recommendation System"
    if len(doc.paragraphs) > 7:
        doc.paragraphs[7].text = title_cn
        doc.paragraphs[7].alignment = WD_ALIGN_PARAGRAPH.CENTER
        for run in doc.paragraphs[7].runs:
            set_run_font(run, 16, True)
    if len(doc.paragraphs) > 9:
        doc.paragraphs[9].text = title_en
        doc.paragraphs[9].alignment = WD_ALIGN_PARAGRAPH.CENTER
        for run in doc.paragraphs[9].runs:
            run.font.name = "Times New Roman"
            run.font.size = Pt(12)
    if len(doc.paragraphs) > 84:
        doc.paragraphs[84].text = (
            "本人声明所呈交的毕业论文（设计），题目 基于 Spring Boot 的校园社交匹配与可解释推荐系统的设计与实现 "
            "是本人在指导教师的指导下，独立进行研究工作所取得的成果。尽我所知，除了文中特别加以标注和致谢中所罗列的内容以外，"
            "论文中不包含其他人已经发表或撰写过的研究成果，也不包含为获得北京交通大学或其他教育机构的学位或证书而使用过的材料。"
        )


def set_section_pg_num(section, fmt: str | None = None, start: int | None = None) -> None:
    pg_num = section._sectPr.find(qn("w:pgNumType"))
    if pg_num is None:
        pg_num = OxmlElement("w:pgNumType")
        section._sectPr.append(pg_num)
    if fmt:
        pg_num.set(qn("w:fmt"), fmt)
    elif qn("w:fmt") in pg_num.attrib:
        del pg_num.attrib[qn("w:fmt")]
    if start is not None:
        pg_num.set(qn("w:start"), str(start))
    elif qn("w:start") in pg_num.attrib:
        del pg_num.attrib[qn("w:start")]


def clear_paragraph_content(paragraph: Paragraph) -> None:
    for child in list(paragraph._p):
        paragraph._p.remove(child)


def set_footer_static(section, text: str = "") -> None:
    section.footer.is_linked_to_previous = False
    paragraph = section.footer.paragraphs[0] if section.footer.paragraphs else section.footer.add_paragraph()
    clear_paragraph_content(paragraph)
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    if text:
        run = paragraph.add_run(text)
        set_run_font(run, 10.5, latin=bool(re.fullmatch(r"[IVXLCDM]+", text)))


def set_footer_page_field(section) -> None:
    section.footer.is_linked_to_previous = False
    paragraph = section.footer.paragraphs[0] if section.footer.paragraphs else section.footer.add_paragraph()
    clear_paragraph_content(paragraph)
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr_run = OxmlElement("w:r")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = "PAGE"
    instr_run.append(instr)
    separate = OxmlElement("w:fldChar")
    separate.set(qn("w:fldCharType"), "separate")
    result_run = OxmlElement("w:r")
    result = OxmlElement("w:t")
    result.text = "1"
    result_run.append(result)
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.append(begin)
    paragraph._p.append(instr_run)
    run2 = paragraph.add_run()
    run2._r.append(separate)
    paragraph._p.append(result_run)
    run3 = paragraph.add_run()
    run3._r.append(end)


def normalize_section_page_numbers(doc: Document) -> None:
    # Template section order after clearing: cover/statement sections, Chinese
    # abstract, English abstract, TOC, body, references, acknowledgements.
    for idx, section in enumerate(doc.sections):
        if idx <= 2:
            set_footer_static(section)
    if len(doc.sections) >= 6:
        set_footer_static(doc.sections[3], "I")
        set_footer_static(doc.sections[4], "II")
        set_footer_static(doc.sections[5], "III")
        set_section_pg_num(doc.sections[3], fmt="lowerRoman", start=1)
        set_section_pg_num(doc.sections[4], fmt="lowerRoman")
        set_section_pg_num(doc.sections[5], fmt="lowerRoman")
    if len(doc.sections) >= 7:
        set_section_pg_num(doc.sections[6], start=1)
        set_footer_page_field(doc.sections[6])
    if len(doc.sections) >= 8:
        set_section_pg_num(doc.sections[7])
        set_footer_page_field(doc.sections[7])
    if len(doc.sections) >= 9:
        set_section_pg_num(doc.sections[8])
        set_footer_page_field(doc.sections[8])


def build() -> None:
    doc = Document(TEMPLATE)
    update_cover(doc)

    cn_start = doc.paragraphs[88]
    en_start = doc.paragraphs[111]
    toc_start = doc.paragraphs[134]
    body_start = doc.paragraphs[163]
    ref_start = doc.paragraphs[232]
    ack_start = doc.paragraphs[258]

    clear_between(cn_start, en_start)
    render_markdown(doc, SOURCES[0][1], "abstract_cn", before=en_start)

    clear_between(en_start, toc_start)
    render_markdown(doc, SOURCES[1][1], "abstract_en", before=toc_start)

    clear_between(toc_start, body_start)
    add_static_toc(doc, before=body_start)

    clear_between(body_start, ref_start)
    for idx, (_, source) in enumerate(SOURCES[2:9]):
        if idx > 0:
            add_page_break(doc, before=ref_start)
        render_markdown(doc, source, "chapter", before=ref_start)

    clear_between(ref_start, ack_start)
    render_markdown(doc, SOURCES[9][1], "references", before=ack_start)

    clear_after(ack_start, doc)
    add_heading(doc, 0, "致    谢")
    add_paragraph_text(
        doc,
        "本论文从选题、需求梳理、系统设计、编码实现到论文撰写，得到了指导教师在研究方向、技术路线和论文规范方面的持续指导。老师对系统边界、工程实现和论文表达提出了许多具体意见，使本文能够围绕工程型毕业设计的要求逐步完善，在此表示诚挚的感谢。",
    )
    add_paragraph_text(
        doc,
        "感谢学院各位任课教师在本科阶段的教学与培养，使本人能够掌握软件工程、数据库、后端开发、测试验证等方面的基础知识，并将其综合应用到本课题的设计与实现过程中。感谢同学和朋友在资料整理、系统试用和论文检查过程中给予的帮助。",
    )
    add_paragraph_text(
        doc,
        "感谢家人在学习和论文完成期间给予的理解与支持。由于本人能力和时间有限，论文与系统仍有不足之处，恳请各位老师批评指正。",
    )

    normalize_section_page_numbers(doc)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    build()
