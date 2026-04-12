const state = {
  userId: 2001,
  topK: 3,
  matrixTopKs: "3,5",
  currentRecommendation: null
};

const els = {
  statusStrip: document.getElementById("status-strip"),
  logStream: document.getElementById("log-stream"),
  overviewGrid: document.getElementById("overview-grid"),
  overviewTimestamp: document.getElementById("overview-timestamp"),
  exportList: document.getElementById("export-list"),
  heroDataScale: document.getElementById("hero-data-scale"),
  heroUserId: document.getElementById("hero-user-id"),
  userId: document.getElementById("user-id"),
  topK: document.getElementById("top-k"),
  matrixTopKs: document.getElementById("matrix-topks"),
  profileTopTags: document.getElementById("profile-top-tags"),
  profileJson: document.getElementById("profile-json"),
  profileUpdatedAt: document.getElementById("profile-updated-at"),
  evaluationGeneratedAt: document.getElementById("evaluation-generated-at"),
  evaluationSummaryBody: document.getElementById("evaluation-summary-body"),
  recommendationList: document.getElementById("recommendation-list"),
  recommendationTrace: document.getElementById("recommendation-trace"),
  recommendationDetail: document.getElementById("recommendation-detail"),
  detailMeta: document.getElementById("detail-meta"),
  explanationDetail: document.getElementById("explanation-detail"),
  explanationMeta: document.getElementById("explanation-meta"),
  recommendationTemplate: document.getElementById("recommendation-card-template")
};

function nowLabel() {
  return new Date().toLocaleTimeString("en-GB", { hour12: false });
}

function addLog(message) {
  const entry = document.createElement("div");
  entry.className = "log-entry";
  entry.innerHTML = `
    <span class="log-time">${nowLabel()}</span>
    <span class="log-message">${message}</span>
  `;
  els.logStream.prepend(entry);
}

function setStatus(message, isError = false) {
  els.statusStrip.innerHTML = `
    <span class="status-dot" style="background:${isError ? "#bf4d28" : "var(--accent)"}"></span>
    <span>${message}</span>
  `;
}

async function apiFetch(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const payload = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    throw new Error(typeof payload === "string" ? payload : payload?.message || `HTTP ${response.status}`);
  }
  if (isJson && payload.success === false) {
    throw new Error(payload.message || "API returned failure");
  }
  return isJson ? payload.data : payload;
}

function renderOverview(summary) {
  const items = [
    ["Active users", summary.activeUserCount ?? "-"],
    ["Tags", summary.tagCount ?? "-"],
    ["Relations", summary.relationCount ?? "-"],
    ["Top K", summary.topK ?? "-"]
  ];
  els.overviewGrid.innerHTML = items.map(([label, value]) => `
    <div>
      <dt>${label}</dt>
      <dd>${value}</dd>
    </div>
  `).join("");
  els.overviewTimestamp.textContent = summary.generatedAt || "Unknown";
  els.heroDataScale.textContent = `${summary.activeUserCount ?? "-"} / ${summary.tagCount ?? "-"} / ${summary.relationCount ?? "-"}`;
}

function renderEvaluationSummary(summary) {
  els.evaluationGeneratedAt.textContent = summary.generatedAt || "Unknown";
  const baselines = summary.baselines || [];
  if (!baselines.length) {
    els.evaluationSummaryBody.innerHTML = `
      <tr>
        <td colspan="4">No baselines returned.</td>
      </tr>
    `;
    return;
  }
  els.evaluationSummaryBody.innerHTML = baselines.map((baseline) => `
    <tr>
      <td>${baseline.baselineName || baseline.baselineCode || "-"}</td>
      <td>${baseline.precisionAtK ?? "-"}</td>
      <td>${baseline.hitRateAtK ?? "-"}</td>
      <td>${baseline.explanationPresenceRate ?? "-"}</td>
    </tr>
  `).join("");
}

