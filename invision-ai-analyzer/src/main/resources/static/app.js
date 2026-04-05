const form = document.getElementById("analysisForm");
const submitButton = document.getElementById("submitButton");
const fillDemoButton = document.getElementById("fillDemoButton");
const emptyState = document.getElementById("emptyState");
const results = document.getElementById("results");
const metaBar = document.getElementById("metaBar");
const historyGrid = document.getElementById("historyGrid");
const compareLeft = document.getElementById("compareLeft");
const compareRight = document.getElementById("compareRight");
const compareButton = document.getElementById("compareButton");
const comparePanel = document.getElementById("comparePanel");
const compareLeftCard = document.getElementById("compareLeftCard");
const compareRightCard = document.getElementById("compareRightCard");
const historySearch = document.getElementById("historySearch");
const historyFilter = document.getElementById("historyFilter");
const candidateDetailEmpty = document.getElementById("candidateDetailEmpty");
const candidateDetailBody = document.getElementById("candidateDetailBody");
const statTotal = document.getElementById("statTotal");
const statStrong = document.getElementById("statStrong");
const statInterview = document.getElementById("statInterview");
const statSaved = document.getElementById("statSaved");

const fields = {
    title: document.getElementById("candidateTitle"),
    headline: document.getElementById("headlineText"),
    overallScore: document.getElementById("overallScore"),
    readinessLabel: document.getElementById("readinessLabel"),
    roleMatchScore: document.getElementById("roleMatchScore"),
    deliveryScore: document.getElementById("deliveryScore"),
    communicationScore: document.getElementById("communicationScore"),
    growthScore: document.getElementById("growthScore"),
    summaryText: document.getElementById("summaryText"),
    badges: document.getElementById("badges"),
    strengthsList: document.getElementById("strengthsList"),
    risksList: document.getElementById("risksList"),
    recommendationsList: document.getElementById("recommendationsList"),
    questionsList: document.getElementById("questionsList"),
    previewScore: document.getElementById("previewScore")
};

const detailFields = {
    name: document.getElementById("detailName"),
    role: document.getElementById("detailRole"),
    score: document.getElementById("detailScore"),
    readiness: document.getElementById("detailReadiness"),
    tags: document.getElementById("detailTags"),
    note: document.getElementById("detailNote"),
    roleMatch: document.getElementById("detailRoleMatch"),
    delivery: document.getElementById("detailDelivery"),
    communication: document.getElementById("detailCommunication"),
    growth: document.getElementById("detailGrowth"),
    strengths: document.getElementById("detailStrengths"),
    risks: document.getElementById("detailRisks")
};

const STORAGE_KEY = "invision-ai-analyzer-history";
const analysisHistory = loadHistory();
let activeController = null;
let selectedCandidateKey = analysisHistory[0]?.key ?? null;

const demoProfile = {
    candidateName: "Aruzhan Sarsen",
    targetRole: "Java Backend Engineer",
    skills: "Java, Spring Boot, REST APIs, PostgreSQL, Docker, Kafka, CI/CD, Testing",
    experienceYears: 4,
    summary: "Built and shipped production backend services, improved API performance, owned integrations, worked closely with product and design, wrote integration tests, mentored juniors, and optimized release quality."
};

