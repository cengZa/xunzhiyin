$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$ScriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Join-Path (Get-Location) "scripts" }
$OutDir = Join-Path $ScriptRoot "..\docs\generated\figures"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function New-Canvas($width, $height) {
    $width = [int][Math]::Max(1, [Math]::Ceiling([double]$width))
    $height = [int][Math]::Max(1, [Math]::Ceiling([double]$height))
    $bmp = New-Object System.Drawing.Bitmap($width, $height)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $g.Clear([System.Drawing.Color]::White)
    return @($bmp, $g)
}

function Font($size, $style = "Regular") {
    return New-Object System.Drawing.Font("Microsoft YaHei", $size, [System.Drawing.FontStyle]::$style)
}

function Brush($html) {
    return New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml($html))
}

function Pen($html, $width = 2) {
    return New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml($html), $width)
}

function Draw-Title($g, $text, $width) {
    $font = Font 24 Bold
    $brush = Brush "#1f2937"
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, $font, $brush, [System.Drawing.RectangleF]::new(0, 24, $width, 40), $sf)
}

function Draw-Box($g, $x, $y, $w, $h, $text, $fill = "#eef6ff", $stroke = "#2f5f9f", $fontSize = 14) {
    $rect = [System.Drawing.Rectangle]::new($x, $y, $w, $h)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $r = 14
    $path.AddArc($x, $y, $r, $r, 180, 90)
    $path.AddArc($x + $w - $r, $y, $r, $r, 270, 90)
    $path.AddArc($x + $w - $r, $y + $h - $r, $r, $r, 0, 90)
    $path.AddArc($x, $y + $h - $r, $r, $r, 90, 90)
    $path.CloseFigure()
    $g.FillPath((Brush $fill), $path)
    $g.DrawPath((Pen $stroke 2), $path)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, (Font $fontSize Bold), (Brush "#111827"), [System.Drawing.RectangleF]::new($x + 6, $y + 4, $w - 12, $h - 8), $sf)
}

function Draw-Arrow($g, $x1, $y1, $x2, $y2, $color = "#374151") {
    $p = Pen $color 2
    $p.CustomEndCap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(5, 6)
    $g.DrawLine($p, $x1, $y1, $x2, $y2)
}

function Draw-Lane($g, $x, $top, $bottom, $label) {
    $g.DrawLine((Pen "#9ca3af" 2), $x, $top, $x, $bottom)
    Draw-Box $g ($x - 78) ($top - 54) 156 42 $label "#f8fafc" "#64748b" 11
}

function Draw-Message($g, $x1, $x2, $y, $text) {
    Draw-Arrow $g $x1 $y $x2 $y "#374151"
    $left = [Math]::Min($x1, $x2)
    $w = [Math]::Abs($x2 - $x1)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, (Font 11 Regular), (Brush "#111827"), [System.Drawing.RectangleF]::new($left, $y - 24, $w, 22), $sf)
}

function Draw-CellText($g, $text, $x, $y, $w, $h, $fontSize = 13, $bold = $false, $align = "Center") {
    $style = if ($bold) { "Bold" } else { "Regular" }
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = if ($align -eq "Left") { [System.Drawing.StringAlignment]::Near } else { [System.Drawing.StringAlignment]::Center }
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rect = [System.Drawing.RectangleF]::new($x + 8, $y + 4, $w - 16, $h - 8)
    $g.DrawString($text, (Font $fontSize $style), (Brush "#111827"), $rect, $sf)
}

function Get-TableKey($lines) {
    $joined = [string]::Join("`n", $lines)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($joined)
    $md5 = [System.Security.Cryptography.MD5]::Create()
    $hash = $md5.ComputeHash($bytes)
    (($hash | ForEach-Object { $_.ToString("x2") }) -join "").Substring(0, 12)
}

function Split-MarkdownRow($line) {
    $trimmed = $line.Trim()
    if ($trimmed.StartsWith("|")) { $trimmed = $trimmed.Substring(1) }
    if ($trimmed.EndsWith("|")) { $trimmed = $trimmed.Substring(0, $trimmed.Length - 1) }
    @($trimmed -split "\|" | ForEach-Object { ($_.Trim() -replace '`', "") })
}

function Estimate-RowHeight($cells, $colWidths, $base = 44) {
    $lines = 1
    for ($i = 0; $i -lt $cells.Count; $i++) {
        $charsPerLine = [Math]::Max(5, [Math]::Floor($colWidths[$i] / 20))
        $need = [Math]::Ceiling(([double]$cells[$i].Length) / $charsPerLine)
        if ($need -gt $lines) { $lines = $need }
    }
    [int]([Math]::Max($base, 28 + $lines * 24))
}

function Get-ColumnWidths($columnCount) {
    switch ($columnCount) {
        2 { return @(300, 980) }
        3 { return @(260, 500, 520) }
        4 { return @(150, 260, 460, 460) }
        5 { return @(240, 220, 220, 220, 220) }
        6 { return @(260, 210, 180, 170, 170, 210) }
        default {
            $width = [Math]::Floor(1320 / $columnCount)
            return @(1..$columnCount | ForEach-Object { $width })
        }
    }
}

function Draw-MarkdownTableImage($tableLines) {
    $headers = Split-MarkdownRow $tableLines[0]
    $rows = @()
    for ($i = 2; $i -lt $tableLines.Count; $i++) {
        $rows += ,(Split-MarkdownRow $tableLines[$i])
    }
    $colWidths = Get-ColumnWidths $headers.Count
    $rowHeights = @(50)
    foreach ($row in $rows) {
        $rowHeights += Estimate-RowHeight $row $colWidths
    }
    $tableWidth = ($colWidths | Measure-Object -Sum).Sum
    $tableHeight = ($rowHeights | Measure-Object -Sum).Sum
    $c = New-Canvas ($tableWidth + 140) ($tableHeight + 80); $bmp = $c[0]; $g = $c[1]
    Draw-Table $g 70 40 $colWidths $rowHeights $headers $rows
    $key = Get-TableKey $tableLines
    Save-Figure $bmp $g "table-$key.png"
}

