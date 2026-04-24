const state = {
  userId: 2001,
  topK: 3,
  scenarioMode: "interest_partner",
  presentationMode: "defense",
  scenarioModes: "interest_partner,study_partner,club_partner",
  matrixTopKs: "3,5",
  profileTopTagCounts: "3,5",
  rerankWeightScales: "0.8,1.0,1.2",
  currentRecommendation: null,
  currentProfile: null,
  currentStory: null,
  currentComparison: null,
  feedbackBefore: null,
  feedbackAfter: null
};

const els = Object.fromEntries(
  [
    "status-strip", "log-stream", "overview-grid", "overview-timestamp", "export-list",
    "hero-data-scale", "hero-user-id", "hero-user-display", "hero-scenario-label",
    "user-id", "top-k", "scenario-mode", "scenario-modes", "matrix-topks",
    "profile-top-tag-counts", "rerank-weight-scales", "story-title", "story-persona",
    "story-narrative", "algorithm-highlight-list", "candidate-spotlight-list",
    "profile-top-tags", "profile-json", "profile-updated-at", "evaluation-generated-at",
    "evaluation-summary-body", "baseline-comparison-grid", "compare-meta", "tag-overlap-title",
    "tag-overlap-summary", "tag-overlap-list", "full-pipeline-title", "full-pipeline-summary",
    "full-pipeline-list", "recommendation-list", "recommendation-trace", "recommendation-detail",
    "detail-meta", "explanation-meta", "explanation-summary", "explanation-detail",
    "explanation-primary", "explanation-rule", "explanation-llm", "presentation-mode-toggle",
    "feedback-diff-meta", "feedback-before-tags", "feedback-after-tags", "feedback-before-list",
    "feedback-after-list", "feedback-change-log", "recommendation-card-template"
  ].map((id) => [camel(id), document.getElementById(id)])
);

function camel(id) {
  return id.replace(/-([a-z])/g, (_, char) => char.toUpperCase());
}

function nowLabel() {
  return new Date().toLocaleTimeString("zh-CN", { hour12: false });
}

function safeArray(value) {
  return Array.isArray(value) ? value : [];
}

function formatValue(value, fallback = "-") {
  return value === null || value === undefined || value === "" ? fallback : String(value);
}

function formatScore(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  const numeric = Number(value);
  return Number.isNaN(numeric) ? String(value) : numeric.toFixed(4);
}

function clearNode(node) {
  while (node.firstChild) {
    node.removeChild(node.firstChild);
  }
}

function createChip(text, className = "") {
  const chip = document.createElement("span");
  chip.className = `chip ${className}`.trim();
  chip.textContent = text;
  return chip;
}

function createSimpleCard(titleText, bodyText) {
  const card = document.createElement("article");
  card.className = "simple-card";
  const title = document.createElement("strong");
  title.textContent = titleText;
  const body = document.createElement("span");
  body.textContent = bodyText;
  card.append(title, body);
  return card;
}

function addLog(message) {
  const row = document.createElement("div");
  row.className = "log-entry";
  const time = document.createElement("span");
  time.className = "log-time";
  time.textContent = nowLabel();
  const text = document.createElement("span");
  text.className = "log-message";
  text.textContent = message;
  row.append(time, text);
  els.logStream.prepend(row);
}

function setStatus(message, isError = false) {
  clearNode(els.statusStrip);
  const dot = document.createElement("span");
  dot.className = "status-dot";
  dot.style.background = isError ? "#bf4d28" : "var(--accent)";
  const text = document.createElement("span");
  text.textContent = message;
  els.statusStrip.append(dot, text);
}

async function apiFetch(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });
  const isJson = response.headers.get("content-type")?.includes("application/json");
  const payload = isJson ? await response.json() : await response.text();
  if (!response.ok) {
    throw new Error(typeof payload === "string" ? payload : payload?.message || `HTTP ${response.status}`);
  }
  if (isJson && payload.success === false) {
    throw new Error(payload.message || "接口返回失败");
  }
  return isJson ? payload.data : payload;
}

