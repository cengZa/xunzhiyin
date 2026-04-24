const pipelineState = {
  userId: 2001,
  topK: 3,
  scenarioMode: "interest_partner",
  presentationMode: "defense"
};

const pipelineEls = {
  userId: document.getElementById("pipeline-user-id"),
  topK: document.getElementById("pipeline-top-k"),
  scenarioMode: document.getElementById("pipeline-scenario-mode"),
  loadButton: document.getElementById("load-pipeline"),
  presentationModeToggle: document.getElementById("pipeline-presentation-mode-toggle"),
  userDisplay: document.getElementById("pipeline-user-display"),
  userMeta: document.getElementById("pipeline-user-meta"),
  scenarioLabel: document.getElementById("pipeline-scenario-label"),
  recallCount: document.getElementById("pipeline-recall-count"),
  status: document.getElementById("pipeline-status"),
  scenarioObjective: document.getElementById("pipeline-scenario-objective"),
  scenarioChanges: document.getElementById("pipeline-scenario-changes"),
  inputTags: document.getElementById("pipeline-input-tags"),
  inputRaw: document.getElementById("pipeline-input-raw"),
  profileBody: document.getElementById("pipeline-profile-body"),
  topTags: document.getElementById("pipeline-top-tags"),
  profileFormula: document.getElementById("pipeline-profile-formula"),
  profileRaw: document.getElementById("pipeline-profile-raw"),
  recallStage: document.getElementById("pipeline-recall-stage"),
  rankingStage: document.getElementById("pipeline-ranking-stage"),
  rerankStage: document.getElementById("pipeline-rerank-stage"),
  finalStage: document.getElementById("pipeline-final-stage"),
  explanationSource: document.getElementById("pipeline-explanation-source"),
  explanationPrimary: document.getElementById("pipeline-explanation-primary"),
  explanationRule: document.getElementById("pipeline-explanation-rule"),
  explanationLlm: document.getElementById("pipeline-explanation-llm"),
  explanationDetail: document.getElementById("pipeline-explanation-detail"),
  rawJson: document.getElementById("pipeline-raw-json")
};

function requireElement(key, elementId) {
  if (!pipelineEls[key]) {
    throw new Error(`透明链路页缺少必要节点：#${elementId}`);
  }
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

function createFactList(items) {
  const list = document.createElement("ul");
  list.className = "detail-list";
  items.forEach((text) => {
    const li = document.createElement("li");
    li.textContent = text;
    list.appendChild(li);
  });
  return list;
}

function createStageCard(titleText, summaryText, facts = [], chips = []) {
  const card = document.createElement("article");
  card.className = "simple-card stage-detail-card";

  const title = document.createElement("strong");
  title.textContent = titleText;
  card.appendChild(title);

  const summary = document.createElement("span");
  summary.className = "simple-card-text";
  summary.textContent = summaryText;
  card.appendChild(summary);

  if (facts.length) {
    card.appendChild(createFactList(facts));
  }

  if (chips.length) {
    const chipGroup = document.createElement("div");
    chipGroup.className = "chip-group";
    chips.forEach((chip) => chipGroup.appendChild(chip));
    card.appendChild(chipGroup);
  }

  return card;
}

async function apiFetch(path) {
  const response = await fetch(path);
  const payload = await response.json();
  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || `HTTP ${response.status}`);
  }
  return payload.data;
}

function applyPresentationMode() {
  document.body.classList.toggle("defense-mode", pipelineState.presentationMode === "defense");
}

function renderStageCards(container, items, factory) {
  clearNode(container);
  safeArray(items).forEach((item) => container.appendChild(factory(item)));
}

function renderScenarioStage(pipeline) {
  pipelineEls.scenarioObjective.textContent = formatValue(
    pipeline.scenarioStage?.objective,
    "未提供当前场景说明。"
  );
  clearNode(pipelineEls.scenarioChanges);
  safeArray(pipeline.scenarioStage?.modeChanges).forEach((change) => {
    pipelineEls.scenarioChanges.appendChild(createChip(change, "chip-accent"));
  });
}

function renderInputStage(pipeline) {
  clearNode(pipelineEls.inputTags);
  safeArray(pipeline.inputTags).forEach((tag) => {
    pipelineEls.inputTags.appendChild(
      createChip(`${formatValue(tag.tagName)} / ${formatValue(tag.tagTypeLabel)}`, "chip-tag")
    );
  });
  pipelineEls.inputRaw.textContent = JSON.stringify(pipeline.inputTags, null, 2);
}