function Generate-MarkdownTables {
    $paths = @(
        "docs\05_thesis\chapter2_related_work.md",
        "docs\05_thesis\chapter3_analysis.md",
        "docs\05_thesis\chapter4_design.md",
        "docs\05_thesis\chapter5_implementation.md",
        "docs\05_thesis\chapter6_test.md"
    )
    foreach ($rel in $paths) {
        $path = Join-Path (Join-Path $ScriptRoot "..") $rel
        $lines = [System.IO.File]::ReadAllLines($path, [System.Text.Encoding]::UTF8)
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i].Trim().StartsWith("|") -and ($i + 1) -lt $lines.Count -and $lines[$i + 1] -match "^\s*\|?\s*:?-{3,}") {
                $tableLines = New-Object System.Collections.Generic.List[string]
                while ($i -lt $lines.Count -and $lines[$i].Trim().StartsWith("|")) {
                    $tableLines.Add($lines[$i].TrimEnd())
                    $i++
                }
                Draw-MarkdownTableImage $tableLines
            }
        }
    }
}

function Figure-11 {
    $c = New-Canvas 1900 760; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "系统技术路线图" 1900
    $items = @(
        @("校园用户`n标签数据", 70, 145), @("改进 TF-IDF`n用户画像", 280, 145), @("Top-K`n核心标签", 500, 145),
        @("标签倒排索引`n候选召回", 700, 145), @("余弦相似度`n基础排序", 940, 145), @("校园规则重排`n可信连接分", 1160, 145),
        @("推荐解释生成`n规则回退", 1400, 145), @("用户反馈`n画像更新", 1640, 145)
    )
    foreach ($i in $items) { Draw-Box $g $i[1] $i[2] 170 82 $i[0] }
    for ($i = 0; $i -lt $items.Count - 1; $i++) { Draw-Arrow $g ($items[$i][1] + 170) 186 $items[$i + 1][1] 186 }
    Draw-Box $g 585 405 230 82 "Spring Boot`nMyBatis Plus" "#f0fdf4" "#15803d"
    Draw-Box $g 885 405 230 82 "MySQL`n结构化数据" "#fefce8" "#a16207"
    Draw-Box $g 1185 405 230 82 "Redis`n索引与缓存" "#fff7ed" "#c2410c"
    Draw-Arrow $g 700 405 700 227 "#6b7280"
    Draw-Arrow $g 1000 405 1000 227 "#6b7280"
    Draw-Arrow $g 1300 405 1300 227 "#6b7280"
    Save-Figure $bmp $g "ch1-1-technical-route.png"
}

function Figure-41 {
    $c = New-Canvas 1550 980; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "系统总体架构图" 1550
    $layers = @(
        @("接口层`nController / REST API", 220, 120, "#eef6ff", "#1d4ed8"),
        @("应用服务层`n推荐编排与业务服务", 220, 250, "#f0fdf4", "#15803d"),
        @("领域能力层`n画像 / 召回 / 排序 / 重排 / 解释", 220, 380, "#fefce8", "#a16207"),
        @("数据访问层`nMapper / Repository", 220, 510, "#fff7ed", "#c2410c")
    )
    foreach ($l in $layers) { Draw-Box $g $l[1] $l[2] 720 82 $l[0] $l[3] $l[4] 15 }
    for ($i=0; $i -lt 3; $i++) { Draw-Arrow $g 580 ($layers[$i][2] + 82) 580 $layers[$i+1][2] }
    Draw-Box $g 1060 250 220 82 "MySQL`n主数据与结果" "#fefce8" "#a16207"
    Draw-Box $g 1060 420 220 82 "Redis`n倒排索引与缓存" "#fff7ed" "#c2410c"
    Draw-Arrow $g 940 551 1060 291
    Draw-Arrow $g 940 551 1060 461
    Save-Figure $bmp $g "ch4-1-system-architecture.png"
}

function Figure-42 {
    $c = New-Canvas 1900 700; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "推荐主流程概要图" 1900
    $items = @(
        @("用户标签", 80, 150), @("画像构建", 270, 150), @("Top-K 标签", 460, 150), @("倒排召回", 650, 150),
        @("基础排序", 840, 150), @("场景重排", 1030, 150), @("解释生成", 1220, 150), @("Top-K 结果", 1410, 150), @("反馈更新", 1600, 150)
    )
    foreach ($i in $items) { Draw-Box $g $i[1] $i[2] 150 72 $i[0] }
    for ($i=0; $i -lt $items.Count-1; $i++) { Draw-Arrow $g ($items[$i][1] + 150) 186 $items[$i+1][1] 186 }
    Draw-Arrow $g 1675 222 360 350 "#6b7280"
    Draw-Box $g 270 350 240 70 "反馈影响下一轮画像" "#ecfdf5" "#15803d" 13
    Save-Figure $bmp $g "ch4-2-main-process.png"
}

function Figure-43 {
    $c = New-Canvas 1750 900; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "数据库 ER 图" 1750
    Draw-Box $g 90 140 210 70 "user" "#eef6ff" "#1d4ed8"
    Draw-Box $g 90 360 250 70 "user_tag_relation" "#f0fdf4" "#15803d"
    Draw-Box $g 430 360 210 70 "tag" "#eef6ff" "#1d4ed8"
    Draw-Box $g 430 140 220 70 "user_profile" "#fefce8" "#a16207"
    Draw-Box $g 760 250 260 70 "recommendation_result" "#fff7ed" "#c2410c"
    Draw-Box $g 1120 150 290 70 "recommendation_explanation" "#fefce8" "#a16207"
    Draw-Box $g 1120 360 220 70 "user_feedback" "#f0fdf4" "#15803d"
    Draw-Arrow $g 195 210 195 360
    Draw-Arrow $g 340 395 430 395
    Draw-Arrow $g 300 175 430 175
    Draw-Arrow $g 650 175 760 285
    Draw-Arrow $g 1020 285 1120 185
    Draw-Arrow $g 1020 285 1120 395
    Draw-Arrow $g 300 175 760 285 "#6b7280"
    Save-Figure $bmp $g "ch4-3-er-diagram.png"
}