function renderChipList(container, values, emptyLabel, className) {
  clearNode(container);
  const list = safeArray(values);
  if (!list.length) {
    container.appendChild(createChip(emptyLabel, "chip-muted"));
    return;
  }
  list.forEach((value) => container.appendChild(createChip(value, className)));
}

function renderStory(story) {
  state.currentStory = story;
  els.heroScenarioLabel.textContent = formatValue(story.scenarioLabel, "兴趣搭子");
  els.heroUserDisplay.textContent = `用户 ${story.demoUserId}`;
  els.storyTitle.textContent = formatValue(story.storyTitle, "演示故事线");
  els.storyPersona.textContent = formatValue(story.personaSummary, "暂无人设说明");
  els.storyNarrative.textContent = formatValue(story.storyNarrative, "暂无故事线说明");

  clearNode(els.algorithmHighlightList);
  safeArray(story.algorithmHighlights).forEach((highlight) => {
    const card = document.createElement("article");
    card.className = "highlight-card";
    const title = document.createElement("strong");
    title.textContent = highlight;
    card.appendChild(title);
    els.algorithmHighlightList.appendChild(card);
  });

  clearNode(els.candidateSpotlightList);
  safeArray(story.candidateSpotlights).forEach((spotlight) => {
    const card = document.createElement("article");
    card.className = "spotlight-card";
    const title = document.createElement("h4");
    title.textContent = `${formatValue(spotlight.candidateNickname, "候选用户")} · ${formatValue(spotlight.candidateUserId)}`;
    const reason = document.createElement("p");
    reason.textContent = formatValue(spotlight.storyReason, "暂无说明");
    const tags = document.createElement("div");
    tags.className = "chip-group";
    safeArray(spotlight.highlightTags).forEach((tag) => tags.appendChild(createChip(tag, "chip-tag")));
    if (!tags.children.length) {
      tags.appendChild(createChip("暂无亮点标签", "chip-muted"));
    }
    card.append(title, reason, tags);
    els.candidateSpotlightList.appendChild(card);
  });
}

function renderOverview(summary) {
  const items = [
    ["当前模式", `${formatValue(summary.scenarioLabel)} / ${formatValue(summary.scenarioMode)}`],
    ["活跃用户", summary.activeUserCount ?? "-"],
    ["标签数量", summary.tagCount ?? "-"],
    ["关系数量", summary.relationCount ?? "-"]
  ];
  els.overviewGrid.innerHTML = items.map(([label, value]) => `
    <div>
      <dt>${label}</dt>
      <dd>${value}</dd>
    </div>
  `).join("");
  els.evaluationGeneratedAt.textContent = formatValue(summary.generatedAt, "未知");
  els.overviewTimestamp.textContent = formatValue(summary.generatedAt, "未知");
  els.heroDataScale.textContent = `${summary.activeUserCount ?? "-"} / ${summary.tagCount ?? "-"} / ${summary.relationCount ?? "-"}`;
}

function renderEvaluationSummary(summary) {
  const baselines = safeArray(summary.baselines);
  if (!baselines.length) {
    els.evaluationSummaryBody.innerHTML = `<tr><td colspan="4">当前没有返回评估基线。</td></tr>`;
    return;
  }
  els.evaluationSummaryBody.innerHTML = baselines.map((baseline) => `
    <tr>
      <td>${formatValue(baseline.baselineName || baseline.baselineCode)}</td>
      <td>${formatScore(baseline.precisionAtK)}</td>
      <td>${formatScore(baseline.hitRateAtK)}</td>
      <td>${formatScore(baseline.explanationPresenceRate)}</td>
    </tr>
  `).join("");

  const noTrust = baselines.find((item) => item.baselineCode === "full_pipeline_no_trust");
  const withTrust = baselines.find((item) => item.baselineCode === "full_pipeline_with_trust");
  const tagOverlap = baselines.find((item) => item.baselineCode === "tag_overlap");

  clearNode(els.baselineComparisonGrid);
  els.baselineComparisonGrid.append(
    createSimpleCard(
      "完整链路 vs 标签重叠",
      tagOverlap && withTrust
        ? `Precision@K 从 ${formatScore(tagOverlap.precisionAtK)} 提升到 ${formatScore(withTrust.precisionAtK)}。`
        : "等待基线数据。"
    ),
    createSimpleCard(
      "完整链路（含可信分）vs 无可信分",
      noTrust && withTrust
        ? `Precision@K 从 ${formatScore(noTrust.precisionAtK)} 变化到 ${formatScore(withTrust.precisionAtK)}。`
        : "等待可信连接分消融数据。"
    ),
    createSimpleCard(
      "当前结论",
      withTrust
        ? `当前模式 ${formatValue(summary.scenarioLabel)} 的解释覆盖率为 ${formatScore(withTrust.explanationPresenceRate)}。`
        : "等待完整链路数据。"
    )
  );
}