const starterCandidates = [
    {
        key: "Aruzhan Sarsen__Java Backend Engineer",
        saved: true,
        analysis: {
            candidateName: "Aruzhan Sarsen",
            targetRole: "Java Backend Engineer",
            overallScore: 91,
            readinessLabel: "Strong shortlist",
            roleMatchScore: 94,
            deliveryScore: 89,
            communicationScore: 81,
            growthScore: 84,
            headline: "Aruzhan looks like a high-confidence backend shortlist with strong production evidence.",
            summary: "Strong backend profile with clear Spring, API and delivery signals. Good shortlist candidate for the next stage.",
            strengths: [
                "Direct stack fit for Java backend work.",
                "Shows production ownership and testing habits.",
                "Can likely contribute quickly in an execution-heavy team."
            ],
            risks: [
                "Architecture depth should still be validated in interview.",
                "Need one strong system tradeoff example."
            ],
            recommendations: [],
            interviewQuestions: [],
            badges: [
                { label: "High hiring potential", tone: "success" },
                { label: "Role aligned", tone: "info" },
                { label: "Execution signal", tone: "accent" }
            ],
            meta: { processingTimeMs: 0, cached: true }
        }
    },
    {
        key: "Dana Kim__Frontend Engineer",
        saved: false,
        analysis: {
            candidateName: "Dana Kim",
            targetRole: "Frontend Engineer",
            overallScore: 78,
            readinessLabel: "Worth interview",
            roleMatchScore: 82,
            deliveryScore: 74,
            communicationScore: 79,
            growthScore: 72,
            headline: "Dana has a solid frontend fit and deserves an interview with a focus on depth.",
            summary: "Good frontend candidate with UI and collaboration signals. Worth interviewing for practical maintainability and scale questions.",
            strengths: [
                "Clear frontend stack alignment.",
                "Good communication and collaboration signals.",
                "Seems comfortable with shipping UI work."
            ],
            risks: [
                "Need clearer performance and architecture evidence.",
                "Testing depth is not obvious yet."
            ],
            recommendations: [],
            interviewQuestions: [],
            badges: [
                { label: "Role aligned", tone: "info" },
                { label: "Team-ready", tone: "warm" }
            ],
            meta: { processingTimeMs: 0, cached: true }
        }
    },
    {
        key: "Nursultan Bek__Flutter Developer",
        saved: false,
        analysis: {
            candidateName: "Nursultan Bek",
            targetRole: "Flutter Developer",
            overallScore: 69,
            readinessLabel: "Needs evidence",
            roleMatchScore: 76,
            deliveryScore: 63,
            communicationScore: 67,
            growthScore: 65,
            headline: "Nursultan has role potential, but the profile needs stronger shipped-product evidence.",
            summary: "The role fit is decent, but the candidate still needs more proof of delivery quality and project ownership.",
            strengths: [
                "Reasonable Flutter and mobile alignment.",
                "Potential for growth if supported well."
            ],
            risks: [
                "Weak delivery proof.",
                "No strong release quality signal yet.",
                "Needs more concrete project outcomes."
            ],
            recommendations: [],
            interviewQuestions: [],
            badges: [
                { label: "Growth mindset", tone: "neutral" }
            ],
            meta: { processingTimeMs: 0, cached: true }
        }
    }
];

fillDemoButton.addEventListener("click", () => {
    for (const [key, value] of Object.entries(demoProfile)) {
        form.elements[key].value = value;
    }
});

historySearch.addEventListener("input", renderHistory);
historyFilter.addEventListener("change", renderHistory);

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (activeController) {
        activeController.abort();
    }

    activeController = new AbortController();
    const timeoutId = window.setTimeout(() => activeController.abort(), 12000);
    setLoading(true);

    const payload = {
        candidateName: form.elements.candidateName.value.trim(),
        targetRole: form.elements.targetRole.value.trim(),
        skills: form.elements.skills.value.trim(),
        experienceYears: Number(form.elements.experienceYears.value),
        summary: form.elements.summary.value.trim()
    };

    try {
        const response = await fetch("/api/analysis", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload),
            signal: activeController.signal
        });

        if (!response.ok) {
            const errorPayload = await response.json().catch(() => ({}));
            throw new Error(errorPayload.detail || "Analysis request failed.");
        }

        const analysis = await response.json();
        renderAnalysis(analysis);
        saveToHistory(analysis);
    } catch (error) {
        window.alert(error.name === "AbortError"
            ? "The request took too long. The previous request was cancelled to keep the UI responsive."
            : error.message);
    } finally {
        window.clearTimeout(timeoutId);
        activeController = null;
        setLoading(false);
    }
});

compareButton.addEventListener("click", () => {
    const left = analysisHistory.find((item) => item.key === compareLeft.value);
    const right = analysisHistory.find((item) => item.key === compareRight.value);

    if (!left || !right) {
        window.alert("Analyze candidates first.");
        return;
    }

    if (left.key === right.key) {
        window.alert("Choose two different candidates to compare.");
        return;
    }

    compareLeftCard.innerHTML = renderCompareCard(left);
    compareRightCard.innerHTML = renderCompareCard(right);
    comparePanel.classList.remove("hidden");
});

seedStarterCandidates();
renderHistory();
renderSelectedCandidate();

