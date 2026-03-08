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
      resumeGenMessage.value = "请填写目标岗位和 JD 文本";
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
      resumeGenMessage.value = generatedResume.modelUsed ? "简历生成成功（LLM）" : "简历生成成功（降级）";
    } catch (err) {
      resumeGenMessage.value = toZhMessage(err?.message || "简历生成失败");
    } finally {
      resumeGenLoading.value = false;
    }
  }

  async function rewriteResumeFromCurrentAnalysis() {
    const analysisId = result.value?.analysisId;
    if (!analysisId) {
      resumeGenMessage.value = "没有可用于改写的分析记录";
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
      resumeGenMessage.value = generatedResume.modelUsed ? "简历改写成功（LLM）" : "简历改写成功（降级）";
    } catch (err) {
      resumeGenMessage.value = toZhMessage(err?.message || "简历改写失败");
    } finally {
      resumeGenLoading.value = false;
    }
  }

  async function copyGeneratedResume() {
    await copyText(generatedResume.markdown);
    resumeGenMessage.value = "已复制到剪贴板";
  }

  function downloadGeneratedResume() {
    downloadText("generated-resume.md", generatedResume.markdown);
  }

  async function startResumeChat() {
    const analysisId = result.value?.analysisId;
    if (!analysisId) {
      chatMessage.value = "请先完成分析";
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
      chatMessage.value = `会话已开始：${data.sessionId}`;
    } catch (err) {
      chatMessage.value = toZhMessage(err?.message || "开始会话失败");
    } finally {
      chatLoading.value = false;
    }
  }

  async function sendChatMessage() {
    if (!chatState.sessionId) {
      chatMessage.value = "请先开始会话";
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
      chatMessage.value = toZhMessage(err?.message || "发送会话消息失败");
    } finally {
      chatLoading.value = false;
    }
  }

  async function quickAsk(text) {
    // 复用发送逻辑，确保状态和错误处理一致。
    chatState.input = text;
    await sendChatMessage();
  }

  async function runJdRadarFromCurrent() {
    const analysisId = result.value?.analysisId;
    if (!analysisId) {
      radarMessage.value = "请先完成分析";
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
      radarMessage.value = "JD 雷达已生成";
    } catch (err) {
      radarMessage.value = toZhMessage(err?.message || "JD 雷达分析失败");
    } finally {
      radarLoading.value = false;
    }
  }

  async function runAuditFromCurrent() {
    const analysisId = result.value?.analysisId;
    if (!analysisId) {
      auditMessage.value = "请先完成分析";
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
      auditMessage.value = "真实性审计已完成";
    } catch (err) {
      auditMessage.value = toZhMessage(err?.message || "真实性审计失败");
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