function renderProfile(profile) {
  els.profileUpdatedAt.textContent = profile.updatedAt || "Unknown";
  els.profileJson.textContent = JSON.stringify(profile, null, 2);
  const tags = profile.topkTags || [];
  els.profileTopTags.innerHTML = tags.length
    ? tags.map((tag) => `<span>${tag}</span>`).join("")
    : "<span>No top tags yet</span>";
}

function renderRecommendationDetail(detail) {
  els.recommendationTrace.textContent = `requestTraceId: ${detail.requestTraceId || "unknown"}`;
  els.detailMeta.textContent = `recallCandidatesCount: ${detail.recallCandidatesCount ?? detail.recallCandidateCount ?? 0}`;
  els.recommendationDetail.textContent = JSON.stringify(detail, null, 2);
}

function renderRecommendations(detail) {
  const items = detail.items || [];
  if (!items.length) {
    els.recommendationList.innerHTML = `
      <article class="recommendation-card empty-card">
        <p>No recommendation items were returned.</p>
      </article>
    `;
    return;
  }

  els.recommendationList.innerHTML = "";
  items.forEach((item) => {
    const fragment = els.recommendationTemplate.content.cloneNode(true);
    fragment.querySelector(".rank-chip").textContent = `Rank #${item.rankNo ?? "-"}`;
    fragment.querySelector(".score-chip").textContent = `Score ${item.finalScore ?? "-"}`;
    fragment.querySelector(".candidate-name").textContent = item.targetNickname || `Candidate ${item.targetUserId}`;
    fragment.querySelector(".candidate-meta").textContent = `targetUserId: ${item.targetUserId}`;
    fragment.querySelector(".candidate-explanation").textContent = item.explanation || item.reasonText || "No explanation yet.";

    fragment.querySelector(".explanation-button").addEventListener("click", () => loadExplanation(item.recommendationId));
    fragment.querySelector(".feedback-button").addEventListener("click", () => submitFeedback(item));

    els.recommendationList.appendChild(fragment);
  });
}

function renderExportList(entries) {
  els.exportList.innerHTML = entries.map((entry) => `
    <div class="export-item">
      <strong>${entry.title}</strong>
      <span>${entry.content}</span>
    </div>
  `).join("");
}

function buildExperimentQuery(value) {
  const searchParams = new URLSearchParams();
  value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((topK) => searchParams.append("topKs", topK));
  return searchParams.toString();
}

async function loadOverview() {
  const summary = await apiFetch(`/api/admin/evaluation/summary?topK=${state.topK}`);
  renderOverview(summary);
  renderEvaluationSummary(summary);
  addLog(`Loaded evaluation summary for topK=${state.topK}.`);
}

async function loadProfile() {
  let profile = await apiFetch(`/api/profiles/${state.userId}`);
  if (profile.profileJson === "{}") {
    profile = await apiFetch(`/api/profiles/${state.userId}/build`, { method: "POST" });
    addLog(`Profile for user ${state.userId} was empty. Built a fresh profile.`);
  } else {
    addLog(`Loaded profile for user ${state.userId}.`);
  }
  renderProfile(profile);
}

async function loadRecommendation() {
  const detail = await apiFetch(`/api/recommendations/${state.userId}?topK=${state.topK}&useCache=false`);
  state.currentRecommendation = detail;
  renderRecommendationDetail(detail);
  renderRecommendations(detail);
  addLog(`Generated ${detail.items?.length || 0} recommendation items for user ${state.userId}.`);
}

async function loadExplanation(recommendationId) {
  const explanation = await apiFetch(`/api/recommendations/${recommendationId}/explanation`);
  els.explanationMeta.textContent = `recommendationId: ${recommendationId}`;
  els.explanationDetail.textContent = JSON.stringify(explanation, null, 2);
  addLog(`Loaded explanation for recommendationId=${recommendationId}.`);
}

