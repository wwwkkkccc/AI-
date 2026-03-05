import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// Vite 构建配置
export default defineConfig({
  plugins: [vue()],       // 启用 Vue 单文件组件支持
  base: "/resume-ai/"     // 部署时的公共基础路径（子路径部署）
});
