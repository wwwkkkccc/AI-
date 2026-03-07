export function resetQueueJobSnapshot(queueJob) {
  queueJob.jobId = "";
  queueJob.status = "";
  queueJob.queuePosition = null;
  queueJob.vipPriority = false;
  queueJob.errorMessage = "";
  queueJob.createdAt = "";
  queueJob.startedAt = "";
  queueJob.finishedAt = "";
}

export function resetAfterLogout(ctx) {
  const {
    tokenKey,
    token,
    me,
    tab,
    queueJob,
    result,
    analyzeMessage,
    analyzeForm,
    resumeGenLoading,
    resumeGenMessage,
    resumeGenForm,
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
    mineItems,
    mineMessage,
    mineKeyword,
    minePage,
    mineTotal,
    adminUsers,
    adminUsersMessage,
    adminUserKeyword,
    adminUserPage,
    adminUserTotal
  } = ctx;

  resetQueueJobSnapshot(queueJob);

  result.value = null;
  analyzeMessage.value = "";
  analyzeForm.targetRole = "";
  analyzeForm.jdText = "";
  analyzeForm.file = null;
  analyzeForm.jdImage = null;

  resumeGenLoading.value = false;
  resumeGenMessage.value = "";
  resumeGenForm.targetRole = "";
  resumeGenForm.jdText = "";
  resumeGenForm.userBackground = "";
  generatedResume.mode = "";
  generatedResume.markdown = "";
  generatedResume.modelUsed = false;
  generatedResume.analysisId = null;

  chatLoading.value = false;
  chatMessage.value = "";
  chatState.sessionId = null;
  chatState.messages = [];
  chatState.input = "";

  radarLoading.value = false;
  radarMessage.value = "";
  jdRadar.value = null;

  auditLoading.value = false;
  auditMessage.value = "";
  resumeAudit.value = null;

  mineItems.value = [];
  mineMessage.value = "";
  mineKeyword.value = "";
  minePage.value = 0;
  mineTotal.value = 0;

  adminUsers.value = [];
  adminUsersMessage.value = "";
  adminUserKeyword.value = "";
  adminUserPage.value = 0;
  adminUserTotal.value = 0;

  token.value = "";
  me.id = null;
  me.username = "";
  me.role = "";
  me.vip = false;
  me.blacklisted = false;
  localStorage.removeItem(tokenKey);
  sessionStorage.removeItem(tokenKey);
  tab.value = "analyze";
}