function renderAnalysis(analysis) {
    emptyState.classList.add("hidden");
    results.classList.remove("hidden");

    fields.title.textContent = `${analysis.candidateName} · ${analysis.targetRole}`;
    fields.headline.textContent = analysis.headline;
    fields.overallScore.textContent = analysis.overallScore;
    fields.readinessLabel.textContent = analysis.readinessLabel;
    fields.roleMatchScore.textContent = analysis.roleMatchScore;
    fields.deliveryScore.textContent = analysis.deliveryScore;
    fields.communicationScore.textContent = analysis.communicationScore;
    fields.growthScore.textContent = analysis.growthScore;
    fields.summaryText.textContent = analysis.summary;
    fields.previewScore.textContent = analysis.overallScore;
    metaBar.textContent = `Processed in ${analysis.meta?.processingTimeMs ?? 0} ms${analysis.meta?.cached ? " · served from cache" : " · fresh analysis"}`;

    renderBadges(analysis.badges || []);
    renderList(fields.strengthsList, analysis.strengths || []);
    renderList(fields.risksList, analysis.risks || []);
    renderList(fields.recommendationsList, analysis.recommendations || []);
    renderList(fields.questionsList, analysis.interviewQuestions || []);
}

function renderBadges(badges) {
    fields.badges.innerHTML = "";
    badges.forEach((badge) => {
        const badgeElement = document.createElement("span");
        badgeElement.className = `badge-${badge.tone}`;
        badgeElement.textContent = badge.label;
        fields.badges.appendChild(badgeElement);
    });
}

function renderList(listElement, items) {
    listElement.innerHTML = "";
    items.forEach((item) => {
        const li = document.createElement("li");
        li.textContent = item;
        listElement.appendChild(li);
    });
}

function setLoading(isLoading) {
    submitButton.disabled = isLoading;
    fillDemoButton.disabled = isLoading;
    submitButton.textContent = isLoading ? "Analyzing..." : "Analyze candidate";
}

function saveToHistory(analysis) {
    const key = `${analysis.candidateName}__${analysis.targetRole}`;
    const existingIndex = analysisHistory.findIndex((item) => item.key === key);
    const saved = existingIndex >= 0 ? analysisHistory[existingIndex].saved : false;
    const entry = { key, analysis, saved };

    if (existingIndex >= 0) {
        analysisHistory.splice(existingIndex, 1);
    }
    analysisHistory.unshift(entry);
    analysisHistory.splice(10);
    selectedCandidateKey = key;

    persistHistory();
    renderHistory();
    renderSelectedCandidate();
}

function renderHistory() {
    const visibleItems = getFilteredHistory();
    renderStats();

    if (!visibleItems.length) {
        historyGrid.innerHTML = '<div class="history-empty">No candidates match the current filters.</div>';
        renderCompareOptions([]);
        return;
    }

    historyGrid.innerHTML = "";
    visibleItems.forEach((entry) => {
        const card = document.createElement("article");
        card.className = `history-card${entry.saved ? " saved" : ""}`;
        card.innerHTML = `
            <div class="history-card-top">
                <div>
                    <h3>${escapeHtml(entry.analysis.candidateName)}</h3>
                    <p>${escapeHtml(entry.analysis.targetRole)}</p>
                </div>
                <span class="history-status">${entry.saved ? "Saved shortlist" : escapeHtml(entry.analysis.readinessLabel)}</span>
            </div>
            <strong>${entry.analysis.overallScore}</strong>
            <p>${escapeHtml(entry.analysis.headline)}</p>
            <div class="history-card-actions">
                <button type="button" class="secondary-button" data-action="open" data-key="${escapeAttribute(entry.key)}">Open details</button>
                <button type="button" class="secondary-button" data-action="toggle-save" data-key="${escapeAttribute(entry.key)}">${entry.saved ? "Remove save" : "Save shortlist"}</button>
            </div>
        `;
        historyGrid.appendChild(card);
    });

    historyGrid.querySelectorAll("button").forEach((button) => {
        button.addEventListener("click", () => handleHistoryAction(button.dataset.action, button.dataset.key));
    });

    renderCompareOptions(visibleItems);
}

function handleHistoryAction(action, key) {
    const entry = analysisHistory.find((item) => item.key === key);
    if (!entry) {
        return;
    }

    if (action === "open") {
        selectedCandidateKey = key;
        renderSelectedCandidate();
        return;
    }

    if (action === "toggle-save") {
        entry.saved = !entry.saved;
        persistHistory();
        renderHistory();
        renderSelectedCandidate();
    }
}