function renderProfileStage(pipeline) {
  pipelineEls.profileFormula.textContent = formatValue(
    pipeline.profileStage?.weightFormula,
    "未提供画像权重公式。"
  );
  pipelineEls.profileBody.innerHTML = safeArray(pipeline.profileStage?.tagWeights)
    .map(
      (item) => `
      <tr>
        <td>${formatValue(item.tagName)}</td>
        <td>${formatValue(item.tagTypeLabel)}</td>
        <td>${formatScore(item.tf)}</td>
        <td>${formatScore(item.idf)}</td>
        <td>${formatScore(item.timeDecay)}</td>
        <td>${formatScore(item.finalWeight)}</td>
        <td>${formatValue(item.formulaText)}</td>
      </tr>
    `
    )
    .join("");

  clearNode(pipelineEls.topTags);
  safeArray(pipeline.profileStage?.topKTags).forEach((item) => {
    pipelineEls.topTags.appendChild(
      createChip(
        `${formatValue(item.tagName)} / ${formatValue(item.tagTypeLabel)} / ${formatScore(item.finalWeight)}`,
        "chip-tag"
      )
    );
  });
  pipelineEls.profileRaw.textContent = JSON.stringify(pipeline.profileStage, null, 2);
}

function renderRecallStage(pipeline) {
  renderStageCards(pipelineEls.recallStage, pipeline.recallStage, (item) => {
    const facts = [
      `召回分：${formatScore(item.recallScore)}`,
      `公式：${formatValue(item.recallFormula)}`,
      `命中召回标签：${safeArray(item.matchedRecallTags).join("、") || "-"}`,
      `候选人专业：${formatValue(item.major)}`
    ];
    const chips = safeArray(item.recallTrace).map((trace) =>
      createChip(
        `${formatValue(trace.tagName)} / ${formatValue(trace.tagTypeLabel)} / ${formatValue(trace.recallSource)}`,
        "chip-tag"
      )
    );
    return createStageCard(
      `${formatValue(item.targetNickname)} / ${formatValue(item.targetUserId)}`,
      "当前召回只看请求用户 Top-K 标签是否命中该候选人，不做额外相似度加权。",
      facts,
      chips
    );
  });
}

function renderRankingStage(pipeline) {
  renderStageCards(pipelineEls.rankingStage, pipeline.rankingStage, (item) => {
    const detail = item.rankingDetail || {};
    const facts = [
      `排序分：${formatScore(item.rankScore)}`,
      `兴趣分：${formatScore(item.interestScore)}`,
      `公式：${formatValue(item.rankingFormula)}`,
      `dotProduct：${formatScore(detail.dotProduct)}`,
      `请求向量范数：${formatScore(detail.requestNorm)}`,
      `候选向量范数：${formatScore(detail.candidateNorm)}`,
      `重叠标签数：${formatValue(detail.overlapCount)}`
    ];
    const chips = safeArray(item.matchedTags).map((tag) => createChip(tag, "chip-tag"));
    return createStageCard(
      `${formatValue(item.targetNickname)} / ${formatValue(item.targetUserId)}`,
      "排序阶段用余弦相似度比较两个画像向量；当前页把 interestScore 直接展示为排序分。",
      facts,
      chips
    );
  });
}