function renderProfile(profile) {
  state.currentProfile = profile;
  els.profileUpdatedAt.textContent = formatValue(profile.updatedAt, "未知");
  els.profileJson.textContent = JSON.stringify(profile, null, 2);
  renderChipList(els.profileTopTags, profile.topkTags, "暂无 Top 标签", "chip-tag");
}

function renderRecommendationDetail(detail) {
  state.currentRecommendation = detail;
  els.recommendationTrace.textContent = `requestTraceId：${formatValue(detail.requestTraceId, "unknown")}`;
  els.detailMeta.textContent = `候选规模：${formatValue(detail.recallCandidatesCount, 0)} · 模式：${formatValue(detail.scenarioLabel)}`;
  els.recommendationDetail.textContent = JSON.stringify(detail, null, 2);
}

function renderRecommendationCards(detail) {
  clearNode(els.recommendationList);
  const items = safeArray(detail.items);
  if (!items.length) {
    const empty = document.createElement("article");
    empty.className = "candidate-card empty-card";
    empty.innerHTML = "<p>当前没有返回推荐结果。</p>";
    els.recommendationList.appendChild(empty);
    return;
  }

  items.forEach((item) => {
    const fragment = els.recommendationCardTemplate.content.cloneNode(true);
    fragment.querySelector(".rank-chip").textContent = `排名 #${formatValue(item.rankNo)}`;
    fragment.querySelector(".label-chip").textContent = formatValue(item.recommendationLabel, "兴趣同频匹配");
    if (item.exploration) {
      fragment.querySelector(".label-chip").textContent = "轻量探索位";
    }
    fragment.querySelector(".candidate-name").textContent = formatValue(item.targetNickname, `用户 ${item.targetUserId}`);
    fragment.querySelector(".candidate-meta").textContent =
      `${formatValue(item.scenarioLabel, "兴趣搭子")} · 用户 ID ${formatValue(item.targetUserId)}`;
    fragment.querySelector(".interest-score").textContent = formatScore(item.interestScore ?? item.rankScore);
    fragment.querySelector(".campus-score").textContent = formatScore(item.campusScore ?? item.rerankScore);
    fragment.querySelector(".trust-score").textContent = formatScore(item.trustScore);
    fragment.querySelector(".final-score").textContent = formatScore(item.finalScore);
    fragment.querySelector(".candidate-explanation").textContent = formatValue(item.explanation || item.reasonText, "暂无解释");

    renderChipList(fragment.querySelector(".matched-tags"), item.matchedTags, "暂无匹配标签", "chip-tag");
    renderChipList(fragment.querySelector(".matched-rules"), item.matchedRules, "暂无命中规则", "chip-rule");
    renderChipList(fragment.querySelector(".trust-reasons"), item.trustReasons, "暂无可信信号", "chip-accent");
    if (item.exploration) {
      fragment.querySelector(".matched-rules").appendChild(createChip("轻量探索位", "chip-rule"));
      if (item.explorationReason) {
        fragment.querySelector(".trust-reasons").appendChild(createChip(item.explorationReason, "chip-accent"));
      }
    }

    fragment.querySelector(".explanation-button").addEventListener("click", () => safeAction(() => loadExplanation(item.recommendationId))());
    fragment.querySelector(".feedback-button").addEventListener("click", () => safeAction(() => submitFeedback(item))());
    els.recommendationList.appendChild(fragment);
  });
}

