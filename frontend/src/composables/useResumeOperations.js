export function useResumeOperations({
  result,
  resumeGenForm,
  resumeGenLoading,
  resumeGenMessage,
  generatedResume,
  chatLoading,
  chatMessage,
  chatState,
  radarLoading,
  radarMessage,
  jdRadar,
  auditLoading,
  auditMessage,
  resumeAudit,
  analyzeForm,
  apiRequest,
  toZhMessage,
  copyText,
  downloadText
}) {
  async function generateResumeFromJd() {
    if (!resumeGenForm.targetRole || !resumeGenForm.jdText) {
      resumeGenMessage.value = "Please provide target role and JD text";
      return;
    }
    resumeGenLoading.value = true;
    resumeGenMessage.value = "";
    try {
      const data = await apiRequest("/resume/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          targetRole: resumeGenForm.targetRole,
          jdText: resumeGenForm.jdText,
          userBackground: resumeGenForm.userBackground
        })
      });
      generatedResume.mode = data.mode || "";
      generatedResume.markdown = data.markdown || "";
      generatedResume.modelUsed = !!data.modelUsed;
      generatedResume.analysisId = data.analysisId ?? null;
      resumeGenMessage.value = generatedResume.modelUsed ? "Resume generated (LLM)" : "Resume generated (fallback)";
    } catch (err) {
      resumeGenMessage.value = toZhMessage(err?.message || "Resume generation failed");
    } finally {
      resumeGenLoading.value = false;
    }
  }

  async function rewriteResumeFromCurrentAnalysis() {
    const analysisId = result.value?.analysisId;
    if (!analysisId) {
      resumeGenMessage.value = "No analysis record available for rewrite";
      return;
    }
    resumeGenLoading.value = true;
    resumeGenMessage.value = "";
    try {
      const data = await apiRequest("/resume/rewrite", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ analysisId })
      });
      generatedResume.mode = data.mode || "";
      generatedResume.markdown = data.markdown || "";
      generatedResume.modelUsed = !!data.modelUsed;
      generatedResume.analysisId = data.analysisId ?? null;
      resumeGenMessage.value = generatedResume.modelUsed ? "Resume rewritten (LLM)" : "Resume rewritten (fallback)";
    } catch (err) {
      resumeGenMessage.value = toZhMessage(err?.message || "Resume rewrite failed");
    } finally {
      resumeGenLoading.value = false;
    }
  }

  async function copyGeneratedResume() {
    await copyText(generatedResume.markdown);
    resumeGenMessage.value = "Copied to clipboard";
  }

  function downloadGeneratedResume() {
    downloadText("generated-resume.md", generatedResume.markdown);
  }

  async function startResumeChat() {
    const analysisId = result.value?.analysisId;
    if (!analysisId) {
      chatMessage.value = "Please complete analysis first";
      return;
    }
    chatLoading.value = true;
    chatMessage.value = "";
    try {
      const data = await apiRequest("/chat/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ analysisId })
      });
      chatState.sessionId = data.sessionId;
      chatState.messages = data.messages || [];
      chatState.input = "";
      chatMessage.value = `Chat session started: ${data.sessionId}`;
    } catch (err) {
      chatMessage.value = toZhMessage(err?.message || "Failed to start chat");
    } finally {
      chatLoading.value = false;
    }
  }

  async function sendChatMessage() {
    if (!chatState.sessionId) {
      chatMessage.value = "Please start a chat session first";
      return;
    }
    if (!chatState.input) return;

    const ask = chatState.input;
    chatLoading.value = true;
    chatMessage.value = "";
    try {
      const data = await apiRequest(`/chat/${chatState.sessionId}/message`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: ask })
      });
      chatState.messages = data.messages || [];
      chatState.input = "";
    } catch (err) {
      chatMessage.value = toZhMessage(err?.message || "Failed to send chat message");
    } finally {
      chatLoading.value = false;
    }
  }

  async function quickAsk(text) {
    chatState.input = text;
    await sendChatMessage();
  }

  async function runJdRadarFromCurrent() {
    const analysisId = result.value?.analysisId;
    if (!analysisId) {
      radarMessage.value = "Please complete analysis first";
      return;
    }
    radarLoading.value = true;
    radarMessage.value = "";
    try {
      jdRadar.value = await apiRequest("/jd/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ analysisId })
      });
      radarMessage.value = "JD radar generated";
    } catch (err) {
      radarMessage.value = toZhMessage(err?.message || "JD radar failed");
    } finally {
      radarLoading.value = false;
    }
  }

  async function runAuditFromCurrent() {
    const analysisId = result.value?.analysisId;
    if (!analysisId) {
      auditMessage.value = "Please complete analysis first";
      return;
    }
    auditLoading.value = true;
    auditMessage.value = "";
    try {
      resumeAudit.value = await apiRequest("/resume/audit", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ analysisId, targetRole: analyzeForm.targetRole || "" })
      });
      auditMessage.value = "Authenticity audit completed";
    } catch (err) {
      auditMessage.value = toZhMessage(err?.message || "Authenticity audit failed");
    } finally {
      auditLoading.value = false;
    }
  }

  return {
    generateResumeFromJd,
    rewriteResumeFromCurrentAnalysis,
    copyGeneratedResume,
    downloadGeneratedResume,
    startResumeChat,
    sendChatMessage,
    quickAsk,
    runJdRadarFromCurrent,
    runAuditFromCurrent
  };
}