function renderSelectedCandidate() {
    const entry = analysisHistory.find((item) => item.key === selectedCandidateKey);
    if (!entry) {
        candidateDetailEmpty.classList.remove("hidden");
        candidateDetailBody.classList.add("hidden");
        return;
    }

    candidateDetailEmpty.classList.add("hidden");
    candidateDetailBody.classList.remove("hidden");

    detailFields.name.textContent = entry.analysis.candidateName;
    detailFields.role.textContent = entry.analysis.targetRole;
    detailFields.score.textContent = entry.analysis.overallScore;
    detailFields.readiness.textContent = entry.saved ? "Saved shortlist" : entry.analysis.readinessLabel;
    detailFields.note.textContent = entry.analysis.summary;
    detailFields.roleMatch.textContent = entry.analysis.roleMatchScore;
    detailFields.delivery.textContent = entry.analysis.deliveryScore;
    detailFields.communication.textContent = entry.analysis.communicationScore;
    detailFields.growth.textContent = entry.analysis.growthScore;

    detailFields.tags.innerHTML = "";
    (entry.analysis.badges || []).forEach((badge) => {
        const tag = document.createElement("span");
        tag.textContent = badge.label;
        detailFields.tags.appendChild(tag);
    });

    renderList(detailFields.strengths, (entry.analysis.strengths || []).slice(0, 3));
    renderList(detailFields.risks, (entry.analysis.risks || []).slice(0, 3));
}

function renderStats() {
    statTotal.textContent = analysisHistory.length;
    statStrong.textContent = analysisHistory.filter((item) => item.analysis.readinessLabel === "Strong shortlist").length;
    statInterview.textContent = analysisHistory.filter((item) => item.analysis.readinessLabel === "Worth interview").length;
    statSaved.textContent = analysisHistory.filter((item) => item.saved).length;
}

function renderCompareOptions(items) {
    const sourceItems = items.length ? items : analysisHistory;
    const optionsMarkup = sourceItems.map((entry) =>
        `<option value="${escapeAttribute(entry.key)}">${escapeHtml(entry.analysis.candidateName)} · ${escapeHtml(entry.analysis.targetRole)}</option>`
    ).join("");

    compareLeft.innerHTML = optionsMarkup;
    compareRight.innerHTML = optionsMarkup;

    if (sourceItems.length > 1) {
        compareLeft.selectedIndex = 0;
        compareRight.selectedIndex = 1;
    } else if (sourceItems.length === 1) {
        compareLeft.selectedIndex = 0;
        compareRight.selectedIndex = 0;
    } else {
        comparePanel.classList.add("hidden");
    }
}

function renderCompareCard(entry) {
    const strengths = (entry.analysis.strengths || []).slice(0, 2).map((item) => `<li>${escapeHtml(item)}</li>`).join("");
    const risks = (entry.analysis.risks || []).slice(0, 2).map((item) => `<li>${escapeHtml(item)}</li>`).join("");
    return `
        <h3>${escapeHtml(entry.analysis.candidateName)}</h3>
        <p>${escapeHtml(entry.analysis.targetRole)}</p>
        <strong>${entry.analysis.overallScore}</strong>
        <p>${escapeHtml(entry.saved ? "Saved shortlist" : entry.analysis.readinessLabel)}</p>
        <p>Role match: ${entry.analysis.roleMatchScore} · Delivery: ${entry.analysis.deliveryScore} · Communication: ${entry.analysis.communicationScore} · Growth: ${entry.analysis.growthScore}</p>
        <ul>${strengths}${risks}</ul>
    `;
}

function getFilteredHistory() {
    const query = historySearch.value.trim().toLowerCase();
    const filter = historyFilter.value;

    return analysisHistory.filter((entry) => {
        const matchesQuery = !query
            || entry.analysis.candidateName.toLowerCase().includes(query)
            || entry.analysis.targetRole.toLowerCase().includes(query);
        const matchesFilter = filter === "all"
            || (filter === "saved" && entry.saved)
            || entry.analysis.readinessLabel === filter;
        return matchesQuery && matchesFilter;
    });
}

function loadHistory() {
    try {
        const raw = window.localStorage.getItem(STORAGE_KEY);
        if (!raw) {
            return [];
        }

        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
    } catch {
        return [];
    }
}

function persistHistory() {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(analysisHistory));
}

function seedStarterCandidates() {
    if (analysisHistory.length) {
        return;
    }

    starterCandidates.forEach((entry) => analysisHistory.push(entry));
    selectedCandidateKey = starterCandidates[0].key;
    persistHistory();
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function escapeAttribute(value) {
    return escapeHtml(value);
}