function renderComparison(comparison) {
  state.currentComparison = comparison;
  els.compareMeta.textContent = `模式：${formatValue(comparison.scenarioLabel)} · 候选池：${formatValue(comparison.candidateCount)}`;
  els.tagOverlapTitle.textContent = formatValue(comparison.tagOverlapView?.viewName, "标签重叠基线");
  els.tagOverlapSummary.textContent = formatValue(comparison.tagOverlapView?.summary, "等待说明");
  els.fullPipelineTitle.textContent = formatValue(comparison.fullPipelineView?.viewName, "完整链路");
  els.fullPipelineSummary.textContent = formatValue(comparison.fullPipelineView?.summary, "等待说明");
  renderCompareList(els.tagOverlapList, comparison.tagOverlapView?.items, false);
  renderCompareList(els.fullPipelineList, comparison.fullPipelineView?.items, true);
}

function renderCompareList(container, items, includeTrust) {
  clearNode(container);
  const list = safeArray(items);
  if (!list.length) {
    container.appendChild(createSimpleCard("暂无结果", "当前没有对比数据。"));
    return;
  }
  list.forEach((item) => {
    const card = document.createElement("article");
    card.className = "simple-card";
    const title = document.createElement("strong");
    title.textContent = `#${formatValue(item.rankNo)} ${formatValue(item.targetNickname, `用户 ${item.targetUserId}`)}`;
    const body = document.createElement("span");
    body.textContent = includeTrust
      ? `兴趣 ${formatScore(item.interestScore ?? item.rankScore)} / 场景 ${formatScore(item.campusScore ?? item.rerankScore)} / 可信 ${formatScore(item.trustScore)}`
      : `召回 ${formatScore(item.recallScore)} / 排序 ${formatScore(item.rankScore)}`;
    const chips = document.createElement("div");
    chips.className = "chip-group";
    safeArray(item.matchedTags).forEach((tag) => chips.appendChild(createChip(tag, "chip-tag")));
    if (includeTrust) {
      safeArray(item.trustReasons).forEach((reason) => chips.appendChild(createChip(reason, "chip-accent")));
      if (item.exploration) {
        chips.appendChild(createChip("轻量探索位", "chip-rule"));
        if (item.explorationReason) {
          chips.appendChild(createChip(item.explorationReason, "chip-accent"));
        }
      }
    }
    card.append(title, body, chips);
    container.appendChild(card);
  });
}

function renderExplanation(explanation, recommendationId) {
  const reasonSource = explanation.reasonSource === "llm" ? "LLM 改写解释" : "规则解释";
  els.explanationMeta.textContent = `recommendationId：${recommendationId} · ${reasonSource}`;
  els.explanationDetail.textContent = JSON.stringify(explanation, null, 2);
  els.explanationPrimary.textContent = formatValue(explanation.reasonText, "鏆傛棤瑙ｉ噴");
  els.explanationRule.textContent = formatValue(explanation.ruleReasonText || explanation.reasonText, "鏆傛棤瑙勫垯瑙ｉ噴");
  els.explanationLlm.textContent = explanation.reasonSource === "llm"
    ? formatValue(explanation.llmReasonText || explanation.reasonText, "鏆傛棤 LLM 鏀瑰啓")
    : "当前未启用 LLM 改写，此处展示规则解释回退结果。";
  clearNode(els.explanationSummary);
  const pills = [];
  pills.push(createChip(reasonSource, explanation.reasonSource === "llm" ? "chip-accent" : "chip-rule"));
  if (explanation.reasonText) {
    pills.push(createChip(explanation.reasonText, "chip-accent"));
  }
  if (explanation.reasonSource === "llm" && explanation.ruleReasonText) {
    pills.push(createChip(`规则依据：${explanation.ruleReasonText}`, "chip-rule"));
  }
  const evidence = explanation.evidence && typeof explanation.evidence === "object" ? explanation.evidence : {};
  safeArray(evidence.trustReasons).forEach((reason) => pills.push(createChip(reason, "chip-accent")));
  safeArray(evidence.ruleHits).slice(0, 3).forEach((rule) => {
    if (rule?.ruleDesc) {
      pills.push(createChip(rule.ruleDesc, "chip-rule"));
    }
  });
  if (evidence.exploration) {
    pills.push(createChip("轻量探索位", "chip-rule"));
  }
  if (evidence.explorationReason) {
    pills.push(createChip(evidence.explorationReason, "chip-accent"));
  }
  if (!pills.length) {
    pills.push(createChip("当前没有结构化解释证据", "chip-muted"));
  }
  pills.forEach((pill) => els.explanationSummary.appendChild(pill));
}

