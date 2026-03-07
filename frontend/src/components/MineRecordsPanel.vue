<template>
  <section class="panel-card">
    <h3>My Analysis Records</h3>
    <div class="toolbar">
      <input :value="mineKeyword" type="text" placeholder="Filter by filename / role / summary" @input="onKeywordInput" />
      <button class="btn ghost" @click="loadMineAnalyses">Refresh</button>
    </div>
    <p class="message">{{ mineMessage }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>File</th>
            <th>Role</th>
            <th>Score</th>
            <th>Coverage</th>
            <th>Created At</th>
            <th>Summary</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredMineItems" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.filename || "-" }}</td>
            <td>{{ item.targetRole || "-" }}</td>
            <td>{{ formatScore(item.score) }}</td>
            <td>{{ formatCoverage(item.coverage) }}</td>
            <td>{{ formatTime(item.createdAt) }}</td>
            <td>{{ item.optimizedSummary || "-" }}</td>
          </tr>
          <tr v-if="!filteredMineItems.length"><td colspan="7" class="empty">No records</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <button class="btn ghost" :disabled="minePage === 0" @click="mineGoPage(minePage - 1)">Prev</button>
      <button v-for="page in minePageRange" :key="`mine-${page}`" class="btn ghost" :class="{ active: page === minePage }" @click="mineGoPage(page)">
        {{ page + 1 }}
      </button>
      <button class="btn ghost" :disabled="minePage >= mineTotalPages - 1" @click="mineGoPage(minePage + 1)">Next</button>
      <select :value="mineSize" @change="mineChangeSize($event.target.value)">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <span>Total {{ mineTotal }}</span>
    </div>
  </section>
</template>

<script setup>
import { formatCoverage, formatScore, formatTime } from "../utils/displayFormat";

defineProps({
  mineKeyword: { type: String, required: true },
  loadMineAnalyses: { type: Function, required: true },
  mineMessage: { type: String, required: true },
  filteredMineItems: { type: Array, required: true },
  minePage: { type: Number, required: true },
  minePageRange: { type: Array, required: true },
  mineTotalPages: { type: Number, required: true },
  mineGoPage: { type: Function, required: true },
  mineSize: { type: Number, required: true },
  mineChangeSize: { type: Function, required: true },
  mineTotal: { type: Number, required: true }
});

const emit = defineEmits(["update:mineKeyword"]);

function onKeywordInput(event) {
  emit("update:mineKeyword", event?.target?.value ?? "");
}
</script>
