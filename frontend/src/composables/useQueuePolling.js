export function useQueuePolling({
  apiRequest,
  queueJob,
  analyzeMessage,
  result,
  loadMineAnalyses,
  toZhMessage
}) {
  let pollTimer = null;
  // 自适应轮询间隔：排队时逐步增加间隔，减少请求压力。
  let pollDelayMs = 1200;

  function stopPolling() {
    if (pollTimer) {
      clearTimeout(pollTimer);
      pollTimer = null;
    }
  }

  function scheduleNextPoll(jobId) {
    stopPolling();
    pollTimer = setTimeout(() => {
      pollJobStatus(jobId);
    }, pollDelayMs);
  }

  async function pollJobStatus(jobId) {
    if (!jobId) return;
    try {
      const statusData = await apiRequest(`/analyze/jobs/${jobId}`);
      queueJob.jobId = statusData.jobId || jobId;
      queueJob.status = statusData.status || "";
      queueJob.queuePosition = statusData.queuePosition ?? null;
      queueJob.errorMessage = statusData.errorMessage || "";
      queueJob.createdAt = statusData.createdAt || "";
      queueJob.startedAt = statusData.startedAt || "";
      queueJob.finishedAt = statusData.finishedAt || "";

      if (queueJob.status === "PENDING") {
        const posText = queueJob.queuePosition ? `，队列位置：${queueJob.queuePosition}` : "";
        analyzeMessage.value = `任务已提交并进入队列${posText}`;
        pollDelayMs = Math.min(5000, pollDelayMs + 400);
        scheduleNextPoll(jobId);
        return;
      }

      if (queueJob.status === "PROCESSING") {
        analyzeMessage.value = "任务分析中...";
        pollDelayMs = 1400;
        scheduleNextPoll(jobId);
        return;
      }

      if (queueJob.status === "DONE") {
        stopPolling();
        result.value = statusData.result || null;
        analyzeMessage.value = `分析完成，记录 ID：${statusData.result?.analysisId || "-"}`;
        await loadMineAnalyses();
        return;
      }

      if (queueJob.status === "FAILED") {
        stopPolling();
        analyzeMessage.value = `分析失败：${toZhMessage(queueJob.errorMessage || "未知错误")}`;
      }
    } catch (err) {
      stopPolling();
      analyzeMessage.value = toZhMessage(err?.message || "获取任务状态失败");
    }
  }

  function startPolling(jobId) {
    stopPolling();
    pollDelayMs = 1200;
    pollJobStatus(jobId);
  }

  async function refreshCurrentJob() {
    if (!queueJob.jobId) return;
    await pollJobStatus(queueJob.jobId);
  }

  return {
    stopPolling,
    startPolling,
    pollJobStatus,
    refreshCurrentJob
  };
}
