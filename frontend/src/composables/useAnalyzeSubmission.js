export function useAnalyzeSubmission({
  analyzeForm,
  analyzeLoading,
  analyzeMessage,
  result,
  chatState,
  chatMessage,
  jdRadar,
  radarMessage,
  resumeAudit,
  auditMessage,
  stopPolling,
  queueJob,
  resetQueueJobSnapshot,
  apiRequest,
  startPolling,
  toZhMessage
}) {
  function onFileChange(event) {
    const files = event?.target?.files || [];
    analyzeForm.file = files[0] || null;
  }

  function onJdImageChange(event) {
    const files = event?.target?.files || [];
    analyzeForm.jdImage = files[0] || null;
  }

  async function submitAnalyze() {
    if (!analyzeForm.file) {
      analyzeMessage.value = "请先选择简历文件";
      return;
    }

    analyzeLoading.value = true;
    analyzeMessage.value = "";

    // 提交新任务前，先重置所有依赖当前分析结果的面板状态。
    result.value = null;
    chatState.sessionId = null;
    chatState.messages = [];
    chatState.input = "";
    chatMessage.value = "";
    jdRadar.value = null;
    radarMessage.value = "";
    resumeAudit.value = null;
    auditMessage.value = "";
    stopPolling();
    resetQueueJobSnapshot(queueJob);

    try {
      const fd = new FormData();
      fd.append("file", analyzeForm.file);
      fd.append("jd_text", analyzeForm.jdText || "");
      fd.append("target_role", analyzeForm.targetRole || "");
      if (analyzeForm.jdImage) {
        fd.append("jd_image", analyzeForm.jdImage);
      }

      const data = await apiRequest("/analyze", {
        method: "POST",
        body: fd
      });

      queueJob.jobId = data.jobId;
      queueJob.status = data.status || "PENDING";
      queueJob.queuePosition = data.queuePosition ?? null;
      queueJob.vipPriority = !!data.vipPriority;
      analyzeMessage.value = `${toZhMessage(data.message || "queued")}，任务 ID：${data.jobId}`;
      startPolling(data.jobId);
    } catch (err) {
      analyzeMessage.value = toZhMessage(err?.message || "提交分析失败");
    } finally {
      analyzeLoading.value = false;
    }
  }

  return {
    onFileChange,
    onJdImageChange,
    submitAnalyze
  };
}