function Draw-Table($g, $x, $y, $colWidths, $rowHeights, $headers, $rows) {
    $tableWidth = ($colWidths | Measure-Object -Sum).Sum
    $tableHeight = ($rowHeights | Measure-Object -Sum).Sum
    $black = Pen "#111111" 2
    $thin = Pen "#111111" 1
    $headerFill = Brush "#e8f0fb"
    $g.FillRectangle($headerFill, $x, $y, $tableWidth, $rowHeights[0])
    $g.DrawLine($black, $x, $y, $x + $tableWidth, $y)
    $g.DrawLine($black, $x, $y + $rowHeights[0], $x + $tableWidth, $y + $rowHeights[0])
    $g.DrawLine($black, $x, $y + $tableHeight, $x + $tableWidth, $y + $tableHeight)
    $cx = $x
    for ($c = 0; $c -lt $colWidths.Count; $c++) {
        $g.DrawLine($thin, $cx, $y, $cx, $y + $tableHeight)
        Draw-CellText $g $headers[$c] $cx $y $colWidths[$c] $rowHeights[0] 13 $true
        $cx += $colWidths[$c]
    }
    $g.DrawLine($thin, $x + $tableWidth, $y, $x + $tableWidth, $y + $tableHeight)
    $cy = $y + $rowHeights[0]
    for ($r = 0; $r -lt $rows.Count; $r++) {
        if ($r -gt 0) { $g.DrawLine($thin, $x, $cy, $x + $tableWidth, $cy) }
        $cx = $x
        for ($c = 0; $c -lt $colWidths.Count; $c++) {
            $align = if ($c -eq 0) { "Center" } else { "Left" }
            Draw-CellText $g $rows[$r][$c] $cx $cy $colWidths[$c] $rowHeights[$r + 1] 12 $false $align
            $cx += $colWidths[$c]
        }
        $cy += $rowHeights[$r + 1]
    }
}

