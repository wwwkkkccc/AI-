export function useQueuePolling({
  apiRequest,
  queueJob,
  analyzeMessage,
  result,
  loadMineAnalyses,
  toZhMessage
}) {
  let pollTimer = null;
  // Adaptive delay reduces request pressure while users wait in queue.
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
        const posText = queueJob.queuePosition ? `, queue position: ${queueJob.queuePosition}` : "";
        analyzeMessage.value = `Job submitted and queued${posText}`;
        pollDelayMs = Math.min(5000, pollDelayMs + 400);
        scheduleNextPoll(jobId);
        return;
      }

      if (queueJob.status === "PROCESSING") {
        analyzeMessage.value = "Job is processing...";
        pollDelayMs = 1400;
        scheduleNextPoll(jobId);
        return;
      }

      if (queueJob.status === "DONE") {
        stopPolling();
        result.value = statusData.result || null;
        analyzeMessage.value = `Analysis completed, Record ID: ${statusData.result?.analysisId || "-"}`;
        await loadMineAnalyses();
        return;
      }

      if (queueJob.status === "FAILED") {
        stopPolling();
        analyzeMessage.value = `Analysis failed: ${toZhMessage(queueJob.errorMessage || "unknown error")}`;
      }
    } catch (err) {
      stopPolling();
      analyzeMessage.value = toZhMessage(err?.message || "failed to get job status");
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
