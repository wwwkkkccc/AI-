import { readPageItems, readPageTotal } from "../utils/pageData";
import { isAbortError } from "../utils/requestAbort";

export function useDataPanels({
  token,
  isAdmin,
  minePage,
  mineSize,
  mineItems,
  mineTotal,
  mineMessage,
  adminUserKeyword,
  adminUserPage,
  adminUserSize,
  adminUsers,
  adminUserTotal,
  adminUsersMessage,
  adminUserLoadingId,
  requestAbort,
  apiRequest,
  toZhMessage
}) {
  async function loadMineAnalyses() {
    if (!token.value) return;
    try {
      const signal = requestAbort.nextSignal("mineAnalyses");
      const data = await apiRequest(`/analyses/mine?page=${minePage.value}&size=${mineSize.value}`, { signal });
      const items = readPageItems(data);
      mineItems.value = items;
      mineTotal.value = readPageTotal(data, items.length);
      mineMessage.value = "";
    } catch (err) {
      if (isAbortError(err)) return;
      mineMessage.value = toZhMessage(err?.message || "Failed to load my records");
    }
  }

  async function loadAdminUsers() {
    if (!token.value || !isAdmin.value) return;
    try {
      const keyword = encodeURIComponent(adminUserKeyword.value || "");
      const signal = requestAbort.nextSignal("adminUsers");
      const data = await apiRequest(`/admin/users?page=${adminUserPage.value}&size=${adminUserSize.value}&keyword=${keyword}`, { signal });
      const items = readPageItems(data);
      adminUsers.value = items;
      adminUserTotal.value = readPageTotal(data, items.length);
      adminUsersMessage.value = "";
    } catch (err) {
      if (isAbortError(err)) return;
      adminUsersMessage.value = toZhMessage(err?.message || "Failed to load users");
    }
  }

  async function updateUserFlags(userId, payload) {
    adminUserLoadingId.value = userId;
    adminUsersMessage.value = "";
    try {
      await apiRequest(`/admin/users/${userId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      await loadAdminUsers();
      adminUsersMessage.value = "User status updated";
    } catch (err) {
      adminUsersMessage.value = toZhMessage(err?.message || "Failed to update user status");
    } finally {
      adminUserLoadingId.value = null;
    }
  }

  async function toggleVip(user) {
    await updateUserFlags(user.id, { vip: !user.vip });
  }

  async function toggleBlacklist(user) {
    await updateUserFlags(user.id, { blacklisted: !user.blacklisted });
  }

  return {
    loadMineAnalyses,
    loadAdminUsers,
    toggleVip,
    toggleBlacklist
  };
}

