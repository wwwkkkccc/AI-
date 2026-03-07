import { computed } from "vue";

export function usePaginationActions({ page, size, total, load }) {
  const totalPages = computed(() => Math.ceil(total.value / size.value) || 1);

  function goPage(nextPage) {
    page.value = Number(nextPage) || 0;
    load();
  }

  function changeSize(nextSize) {
    const normalizedSize = Number(nextSize) || size.value;
    size.value = normalizedSize;
    page.value = 0;
    load();
  }

  return {
    totalPages,
    goPage,
    changeSize
  };
}
