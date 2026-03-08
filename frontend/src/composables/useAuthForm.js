import { computed, reactive, ref } from "vue";
import { toZhMessage } from "../utils/errorMessages";

export function useAuthForm({ token, tokenKey, apiRequest, loadMe, onLoggedIn }) {
  const authLoading = ref(false);
  const authMessage = ref("");
  const authMode = ref("login");
  const rememberMe = ref(true);

  const loginForm = reactive({ username: "", password: "" });
  const registerForm = reactive({ username: "", password: "" });
  const activeAuthForm = computed(() => (authMode.value === "login" ? loginForm : registerForm));

  function persistToken(newToken) {
    // 根据“记住我”偏好把 token 写入 localStorage 或 sessionStorage。
    const nextToken = String(newToken || "");
    token.value = nextToken;
    localStorage.removeItem(tokenKey);
    sessionStorage.removeItem(tokenKey);
    if (!nextToken) return;
    if (rememberMe.value) {
      localStorage.setItem(tokenKey, nextToken);
    } else {
      sessionStorage.setItem(tokenKey, nextToken);
    }
  }

  function setAuthMode(mode) {
    authMode.value = mode === "register" ? "register" : "login";
    authMessage.value = "";
  }

  function toggleAuthMode() {
    setAuthMode(authMode.value === "login" ? "register" : "login");
  }

  async function submitLogin() {
    authLoading.value = true;
    authMessage.value = "";
    try {
      const data = await apiRequest("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(loginForm)
      });
      persistToken(data.token);
      await loadMe();
      authMessage.value = "登录成功";
      if (typeof onLoggedIn === "function") onLoggedIn();
    } catch (err) {
      authMessage.value = toZhMessage(err?.message || "登录失败");
    } finally {
      authLoading.value = false;
    }
  }

  async function submitRegister() {
    authLoading.value = true;
    authMessage.value = "";
    try {
      const data = await apiRequest("/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(registerForm)
      });
      persistToken(data.token);
      await loadMe();
      authMessage.value = "注册成功";
      if (typeof onLoggedIn === "function") onLoggedIn();
    } catch (err) {
      authMessage.value = toZhMessage(err?.message || "注册失败");
    } finally {
      authLoading.value = false;
    }
  }

  async function submitAuth() {
    if (authMode.value === "login") {
      await submitLogin();
    } else {
      await submitRegister();
    }
  }

  return {
    authLoading,
    authMessage,
    authMode,
    rememberMe,
    activeAuthForm,
    setAuthMode,
    toggleAuthMode,
    submitAuth
  };
}
