import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

export default defineConfig({
  plugins: [vue()],
  base: "/resume-ai/",
  build: {
    cssCodeSplit: true,
    rollupOptions: {
      output: {
        // Keep vendor cache stable and avoid rebundling app code for each deploy.
        manualChunks(id) {
          if (!id.includes("node_modules")) return;
          if (id.includes("vue")) return "vendor-vue";
          return "vendor";
        }
      }
    }
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src")
    }
  }
});