function applyPresentationMode() {
  document.body.classList.toggle("defense-mode", state.presentationMode === "defense");
}

function snapshotFeedbackState() {
  return {
    topTags: safeArray(state.currentProfile?.topkTags),
    items: safeArray(state.currentRecommendation?.items).map((item) => ({
      targetUserId: item.targetUserId,
      targetNickname: item.targetNickname,
      finalScore: item.finalScore
    }))
  };
}

function renderFeedbackSnapshot(containerTags, containerList, snapshot) {
  renderChipList(containerTags, snapshot?.topTags, "暂无画像标签", "chip-tag");
  clearNode(containerList);
  const items = safeArray(snapshot?.items);
  if (!items.length) {
    containerList.appendChild(createSimpleCard("暂无推荐结果", "等待反馈操作。"));
    return;
  }
  items.forEach((item, index) => {
    containerList.appendChild(createSimpleCard(
      `#${index + 1} ${formatValue(item.targetNickname, `用户 ${item.targetUserId}`)}`,
      `最终分 ${formatScore(item.finalScore)}`
    ));
  });
}

function renderFeedbackDiff() {
  renderFeedbackSnapshot(els.feedbackBeforeTags, els.feedbackBeforeList, state.feedbackBefore);
  renderFeedbackSnapshot(els.feedbackAfterTags, els.feedbackAfterList, state.feedbackAfter);
  if (!state.feedbackBefore || !state.feedbackAfter) {
    els.feedbackDiffMeta.textContent = "尚未发生反馈";
    els.feedbackChangeLog.textContent = "在推荐卡片中提交一次“关注反馈”后，这里会展示画像和推荐的变化。";
    return;
  }
  const beforeFirst = state.feedbackBefore.items[0]?.targetNickname || "无";
  const afterFirst = state.feedbackAfter.items[0]?.targetNickname || "无";
  els.feedbackDiffMeta.textContent = `模式：${scenarioLabel(state.scenarioMode)} · ${nowLabel()}`;
  els.feedbackChangeLog.textContent = `反馈后，画像 Top 标签和推荐顺序已刷新。首位推荐从“${beforeFirst}”变化为“${afterFirst}”。`;
}

function scenarioLabel(mode) {
  switch (mode) {
    case "study_partner":
      return "学习搭子";
    case "club_partner":
      return "社团搭子";
    default:
      return "兴趣搭子";
  }
}

async function loadStory() {
  const story = await apiFetch(`/api/admin/demo/story?scenarioMode=${state.scenarioMode}`);
  renderStory(story);
  addLog(`已加载 ${scenarioLabel(state.scenarioMode)} 的答辩故事线。`);
}

async function loadOverview() {
  const summary = await apiFetch(`/api/admin/evaluation/summary?topK=${state.topK}`);
  renderOverview(summary);
  renderEvaluationSummary(summary);
  addLog(`已加载 ${scenarioLabel(state.scenarioMode)} 的评估摘要。`);
}