async function submitFeedback(item) {
  const payload = {
    recommendationId: item.recommendationId,
    targetUserId: item.targetUserId,
    feedbackType: "follow"
  };

  await apiFetch(`/api/recommendations/${state.userId}/feedback`, {
    method: "POST",
    body: JSON.stringify(payload)
  });

  addLog(`Submitted follow feedback for targetUserId=${item.targetUserId}.`);
  await loadProfile();
  await loadRecommendation();
}

async function runMockInit() {
  const summary = await apiFetch("/api/admin/mock/init", { method: "POST" });
  renderExportList([
    {
      title: "Initialization result",
      content: `users ${summary.userCount} / tags ${summary.tagCount} / relations ${summary.relationCount}`
    },
    {
      title: "Derived rebuilds",
      content: `profiles ${summary.profileRebuiltCount ?? "-"} / recall index ${summary.recallIndexCount ?? "-"}`
    }
  ]);
  addLog("Initialized mock data and rebuilt dependent profile and recall structures.");
  await loadOverview();
  await loadProfile();
}

async function rebuildProfiles() {
  const result = await apiFetch("/api/admin/profiles/rebuild-all", { method: "POST" });
  addLog(`Rebuilt all profiles. rebuildCount=${result.rebuildCount}`);
  await loadProfile();
}

async function rebuildRecallIndex() {
  const result = await apiFetch("/api/admin/recall/rebuild-index", { method: "POST" });
  addLog(`Rebuilt recall index. indexCount=${result.indexCount}`);
}

async function exportEvaluation() {
  const result = await apiFetch(`/api/admin/evaluation/export?topK=${state.topK}`, { method: "POST" });
  renderExportList([
    { title: "Evaluation snapshot", content: result.fileName },
    { title: "Output path", content: result.filePath }
  ]);
  addLog(`Exported latest evaluation snapshot to ${result.fileName}.`);
}

async function exportMatrix() {
  const query = buildExperimentQuery(state.matrixTopKs);
  const result = await apiFetch(`/api/admin/evaluation/experiments/export?${query}`, { method: "POST" });
  renderExportList([
    { title: "Evaluation matrix", content: result.fileName },
    { title: "Top K set", content: (result.topKValues || []).join(", ") || "-" }
  ]);
  addLog(`Exported evaluation matrix for topKs=${(result.topKValues || []).join(", ")}.`);
}

function safeAction(fn) {
  return async () => {
    try {
      setStatus("Running...");
      await fn();
      setStatus("Last action completed");
    } catch (error) {
      console.error(error);
      setStatus(`Action failed: ${error.message}`, true);
      addLog(`Action failed: ${error.message}`);
    }
  };
}

function bindEvents() {
  document.getElementById("refresh-overview").addEventListener("click", safeAction(loadOverview));
  document.getElementById("init-mock-data").addEventListener("click", safeAction(runMockInit));
  document.getElementById("rebuild-profiles").addEventListener("click", safeAction(rebuildProfiles));
  document.getElementById("rebuild-recall-index").addEventListener("click", safeAction(rebuildRecallIndex));
  document.getElementById("export-evaluation").addEventListener("click", safeAction(exportEvaluation));
  document.getElementById("export-matrix").addEventListener("click", safeAction(exportMatrix));
  document.getElementById("load-user").addEventListener("click", safeAction(loadProfile));
  document.getElementById("run-recommendation").addEventListener("click", safeAction(loadRecommendation));

  els.userId.addEventListener("input", () => {
    state.userId = Number(els.userId.value || 0);
    els.heroUserId.textContent = state.userId || "-";
  });
  els.topK.addEventListener("input", () => {
    state.topK = Number(els.topK.value || 3);
  });
  els.matrixTopKs.addEventListener("input", () => {
    state.matrixTopKs = els.matrixTopKs.value;
  });
}

async function bootstrap() {
  bindEvents();
  setStatus("Page loaded. Fetching overview and profile.");
  await loadOverview();
  await loadProfile();
  addLog("Frontend demo console is ready.");
}

bootstrap().catch((error) => {
  console.error(error);
  setStatus(`Bootstrap failed: ${error.message}`, true);
});
