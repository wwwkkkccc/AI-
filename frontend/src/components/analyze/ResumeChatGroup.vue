<template>
  <div class="sub-card">
    <h3>多轮对话式简历优化助手</h3>
    <div class="inline">
      <button type="button" :disabled="loading || !canStart" @click="$emit('start')">
        {{ chatState?.sessionId ? "重新开启会话" : "开启会话" }}
      </button>
      <button
        type="button"
        class="mini-btn neutral"
        :disabled="loading || !chatState?.sessionId"
        @click="$emit('quick-ask', '这条经历具体怎么改成 STAR？')"
      >
        快捷追问
      </button>
    </div>
    <p class="message">{{ message }}</p>
    <div v-if="chatState?.sessionId" class="chat-panel">
      <div class="chat-list">
        <div
          v-for="msg in chatState.messages || []"
          :key="`chat-${msg.id}`"
          class="chat-item"
          :class="msg.role === 'USER' ? 'chat-user' : 'chat-ai'"
        >
          <strong>{{ msg.role === "USER" ? "我" : "AI" }}</strong>
          <p>{{ msg.content }}</p>
        </div>
      </div>
      <form class="chat-form" @submit.prevent="$emit('send')">
        <input
          v-model.trim="chatState.input"
          type="text"
          placeholder="例如：这段项目如何写成带量化结果的 STAR？"
        />
        <button :disabled="loading || !chatState.input">
          {{ loading ? "发送中..." : "发送" }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
defineProps({
  chatState: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" },
  canStart: { type: Boolean, default: false }
});

defineEmits(["start", "send", "quick-ask"]);
</script>