async function loadComparison() {
  const comparison = await apiFetch(`/api/admin/demo/compare?userId=${state.userId}&topK=${state.topK}&scenarioMode=${state.scenarioMode}`);
  renderComparison(comparison);
  addLog(`已加载 ${scenarioLabel(state.scenarioMode)} 的双视图对比。`);
}

async function loadProfile() {
  const profile = await apiFetch(`/api/profiles/${state.userId}`);
  renderProfile(profile);
  addLog(`已加载用户 ${state.userId} 的画像。`);
}

async function loadRecommendation() {
  const detail = await apiFetch(`/api/recommendations/${state.userId}?topK=${state.topK}&useCache=false&scenarioMode=${state.scenarioMode}`);
  renderRecommendationDetail(detail);
  renderRecommendationCards(detail);
  addLog(`已生成 ${scenarioLabel(state.scenarioMode)} 推荐，共 ${safeArray(detail.items).length} 条。`);
}

async function loadExplanation(recommendationId) {
  const explanation = await apiFetch(`/api/recommendations/${recommendationId}/explanation`);
  renderExplanation(explanation, recommendationId);
  addLog(`已加载 recommendationId=${recommendationId} 的解释详情。`);
}

async function submitFeedback(item) {
  state.feedbackBefore = snapshotFeedbackState();
  renderFeedbackDiff();
  await apiFetch(`/api/recommendations/${state.userId}/feedback`, {
    method: "POST",
    body: JSON.stringify({
      recommendationId: item.recommendationId,
      targetUserId: item.targetUserId,
      feedbackType: "follow"
    })
  });
  await loadProfile();
  await loadRecommendation();
  await loadComparison();
  state.feedbackAfter = snapshotFeedbackState();
  renderFeedbackDiff();
  addLog(`已为 targetUserId=${item.targetUserId} 提交关注反馈，并刷新当前模式结果。`);
}

async function runMockInit() {
  const summary = await apiFetch("/api/admin/mock/init", { method: "POST" });
  renderExportList([
    { title: "初始化结果", content: `用户 ${summary.userCount} / 标签 ${summary.tagCount} / 关系 ${summary.relationCount}` },
    { title: "派生重建", content: `画像 ${summary.profileRebuiltCount ?? "-"} / 召回索引 ${summary.recallIndexCount ?? "-"}` }
  ]);
  addLog("已初始化 mock 数据，并重建画像与召回索引。");
  await reloadAll();
}

function renderExportList(entries) {
  clearNode(els.exportList);
  entries.forEach((entry) => {
    const item = document.createElement("div");
    item.className = "export-item";
    const title = document.createElement("strong");
    title.textContent = entry.title;
    const content = document.createElement("span");
    content.textContent = entry.content;
    item.append(title, content);
    els.exportList.appendChild(item);
  });
}

async function rebuildProfiles() {
  const result = await apiFetch("/api/admin/profiles/rebuild-all", { method: "POST" });
  addLog(`已重建全部画像，数量为 ${result.rebuildCount}。`);
  await loadProfile();
  await loadComparison();
}

async function rebuildRecallIndex() {
  const result = await apiFetch("/api/admin/recall/rebuild-index", { method: "POST" });
  addLog(`已重建召回索引，数量为 ${result.indexCount}。`);
}

async function exportEvaluation() {
  const result = await apiFetch(`/api/admin/evaluation/export?topK=${state.topK}`, { method: "POST" });
  renderExportList([
    { title: "评估快照", content: result.fileName },
    { title: "输出路径", content: result.filePath }
  ]);
  addLog(`已导出最新评估快照：${result.fileName}。`);
}