function Save-Figure($bmp, $g, $name) {
    $g.Dispose()
    $path = Join-Path $OutDir $name
    $minX = $bmp.Width
    $minY = $bmp.Height
    $maxX = 0
    $maxY = 0
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $c = $bmp.GetPixel($x, $y)
            if ($c.R -lt 248 -or $c.G -lt 248 -or $c.B -lt 248) {
                if ($x -lt $minX) { $minX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    $pad = 24
    $minX = [Math]::Max(0, $minX - $pad)
    $minY = [Math]::Max(0, $minY - $pad)
    $maxX = [Math]::Min($bmp.Width - 1, $maxX + $pad)
    $maxY = [Math]::Min($bmp.Height - 1, $maxY + $pad)
    $cropRect = [System.Drawing.Rectangle]::new($minX, $minY, $maxX - $minX + 1, $maxY - $minY + 1)
    $cropped = $bmp.Clone($cropRect, $bmp.PixelFormat)
    $cropped.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $cropped.Dispose()
    $bmp.Dispose()
    Write-Output $path
}

function Figure-51 {
    $c = New-Canvas 1900 760; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "推荐主链路流程图" 1900
    $items = @(
        @("用户标签", 80, 130), @("画像构建`n改进 TF-IDF", 280, 130), @("Top-K`n核心标签", 500, 130),
        @("倒排召回`n候选用户", 700, 130), @("相似度排序`n标签贡献", 920, 130), @("校园规则重排`n年级/专业/社团", 1140, 130),
        @("可信连接分`n轻量探索", 1380, 130), @("推荐解释`n证据生成", 1600, 130)
    )
    foreach ($i in $items) { Draw-Box $g $i[1] $i[2] 160 86 $i[0] }
    for ($i = 0; $i -lt $items.Count - 1; $i++) { Draw-Arrow $g ($items[$i][1] + 160) 173 $items[$i + 1][1] 173 }
    Draw-Box $g 760 380 180 86 "Top-K`n推荐结果" "#ecfdf5" "#15803d"
    Draw-Box $g 1030 380 180 86 "用户反馈`n关注/忽略" "#fff7ed" "#c2410c"
    Draw-Box $g 1300 380 180 86 "画像轻量更新" "#fefce8" "#a16207"
    Draw-Arrow $g 1680 216 850 380
    Draw-Arrow $g 940 423 1030 423
    Draw-Arrow $g 1210 423 1300 423
    Draw-Arrow $g 1390 380 360 216 "#6b7280"
    Save-Figure $bmp $g "ch5-1-recommendation-pipeline.png"
}

function Figure-52 {
    $c = New-Canvas 1700 980; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "系统核心类图" 1700
    Draw-Box $g 690 95 300 70 "RecommendationServiceImpl" "#eef6ff" "#1d4ed8" 14
    Draw-Box $g 700 20 280 48 "RecommendationController" "#f8fafc" "#64748b" 12
    Draw-Arrow $g 840 68 840 95
    $left = @(
        @("ProfileService", 130, 230), @("RecallService", 130, 350), @("RankingService", 130, 470), @("RerankService", 130, 590)
    )
    $right = @(
        @("TrustScoreService", 1230, 230), @("ExplorationService", 1230, 350), @("ExplanationService", 1230, 470), @("FeedbackService", 1230, 590)
    )
    foreach ($b in $left) { Draw-Box $g $b[1] $b[2] 260 64 $b[0] "#f0fdf4" "#15803d" 12; Draw-Arrow $g 690 130 ($b[1] + 260) ($b[2] + 32) }
    foreach ($b in $right) { Draw-Box $g $b[1] $b[2] 260 64 $b[0] "#fff7ed" "#c2410c" 12; Draw-Arrow $g 990 130 $b[1] ($b[2] + 32) }
    $bottom = @(
        @("ProfileCacheRepository", 180, 790), @("RecallIndexRepository", 500, 790), @("RecommendationResultMapper", 820, 790), @("RecommendationExplanationMapper", 1140, 790)
    )
    foreach ($b in $bottom) { Draw-Box $g $b[1] $b[2] 260 70 $b[0] "#fefce8" "#a16207" 11 }
    Draw-Arrow $g 260 294 310 790; Draw-Arrow $g 260 414 630 790; Draw-Arrow $g 840 165 950 790; Draw-Arrow $g 840 165 1270 790
    Save-Figure $bmp $g "ch5-2-core-class-diagram.png"
}

function Figure-54 {
    $c = New-Canvas 1700 980; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "用户画像生成时序图" 1700
    $lanes = @(180, 430, 680, 930, 1180, 1430)
    $labels = @("用户", "ProfileController", "ProfileService", "标签关系Mapper", "权重计算器", "画像存储/缓存")
    for ($i=0; $i -lt $lanes.Count; $i++) { Draw-Lane $g $lanes[$i] 130 900 $labels[$i] }
    Draw-Message $g 180 430 190 "请求生成/刷新画像"
    Draw-Message $g 430 680 280 "buildProfile(userId)"
    Draw-Message $g 680 930 370 "查询用户标签关系"
    Draw-Message $g 930 680 460 "返回标签、时间、权重种子"
    Draw-Message $g 680 1180 550 "计算 TF-IDF、时间衰减、Top-K"
    Draw-Message $g 1180 680 640 "返回画像权重向量"
    Draw-Message $g 680 1430 730 "保存画像并写入缓存"
    Draw-Message $g 680 430 820 "返回画像结果"
    Save-Figure $bmp $g "ch5-4-profile-sequence.png"
}

function Figure-55 {
    $c = New-Canvas 1700 930; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "候选召回与相似度排序时序图" 1700
    $lanes = @(220, 500, 780, 1060, 1340)
    $labels = @("推荐编排服务", "画像服务", "召回服务", "倒排索引", "排序服务")
    for ($i=0; $i -lt $lanes.Count; $i++) { Draw-Lane $g $lanes[$i] 130 850 $labels[$i] }
    Draw-Message $g 220 500 210 "获取请求用户 Top-K 标签"
    Draw-Message $g 500 220 300 "返回画像与 Top-K"
    Draw-Message $g 220 780 390 "发起候选召回"
    Draw-Message $g 780 1060 480 "按标签查询倒排索引"
    Draw-Message $g 1060 780 570 "返回候选用户集合"
    Draw-Message $g 780 220 660 "返回去重候选用户"
    Draw-Message $g 220 1340 750 "计算余弦相似度和标签贡献"
    Save-Figure $bmp $g "ch5-5-recall-ranking-sequence.png"
}

function Figure-56 {
    $c = New-Canvas 1600 760; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "校园规则重排流程图" 1600
    Draw-Box $g 90 150 190 82 "基础排序结果" "#eef6ff" "#1d4ed8"
    Draw-Box $g 350 150 190 82 "场景模式`n学习/社团/兴趣" "#eef6ff" "#1d4ed8"
    Draw-Box $g 660 90 170 70 "年级差规则" "#f0fdf4" "#15803d"
    Draw-Box $g 660 210 170 70 "专业相关规则" "#f0fdf4" "#15803d"
    Draw-Box $g 660 330 170 70 "社团重合规则" "#f0fdf4" "#15803d"
    Draw-Box $g 950 210 180 82 "校园规则分" "#fefce8" "#a16207"
    Draw-Box $g 1210 210 180 82 "可信连接分" "#fff7ed" "#c2410c"
    Draw-Box $g 1210 390 180 82 "轻量探索位" "#fff7ed" "#c2410c"
    Draw-Box $g 950 520 230 82 "最终 Top-K 推荐结果" "#ecfdf5" "#15803d"
    Draw-Arrow $g 280 191 350 191
    Draw-Arrow $g 540 191 660 125
    Draw-Arrow $g 540 191 660 245
    Draw-Arrow $g 540 191 660 365
    Draw-Arrow $g 830 125 950 245
    Draw-Arrow $g 830 245 950 245
    Draw-Arrow $g 830 365 950 245
    Draw-Arrow $g 1130 251 1210 251
    Draw-Arrow $g 1300 292 1300 390
    Draw-Arrow $g 1210 431 1065 520
    Save-Figure $bmp $g "ch5-6-rerank-flow.png"
}

function Figure-57 {
    $c = New-Canvas 1650 820; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "推荐解释证据流图" 1650
    Draw-Box $g 80 150 210 82 "排序贡献`n共同标签与权重" "#eef6ff" "#1d4ed8" 13
    Draw-Box $g 80 300 210 82 "规则命中`n年级/专业/社团" "#f0fdf4" "#15803d" 13
    Draw-Box $g 80 450 210 82 "可信连接原因`n资料与证据强度" "#fff7ed" "#c2410c" 13
    Draw-Box $g 430 285 220 92 "证据提取`n结构化解释依据" "#fefce8" "#a16207" 13
    Draw-Box $g 780 210 220 82 "模板解释生成" "#eef6ff" "#1d4ed8" 13
    Draw-Box $g 780 380 220 82 "规则回退解释" "#f8fafc" "#64748b" 13
    Draw-Box $g 1120 210 230 82 "可选 LLM 润色`n只优化表达" "#f0fdf4" "#15803d" 13
    Draw-Box $g 1120 380 230 82 "解释持久化`n结果与证据留存" "#ecfdf5" "#15803d" 13
    Draw-Box $g 1390 295 190 82 "返回推荐理由" "#fff7ed" "#c2410c" 13
    Draw-Arrow $g 290 191 430 315
    Draw-Arrow $g 290 341 430 331
    Draw-Arrow $g 290 491 430 355
    Draw-Arrow $g 650 331 780 251
    Draw-Arrow $g 650 331 780 421
    Draw-Arrow $g 1000 251 1120 251
    Draw-Arrow $g 1000 421 1120 421
    Draw-Arrow $g 1235 292 1235 380
    Draw-Arrow $g 1350 421 1390 336
    Draw-Arrow $g 1350 251 1390 315
    Save-Figure $bmp $g "ch5-7-explanation-evidence-flow.png"
}

function Figure-58 {
    $c = New-Canvas 1700 960; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "反馈更新时序图" 1700
    $lanes = @(180, 430, 680, 930, 1180, 1430)
    $labels = @("用户", "FeedbackController", "FeedbackService", "推荐结果存储", "解释证据存储", "画像服务")
    for ($i=0; $i -lt $lanes.Count; $i++) { Draw-Lane $g $lanes[$i] 130 880 $labels[$i] }
    Draw-Message $g 180 430 205 "提交关注/忽略反馈"
    Draw-Message $g 430 680 295 "saveFeedback(request)"
    Draw-Message $g 680 930 385 "查询推荐结果"
    Draw-Message $g 930 680 475 "返回推荐对象与批次"
    Draw-Message $g 680 1180 565 "查询解释证据"
    Draw-Message $g 1180 680 655 "返回主要贡献标签"
    Draw-Message $g 680 1430 745 "按反馈类型轻量调整画像"
    Draw-Message $g 680 430 835 "返回保存结果"
    Save-Figure $bmp $g "ch5-8-feedback-update-sequence.png"
}

function Table-51 {
    $c = New-Canvas 1500 540; $bmp = $c[0]; $g = $c[1]
    $headers = @("机制", "作用", "实现要点")
    $rows = @(
        @("用户画像", "将用户标签转化为可计算的兴趣向量", "使用改进 TF-IDF、时间衰减和 Top-K 裁剪"),
        @("候选召回", "从全量用户中筛选可能匹配的候选对象", "使用标签倒排索引合并候选用户集合"),
        @("相似度排序", "计算目标用户与候选用户的兴趣接近程度", "使用画像向量余弦相似度并记录标签贡献"),
        @("校园规则重排", "使推荐结果贴合学习、社团和兴趣场景", "根据年级、专业、社团和场景模式调整分数"),
        @("可信连接分", "抑制资料不足或证据较弱的推荐对象", "根据资料完整度和匹配证据生成可信原因"),
        @("推荐解释", "说明推荐结果产生的依据", "从标签贡献、规则命中和可信连接原因中提取证据"),
        @("反馈更新", "让用户行为影响后续画像", "根据关注或忽略反馈对相关标签权重轻量调整")
    )
    Draw-Table $g 95 30 @(210, 530, 570) @(48, 58, 58, 58, 58, 58, 58, 58) $headers $rows
    Save-Figure $bmp $g "ch5-table-1-core-mechanisms.png"
}

function Table-52 {
    $c = New-Canvas 1600 470; $bmp = $c[0]; $g = $c[1]
    $headers = @("公式", "含义", "用途")
    $rows = @(
        @("w(u,t) = tf(u,t) x idf(t) x decay(t) x seed(u,t)", "用户标签权重", "构建用户画像"),
        @("decay(t) = exp(-lambda x days(t))", "时间衰减因子", "降低旧标签影响"),
        @("sim(u,v) = (P_u · P_v) / (||P_u|| x ||P_v||)", "画像余弦相似度", "计算基础兴趣分"),
        @("S_final = S_i + S_c + alpha x S_t", "最终推荐分", "汇总兴趣、规则和可信连接"),
        @("S_c = sum(baseRuleScore_r x scenarioMultiplier_r x scale)", "校园规则分", "累加场景规则贡献")
    )
    Draw-Table $g 80 30 @(760, 280, 380) @(50, 64, 64, 64, 64, 64) $headers $rows
    Save-Figure $bmp $g "ch5-table-2-formulas.png"
}

function Draw-UseCase($g, $x, $y, $w, $h, $text) {
    $rect = [System.Drawing.Rectangle]::new($x, $y, $w, $h)
    $g.FillEllipse((Brush "#ffffff"), $rect)
    $g.DrawEllipse((Pen "#111827" 2), $rect)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, (Font 13 Regular), (Brush "#111827"), [System.Drawing.RectangleF]::new($x + 8, $y + 4, $w - 16, $h - 8), $sf)
}

function Draw-Actor($g, $x, $y, $label) {
    $pen = Pen "#111827" 2
    $g.DrawEllipse($pen, $x + 24, $y, 26, 26)
    $g.DrawLine($pen, $x + 37, $y + 26, $x + 37, $y + 82)
    $g.DrawLine($pen, $x + 8, $y + 48, $x + 66, $y + 48)
    $g.DrawLine($pen, $x + 37, $y + 82, $x + 14, $y + 120)
    $g.DrawLine($pen, $x + 37, $y + 82, $x + 60, $y + 120)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($label, (Font 12 Regular), (Brush "#111827"), [System.Drawing.RectangleF]::new($x - 18, $y + 126, 112, 36), $sf)
}

function Draw-Class($g, $x, $y, $w, $h, $title, $members) {
    $g.FillRectangle((Brush "#ffffff"), $x, $y, $w, $h)
    $g.DrawRectangle((Pen "#111827" 2), $x, $y, $w, $h)
    $g.FillRectangle((Brush "#e8f0fb"), $x, $y, $w, 38)
    $g.DrawLine((Pen "#111827" 1), $x, $y + 38, $x + $w, $y + 38)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($title, (Font 12 Bold), (Brush "#111827"), [System.Drawing.RectangleF]::new($x + 4, $y, $w - 8, 38), $sf)
    $body = [string]::Join("`n", $members)
    $sf2 = New-Object System.Drawing.StringFormat
    $sf2.Alignment = [System.Drawing.StringAlignment]::Near
    $g.DrawString($body, (Font 10 Regular), (Brush "#111827"), [System.Drawing.RectangleF]::new($x + 10, $y + 48, $w - 20, $h - 54), $sf2)
}

function Figure-31 {
    $c = New-Canvas 1450 700; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "用户与标签维护用例图" 1450
    $g.DrawRectangle((Pen "#111827" 2), 330, 105, 760, 500)
    $g.DrawString("校园社交匹配推荐系统", (Font 14 Bold), (Brush "#111827"), 580, 126)
    Draw-Actor $g 120 210 "校园用户"
    Draw-Actor $g 1180 210 "系统管理员"
    Draw-UseCase $g 475 180 220 70 "维护个人资料"
    Draw-UseCase $g 475 310 220 70 "维护兴趣标签"
    Draw-UseCase $g 735 180 220 70 "维护标签体系"
    Draw-UseCase $g 735 310 220 70 "维护基础用户数据"
    Draw-UseCase $g 600 450 230 70 "保存用户标签关系"
    Draw-Arrow $g 190 285 475 215
    Draw-Arrow $g 190 285 475 345
    Draw-Arrow $g 1180 285 955 215
    Draw-Arrow $g 1180 285 955 345
    Draw-Arrow $g 585 380 660 450
    Draw-Arrow $g 845 380 760 450
    Save-Figure $bmp $g "ch3-1-user-tag-usecase.png"
}

function Figure-32 {
    $c = New-Canvas 1450 720; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "画像构建与推荐生成用例图" 1450
    $g.DrawRectangle((Pen "#111827" 2), 320, 105, 800, 520)
    $g.DrawString("校园社交匹配推荐系统", (Font 14 Bold), (Brush "#111827"), 600, 126)
    Draw-Actor $g 110 230 "校园用户"
    Draw-Actor $g 1200 230 "系统管理员"
    Draw-UseCase $g 440 165 230 68 "请求推荐结果"
    Draw-UseCase $g 440 285 230 68 "构建用户画像"
    Draw-UseCase $g 720 225 240 68 "候选召回"
    Draw-UseCase $g 720 345 240 68 "排序与重排"
    Draw-UseCase $g 575 485 260 68 "返回 Top-K 推荐结果"
    Draw-UseCase $g 860 485 200 68 "重建索引"
    Draw-Arrow $g 180 305 440 199
    Draw-Arrow $g 555 233 555 285
    Draw-Arrow $g 670 319 720 260
    Draw-Arrow $g 840 293 840 345
    Draw-Arrow $g 840 413 705 485
    Draw-Arrow $g 1200 305 960 520
    Save-Figure $bmp $g "ch3-2-recommendation-usecase.png"
}

function Figure-33 {
    $c = New-Canvas 1400 660; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "推荐解释用例图" 1400
    $g.DrawRectangle((Pen "#111827" 2), 340, 115, 720, 450)
    $g.DrawString("校园社交匹配推荐系统", (Font 14 Bold), (Brush "#111827"), 575, 136)
    Draw-Actor $g 120 235 "校园用户"
    Draw-Actor $g 1160 235 "外部大语言`n模型服务"
    Draw-UseCase $g 450 190 240 72 "查看推荐解释"
    Draw-UseCase $g 710 190 240 72 "抽取解释证据"
    Draw-UseCase $g 450 350 240 72 "生成规则解释"
    Draw-UseCase $g 710 350 240 72 "润色解释文本"
    Draw-Arrow $g 190 310 450 226
    Draw-Arrow $g 690 226 710 226
    Draw-Arrow $g 570 262 570 350
    Draw-Arrow $g 690 386 710 386
    Draw-Arrow $g 1160 310 950 386
    Save-Figure $bmp $g "ch3-3-explanation-usecase.png"
}

function Figure-34 {
    $c = New-Canvas 1400 680; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "用户反馈与画像更新用例图" 1400
    $g.DrawRectangle((Pen "#111827" 2), 350, 115, 720, 460)
    $g.DrawString("校园社交匹配推荐系统", (Font 14 Bold), (Brush "#111827"), 585, 136)
    Draw-Actor $g 130 250 "校园用户"
    Draw-UseCase $g 455 185 240 72 "提交关注/忽略反馈"
    Draw-UseCase $g 745 185 220 72 "保存反馈记录"
    Draw-UseCase $g 455 355 240 72 "读取推荐证据标签"
    Draw-UseCase $g 745 355 220 72 "重建用户画像"
    Draw-Arrow $g 200 325 455 221
    Draw-Arrow $g 695 221 745 221
    Draw-Arrow $g 855 257 575 355
    Draw-Arrow $g 695 391 745 391
    Save-Figure $bmp $g "ch3-4-feedback-usecase.png"
}

function Figure-41 {
    $c = New-Canvas 1650 1120; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "系统架构图" 1650
    Draw-Box $g 85 190 170 86 "校园用户" "#ffffff" "#111827" 13
    Draw-Box $g 85 390 170 86 "系统管理员" "#ffffff" "#111827" 13
    Draw-Box $g 1370 310 190 90 "外部大语言`n模型服务" "#ffffff" "#111827" 12
    $g.DrawRectangle((Pen "#111827" 2), 340, 110, 900, 790)
    $g.DrawString("校园社交匹配推荐系统", (Font 16 Bold), (Brush "#111827"), 670, 132)
    Draw-Box $g 470 185 640 62 "接口层：用户、标签、画像、推荐、解释、反馈 Controller" "#eef6ff" "#1d4ed8" 12
    Draw-Box $g 470 300 640 70 "应用服务层：业务编排与推荐主链路" "#f0fdf4" "#15803d" 13
    Draw-Box $g 470 430 640 92 "领域能力层：画像计算 / 候选召回 / 相似度排序 / 校园规则重排 / 解释证据抽取" "#fefce8" "#a16207" 12
    Draw-Box $g 470 580 640 70 "数据访问层：Mapper / Repository" "#fff7ed" "#c2410c" 13
    Draw-Box $g 470 760 250 82 "MySQL`n结构化业务数据" "#ffffff" "#111827" 12
    Draw-Box $g 860 760 250 82 "Redis`n倒排索引与缓存" "#ffffff" "#111827" 12
    Draw-Arrow $g 255 233 470 216
    Draw-Arrow $g 255 433 470 216
    Draw-Arrow $g 790 247 790 300
    Draw-Arrow $g 790 370 790 430
    Draw-Arrow $g 790 522 790 580
    Draw-Arrow $g 650 650 600 760
    Draw-Arrow $g 930 650 985 760
    Draw-Arrow $g 1110 476 1370 355
    Save-Figure $bmp $g "ch4-1-system-architecture.png"
}

function Figure-42 {
    $c = New-Canvas 1700 820; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "系统功能结构图" 1700
    Draw-Box $g 600 90 500 70 "校园社交匹配推荐系统" "#0f4c92" "#0f4c92" 18
    $cols = @(
        @("用户与标签维护", @("用户资料维护", "标签体系维护", "用户标签绑定"), 130),
        @("用户画像构建", @("标签权重计算", "Top-K 标签截取", "画像缓存"), 390),
        @("匹配推荐生成", @("候选召回", "相似度排序", "校园规则重排"), 650),
        @("推荐解释", @("证据抽取", "规则解释", "解释润色"), 910),
        @("用户反馈更新", @("反馈记录", "权重调整", "画像重建"), 1170),
        @("运行评估", @("透明链路", "双视图对比", "离线评估"), 1430)
    )
    foreach ($col in $cols) {
        Draw-Arrow $g 850 160 ($col[2] + 80) 235
        Draw-Box $g $col[2] 235 190 62 $col[0] "#dbeafe" "#1d4ed8" 12
        $y = 345
        foreach ($item in $col[1]) {
            Draw-Box $g ($col[2] + 14) $y 162 54 $item "#ffffff" "#64748b" 11
            Draw-Arrow $g ($col[2] + 95) 297 ($col[2] + 95) $y "#9ca3af"
            $y += 88
        }
    }
    Save-Figure $bmp $g "ch4-2-function-structure.png"
}

function Figure-43 {
    $c = New-Canvas 1550 980; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "系统 ER 图" 1550
    Draw-Box $g 90 170 190 62 "用户" "#ffffff" "#111827" 12
    Draw-Box $g 390 170 190 62 "用户标签关系" "#ffffff" "#111827" 12
    Draw-Box $g 690 170 190 62 "标签" "#ffffff" "#111827" 12
    Draw-Box $g 390 360 190 62 "用户画像" "#ffffff" "#111827" 12
    Draw-Box $g 690 360 210 62 "推荐结果" "#ffffff" "#111827" 12
    Draw-Box $g 1010 260 220 62 "推荐解释" "#ffffff" "#111827" 12
    Draw-Box $g 1010 460 190 62 "用户反馈" "#ffffff" "#111827" 12
    Draw-Arrow $g 280 201 390 201; $g.DrawString("1", (Font 11 Regular), (Brush "#111827"), 300, 178); $g.DrawString("n", (Font 11 Regular), (Brush "#111827"), 365, 178)
    Draw-Arrow $g 580 201 690 201; $g.DrawString("n", (Font 11 Regular), (Brush "#111827"), 600, 178); $g.DrawString("1", (Font 11 Regular), (Brush "#111827"), 665, 178)
    Draw-Arrow $g 485 232 485 360; $g.DrawString("1", (Font 11 Regular), (Brush "#111827"), 500, 255); $g.DrawString("n", (Font 11 Regular), (Brush "#111827"), 500, 330)
    Draw-Arrow $g 580 391 690 391
    Draw-Arrow $g 900 391 1010 291
    Draw-Arrow $g 900 391 1010 491
    Draw-Arrow $g 185 232 690 391 "#6b7280"
    Save-Figure $bmp $g "ch4-3-er-diagram.png"
}

function Figure-51 {
    $c = New-Canvas 1700 900; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "用户画像构建模块类图" 1700
    Draw-Class $g 90 150 260 130 "ProfileController" @("+ buildProfile()", "+ getProfile()")
    Draw-Class $g 470 130 300 170 "ProfileServiceImpl" @("- weightCalculator", "- userTagRelationMapper", "- userProfileMapper", "- profileCacheRepository", "+ buildProfile()", "+ getProfile()")
    Draw-Class $g 900 130 320 140 "ProfileWeightCalculator" @("+ calculate(userId, relations)")
    Draw-Class $g 900 340 320 150 "ImprovedTfIdfProfileWeightCalculator" @("+ calculate()", "- applyTimeDecay()", "- selectTopK()")
    Draw-Class $g 120 560 280 130 "UserTagRelationMapper" @("+ selectList()")
    Draw-Class $g 510 560 260 130 "UserProfileMapper" @("+ insert()", "+ selectOne()")
    Draw-Class $g 900 560 300 130 "ProfileCacheRepository" @("+ get()", "+ save()")
    Draw-Arrow $g 350 215 470 215
    Draw-Arrow $g 770 215 900 200
    Draw-Arrow $g 1060 270 1060 340
    Draw-Arrow $g 610 300 260 560
    Draw-Arrow $g 630 300 640 560
    Draw-Arrow $g 680 300 1030 560
    Save-Figure $bmp $g "ch5-1-profile-class-diagram.png"
}

function Figure-52 {
    $c = New-Canvas 1700 980; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "用户画像生成时序图" 1700
    $lanes = @(180, 430, 680, 930, 1180, 1430)
    $labels = @("用户", "ProfileController", "ProfileService", "标签关系Mapper", "权重计算器", "画像存储/缓存")
    for ($i=0; $i -lt $lanes.Count; $i++) { Draw-Lane $g $lanes[$i] 130 900 $labels[$i] }
    Draw-Message $g 180 430 190 "请求生成/刷新画像"
    Draw-Message $g 430 680 280 "buildProfile(userId)"
    Draw-Message $g 680 930 370 "查询用户标签关系"
    Draw-Message $g 930 680 460 "返回标签、时间、权重种子"
    Draw-Message $g 680 1180 550 "计算 TF-IDF、时间衰减、Top-K"
    Draw-Message $g 1180 680 640 "返回画像权重向量"
    Draw-Message $g 680 1430 730 "保存画像并写入缓存"
    Draw-Message $g 680 430 820 "返回画像结果"
    Save-Figure $bmp $g "ch5-2-profile-sequence.png"
}

function Figure-54 {
    $c = New-Canvas 1720 930; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "匹配推荐生成模块类图" 1720
    Draw-Class $g 70 105 260 120 "RecommendationController" @("+ recommend()", "+ detail()")
    Draw-Class $g 450 80 320 190 "RecommendationServiceImpl" @("- profileService", "- recallService", "- rankingService", "- rerankService", "- explanationService", "+ recommend()")
    $classes = @(
        @("ProfileService", 90, 410), @("RecallService", 370, 410), @("RankingService", 650, 410),
        @("RerankService", 930, 410), @("TrustScoreService", 1210, 410), @("ExplorationService", 90, 640),
        @("RecallIndexRepository", 370, 640), @("RerankRule", 650, 640), @("RecommendationResultMapper", 930, 640), @("ExplanationService", 1210, 640)
    )
    foreach ($cinfo in $classes) { Draw-Class $g $cinfo[1] $cinfo[2] 230 105 $cinfo[0] @("+ execute()") }
    Draw-Arrow $g 330 165 450 175
    foreach ($cinfo in $classes) { Draw-Arrow $g 610 270 ($cinfo[1] + 115) $cinfo[2] "#6b7280" }
    Save-Figure $bmp $g "ch5-4-recommendation-class-diagram.png"
}

function Figure-55 {
    $c = New-Canvas 1700 960; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "匹配推荐生成时序图" 1700
    $lanes = @(150, 380, 610, 840, 1070, 1300, 1530)
    $labels = @("用户", "RecommendationController", "RecommendationService", "ProfileService", "RecallService", "RankingService", "RerankService")
    for ($i=0; $i -lt $lanes.Count; $i++) { Draw-Lane $g $lanes[$i] 130 900 $labels[$i] }
    Draw-Message $g 150 380 190 "请求 Top-K 推荐"
    Draw-Message $g 380 610 280 "recommend(userId, topK)"
    Draw-Message $g 610 840 370 "获取目标用户画像"
    Draw-Message $g 610 1070 460 "按 Top-K 标签召回候选"
    Draw-Message $g 610 1300 550 "计算余弦相似度"
    Draw-Message $g 610 1530 640 "执行校园规则重排"
    Draw-Message $g 1530 610 730 "返回最终排序"
    Draw-Message $g 610 380 820 "返回推荐详情"
    Save-Figure $bmp $g "ch5-5-recommendation-sequence.png"
}

function Figure-57 {
    $c = New-Canvas 1700 900; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "推荐解释与用户反馈模块类图" 1700
    Draw-Class $g 90 120 260 130 "RecommendationController" @("+ getExplanation()")
    Draw-Class $g 90 340 260 130 "FeedbackController" @("+ submitFeedback()")
    Draw-Class $g 470 100 300 180 "ExplanationServiceImpl" @("- evidenceExtractor", "- templateBuilder", "- aiClient", "+ generate()", "+ getByRecommendationId()")
    Draw-Class $g 470 350 300 150 "FeedbackServiceImpl" @("- feedbackMapper", "- profileService", "+ submitFeedback()", "+ applyFeedbackUpdate()")
    Draw-Class $g 900 80 300 120 "ExplanationEvidenceExtractor" @("+ extract()")
    Draw-Class $g 900 240 300 120 "ExplanationTemplateBuilder" @("+ build()")
    Draw-Class $g 900 400 300 120 "AiExplanationClient" @("+ generateExplanation()")
    Draw-Class $g 1280 150 300 120 "RecommendationExplanationMapper" @("+ insert()", "+ selectOne()")
    Draw-Class $g 1280 360 260 120 "UserFeedbackMapper" @("+ insert()", "+ selectList()")
    Draw-Arrow $g 350 185 470 190
    Draw-Arrow $g 350 405 470 425
    Draw-Arrow $g 770 190 900 140
    Draw-Arrow $g 770 190 900 300
    Draw-Arrow $g 770 190 900 460
    Draw-Arrow $g 770 190 1280 210
    Draw-Arrow $g 770 425 1280 420
    Save-Figure $bmp $g "ch5-7-explanation-feedback-class-diagram.png"
}

function Figure-58 {
    $c = New-Canvas 1700 940; $bmp = $c[0]; $g = $c[1]
    Draw-Title $g "用户反馈更新时序图" 1700
    $lanes = @(170, 430, 690, 950, 1210, 1470)
    $labels = @("用户", "FeedbackController", "FeedbackService", "UserFeedbackMapper", "RecommendationResultMapper", "ProfileService")
    for ($i=0; $i -lt $lanes.Count; $i++) { Draw-Lane $g $lanes[$i] 130 880 $labels[$i] }
    Draw-Message $g 170 430 200 "提交关注/忽略反馈"
    Draw-Message $g 430 690 300 "submitFeedback()"
    Draw-Message $g 690 950 400 "保存反馈记录"
    Draw-Message $g 690 1210 500 "读取推荐证据标签"
    Draw-Message $g 690 690 600 "调整相关标签权重"
    Draw-Message $g 690 1470 700 "重建用户画像"
    Draw-Message $g 690 430 800 "返回处理结果"
    Save-Figure $bmp $g "ch5-8-feedback-sequence.png"
}

function Table-52 {
    $c = New-Canvas 1520 440; $bmp = $c[0]; $g = $c[1]
    $headers = @("数据项", "含义", "用途")
    $rows = @(
        @("标签贡献", "共同标签及其对相似度的贡献", "支撑推荐解释和画像更新"),
        @("规则命中", "年级、专业、社团等校园规则命中情况", "支撑场景重排和解释生成"),
        @("可信连接原因", "资料完整度、反馈记录等可信依据", "支撑可信连接分和解释展示"),
        @("反馈类型", "关注或忽略", "决定画像权重调整方向"),
        @("画像版本", "用户画像更新后的版本号", "支撑反馈前后结果对比")
    )
    Draw-Table $g 80 30 @(280, 570, 500) @(50, 58, 58, 58, 58, 58) $headers $rows
    Save-Figure $bmp $g "ch5-table-2-explanation-feedback-data.png"
}

function Table-51 {
    $c = New-Canvas 1560 450; $bmp = $c[0]; $g = $c[1]
    $headers = @("机制", "作用", "实现要点")
    $rows = @(
        @("候选召回", "缩小排序计算范围", "根据 Top-K 标签读取倒排集合并合并候选用户"),
        @("基础排序", "计算兴趣相似度", "使用画像向量余弦相似度，并记录标签贡献"),
        @("场景重排", "适配校园匹配场景", "按学习、社团、兴趣模式调整规则权重"),
        @("可信连接分", "抑制证据不足的推荐对象", "根据资料完整度和历史反馈生成可信原因"),
        @("轻量探索", "保留少量非最高相似度对象", "在不破坏主排序的前提下增加推荐多样性")
    )
    Draw-Table $g 80 30 @(250, 480, 650) @(50, 58, 58, 58, 58, 58) $headers $rows
    Save-Figure $bmp $g "ch5-table-1-core-mechanisms.png"
}

Figure-11
Figure-31
Figure-32
Figure-33
Figure-34
Figure-41
Figure-42
Figure-43
Figure-51
Figure-52
Figure-54
Figure-55
Figure-56
Figure-57
Figure-58
Table-51
Table-52
Generate-MarkdownTables