function renderRerankStage(pipeline) {
  renderStageCards(pipelineEls.rerankStage, pipeline.rerankStage, (item) => {
    const trustBreakdown = item.trustBreakdown || {};
    const facts = [
      `兴趣分：${formatScore(item.interestScore)}`,
      `场景分：${formatScore(item.campusScore)}`,
      `可信分：${formatScore(item.trustScore)}`,
      `最终分：${formatScore(item.finalScore)}`,
      `最终公式：${formatValue(item.finalScoreFormulaLabel)}`,
      `资料完整度分：${formatScore(trustBreakdown.profileScore)}`,
      `标签丰富度分：${formatScore(trustBreakdown.tagScore)}`,
      `历史关注分：${formatScore(trustBreakdown.followScore)}`,
      `历史关注次数：${formatValue(trustBreakdown.followCount, 0)}`
    ];
    const chips = [];
    safeArray(item.ruleDetails).forEach((rule) => {
      chips.push(
        createChip(
          `${formatValue(rule.ruleDesc)} / ${formatScore(rule.baseScore)} × ${formatScore(rule.scenarioMultiplier)} × ${formatScore(
            rule.rerankWeightScale
          )} = ${formatScore(rule.weightedContribution)}`,
          "chip-rule"
        )
      );
    });
    safeArray(item.trustReasons).forEach((reason) => chips.push(createChip(reason, "chip-accent")));
    safeArray(trustBreakdown.reasonThresholds).forEach((threshold) =>
      chips.push(createChip(threshold, "chip-muted"))
    );
    if (item.exploration) {
      chips.push(createChip(`轻量探索位 / ${formatValue(item.explorationReason)}`, "chip-rule"));
    }
    return createStageCard(
      `${formatValue(item.targetNickname)} / ${formatValue(item.targetUserId)}`,
      "重排阶段会把场景规则、可信连接分和轻量探索叠加到排序分上。",
      facts,
      chips
    );
  });
}

function renderSelectedExplanation(explanation, recommendationId) {
  const reasonSource = explanation.reasonSource === "llm" ? "LLM 改写解释" : "规则解释";
  pipelineEls.explanationSource.textContent = `recommendationId：${recommendationId} · ${reasonSource}`;
  pipelineEls.explanationPrimary.textContent = formatValue(explanation.reasonText, "暂无解释");
  pipelineEls.explanationRule.textContent = formatValue(
    explanation.ruleReasonText || explanation.reasonText,
    "暂无规则解释"
  );
  pipelineEls.explanationLlm.textContent = explanation.reasonSource === "llm"
    ? formatValue(explanation.llmReasonText || explanation.reasonText, "暂无 LLM 改写")
    : "当前未启用 LLM 改写，此处展示规则解释回退结果。";
  pipelineEls.explanationDetail.textContent = JSON.stringify(explanation, null, 2);
}

async function loadPipelineExplanation(recommendationId) {
  const explanation = await apiFetch(`/api/recommendations/${recommendationId}/explanation`);
  renderSelectedExplanation(explanation, recommendationId);
}

function renderFinalStage(pipeline) {
  clearNode(pipelineEls.finalStage);
  const finalItems = safeArray(pipeline.finalStage);
  finalItems.forEach((item) => {
    const article = document.createElement("article");
    article.className = "candidate-card";
    article.innerHTML = `
      <div class="candidate-topline">
        <span class="rank-chip">排名 #${formatValue(item.rankNo)}</span>
        <span class="label-chip">${item.exploration ? "轻量探索位" : formatValue(item.targetNickname)}</span>
      </div>
      <h3 class="candidate-name">${formatValue(item.targetNickname)} / ${formatValue(item.targetUserId)}</h3>
      <p class="candidate-meta">${formatValue(item.major)} / ${formatValue(item.college)} / ${formatValue(
        item.grade,
        "-"
      )} 年级</p>
      <div class="score-board">
        <div><span>召回分</span><strong>${formatScore(item.recallScore)}</strong></div>
        <div><span>兴趣分</span><strong>${formatScore(item.interestScore)}</strong></div>
        <div><span>场景分</span><strong>${formatScore(item.campusScore)}</strong></div>
        <div><span>最终分</span><strong>${formatScore(item.finalScore)}</strong></div>
      </div>
      <p class="candidate-explanation">${formatValue(item.reasonText, "暂无解释")}</p>
    `;

    const chipGroup = document.createElement("div");
    chipGroup.className = "chip-group";
    safeArray(item.matchedTags).forEach((tag) => chipGroup.appendChild(createChip(tag, "chip-tag")));
    safeArray(item.ruleHits).forEach((rule) => chipGroup.appendChild(createChip(rule.ruleDesc, "chip-rule")));
    safeArray(item.trustReasons).forEach((reason) => chipGroup.appendChild(createChip(reason, "chip-accent")));
    if (item.exploration) {
      chipGroup.appendChild(createChip("轻量探索位", "chip-rule"));
      chipGroup.appendChild(createChip(formatValue(item.explorationReason), "chip-accent"));
    }
    article.appendChild(chipGroup);

    const actions = document.createElement("div");
    actions.className = "button-row";
    const explanationButton = document.createElement("button");
    explanationButton.className = "ghost-button";
    explanationButton.type = "button";
    explanationButton.textContent = "查看解释对照";
    explanationButton.addEventListener("click", async () => {
      try {
        pipelineEls.status.textContent = "正在加载解释对照...";
        await loadPipelineExplanation(item.recommendationId);
        pipelineEls.status.textContent = "透明链路已加载";
      } catch (error) {
        pipelineEls.status.textContent = `解释加载失败：${error.message}`;
      }
    });
    actions.appendChild(explanationButton);
    article.appendChild(actions);

    pipelineEls.finalStage.appendChild(article);
  });

  if (finalItems.length && finalItems[0].recommendationId) {
    loadPipelineExplanation(finalItems[0].recommendationId).catch(() => {
      pipelineEls.explanationSource.textContent = "默认解释加载失败";
    });
  } else {
    pipelineEls.explanationSource.textContent = "当前没有可展示的最终推荐";
  }
}