async function exportMatrix() {
  const params = new URLSearchParams();
  state.scenarioModes.split(",").map((item) => item.trim()).filter(Boolean).forEach((value) => params.append("scenarioModes", value));
  state.matrixTopKs.split(",").map((item) => item.trim()).filter(Boolean).forEach((value) => params.append("topKs", value));
  state.profileTopTagCounts.split(",").map((item) => item.trim()).filter(Boolean).forEach((value) => params.append("profileTopTagCounts", value));
  state.rerankWeightScales.split(",").map((item) => item.trim()).filter(Boolean).forEach((value) => params.append("rerankWeightScales", value));

  const result = await apiFetch(`/api/admin/evaluation/scenarios/export?${params.toString()}`, { method: "POST" });
  renderExportList([
    { title: "场景矩阵", content: result.fileName },
    { title: "场景模式", content: safeArray(result.scenarioModes).join(", ") || "-" },
    { title: "Top K 集合", content: safeArray(result.topKValues).join(", ") || "-" },
    { title: "画像 Top 标签数", content: safeArray(result.profileTopTagCounts).join(", ") || "-" },
    { title: "重排权重缩放", content: safeArray(result.rerankWeightScales).join(", ") || "-" }
  ]);
  addLog(`已导出场景矩阵：${safeArray(result.scenarioModes).join(", ")}。`);
}

async function reloadAll() {
  await loadStory();
  await loadOverview();
  await loadProfile();
  await loadComparison();
}

function safeAction(fn) {
  return async () => {
    try {
      setStatus("执行中...");
      await fn();
      setStatus("最近一次操作已完成");
    } catch (error) {
      console.error(error);
      setStatus(`操作失败：${error.message}`, true);
      addLog(`操作失败：${error.message}`);
    }
  };
}

function bindEvents() {
  document.getElementById("refresh-overview").addEventListener("click", safeAction(reloadAll));
  document.getElementById("init-mock-data").addEventListener("click", safeAction(runMockInit));
  document.getElementById("rebuild-profiles").addEventListener("click", safeAction(rebuildProfiles));
  document.getElementById("rebuild-recall-index").addEventListener("click", safeAction(rebuildRecallIndex));
  document.getElementById("export-evaluation").addEventListener("click", safeAction(exportEvaluation));
  document.getElementById("export-matrix").addEventListener("click", safeAction(exportMatrix));
  document.getElementById("load-user").addEventListener("click", safeAction(async () => {
    await loadProfile();
    await loadComparison();
  }));
  document.getElementById("run-recommendation").addEventListener("click", safeAction(async () => {
    await loadRecommendation();
    await loadComparison();
  }));

  els.userId.addEventListener("input", () => {
    state.userId = Number(els.userId.value || 0);
    els.heroUserId.textContent = state.userId || "-";
    els.heroUserDisplay.textContent = `用户 ${state.userId || "-"}`;
  });
  els.topK.addEventListener("input", () => {
    state.topK = Number(els.topK.value || 3);
  });
  els.scenarioMode.addEventListener("change", safeAction(async () => {
    state.scenarioMode = els.scenarioMode.value;
    await reloadAll();
  }));
  els.scenarioModes.addEventListener("input", () => {
    state.scenarioModes = els.scenarioModes.value;
  });
  els.matrixTopKs.addEventListener("input", () => {
    state.matrixTopKs = els.matrixTopKs.value;
  });
  els.profileTopTagCounts.addEventListener("input", () => {
    state.profileTopTagCounts = els.profileTopTagCounts.value;
  });
  els.rerankWeightScales.addEventListener("input", () => {
    state.rerankWeightScales = els.rerankWeightScales.value;
  });
  els.presentationModeToggle.addEventListener("change", () => {
    state.presentationMode = els.presentationModeToggle.checked ? "defense" : "debug";
    applyPresentationMode();
    addLog(state.presentationMode === "defense" ? "已切换到答辩模式。" : "已切换到调试模式。");
  });
}

async function bootstrap() {
  bindEvents();
  applyPresentationMode();
  renderFeedbackDiff();
  setStatus("页面已加载，正在获取故事线、评估摘要、用户画像和双视图对比。");
  await reloadAll();
  addLog("产品首页与演示侧栏已就绪。");
}

bootstrap().catch((error) => {
  console.error(error);
  setStatus(`初始化失败：${error.message}`, true);
});
