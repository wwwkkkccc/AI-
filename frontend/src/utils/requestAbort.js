// Keep only the newest request per key, cancel previous in-flight request.
export function createRequestAbortManager() {
  const controllers = new Map();

  function nextSignal(key) {
    cancel(key);
    const controller = new AbortController();
    controllers.set(key, controller);
    return controller.signal;
  }

  function cancel(key) {
    const controller = controllers.get(key);
    if (!controller) return;
    controller.abort();
    controllers.delete(key);
  }

  function cancelAll() {
    controllers.forEach((controller) => controller.abort());
    controllers.clear();
  }

  return { nextSignal, cancel, cancelAll };
}

export function isAbortError(err) {
  return err?.name === "AbortError";
}