function renderPipeline(pipeline) {
  pipelineEls.userDisplay.textContent = `${formatValue(pipeline.requestUser?.nickname, "用户")} / ${pipeline.userId}`;
  pipelineEls.userMeta.textContent = `${formatValue(pipeline.requestUser?.major)} / ${formatValue(
    pipeline.requestUser?.college
  )} / ${formatValue(pipeline.requestUser?.grade, "-")} 年级`;
  pipelineEls.scenarioLabel.textContent = formatValue(pipeline.scenarioLabel);
  pipelineEls.recallCount.textContent = `召回候选池：${formatValue(pipeline.recallCandidateCount, 0)}`;
  pipelineEls.status.textContent = "透明链路已加载";

  renderScenarioStage(pipeline);
  renderInputStage(pipeline);
  renderProfileStage(pipeline);
  renderRecallStage(pipeline);
  renderRankingStage(pipeline);
  renderRerankStage(pipeline);
  renderFinalStage(pipeline);
  pipelineEls.rawJson.textContent = JSON.stringify(pipeline, null, 2);
}

async function loadPipeline() {
  pipelineEls.status.textContent = "加载中...";
  const params = new URLSearchParams({
    userId: String(pipelineState.userId),
    topK: String(pipelineState.topK),
    scenarioMode: pipelineState.scenarioMode
  });
  const pipeline = await apiFetch(`/api/admin/demo/pipeline?${params.toString()}`);
  renderPipeline(pipeline);
}

function bindEvents() {
  requireElement("userId", "pipeline-user-id");
  requireElement("topK", "pipeline-top-k");
  requireElement("scenarioMode", "pipeline-scenario-mode");
  requireElement("loadButton", "load-pipeline");
  requireElement("status", "pipeline-status");
  requireElement("presentationModeToggle", "pipeline-presentation-mode-toggle");

  pipelineEls.userId.addEventListener("input", () => {
    pipelineState.userId = Number(pipelineEls.userId.value || 0);
  });
  pipelineEls.topK.addEventListener("input", () => {
    pipelineState.topK = Number(pipelineEls.topK.value || 3);
  });
  pipelineEls.scenarioMode.addEventListener("change", () => {
    pipelineState.scenarioMode = pipelineEls.scenarioMode.value;
  });
  pipelineEls.presentationModeToggle.addEventListener("change", () => {
    pipelineState.presentationMode = pipelineEls.presentationModeToggle.checked ? "defense" : "debug";
    applyPresentationMode();
  });
  pipelineEls.loadButton.addEventListener("click", async () => {
    try {
      await loadPipeline();
    } catch (error) {
      pipelineEls.status.textContent = `加载失败：${error.message}`;
    }
  });
}

function applyQuery() {
  const query = new URLSearchParams(window.location.search);
  pipelineState.userId = Number(query.get("userId") || pipelineState.userId);
  pipelineState.topK = Number(query.get("topK") || pipelineState.topK);
  pipelineState.scenarioMode = query.get("scenarioMode") || pipelineState.scenarioMode;
  pipelineEls.userId.value = String(pipelineState.userId);
  pipelineEls.topK.value = String(pipelineState.topK);
  pipelineEls.scenarioMode.value = pipelineState.scenarioMode;
}

async function bootstrap() {
  bindEvents();
  applyQuery();
  applyPresentationMode();
  await loadPipeline();
}

bootstrap().catch((error) => {
  pipelineEls.status.textContent = `初始化失败：${error.message}`;
});
