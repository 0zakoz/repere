import { createSeedState } from "./seed.js";
import { normalizeState } from "./state.js";
import { adoptLegacyTargets } from "./rules.js";

const DB_NAME = "repere-pwa";
const STORE = "state";
const KEY = "app-state";

function openDb() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function transact(mode, action) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, mode);
    const store = tx.objectStore(STORE);
    const request = action(store);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
    tx.oncomplete = () => db.close();
  });
}

export async function loadState() {
  const raw = await transact("readonly", store => store.get(KEY));
  if (raw == null) {
    const seeded = createSeedState();
    await saveState(seeded);
    return seeded;
  }
  const migrated = normalizeState(raw);
  const adopted = adoptLegacyTargets(migrated, readLegacyTargets());
  if (adopted !== migrated) {
    try { localStorage.removeItem(TARGETS_KEY); } catch { /* stockage indisponible */ }
  }
  if (JSON.stringify(raw) !== JSON.stringify(adopted)) await saveState(adopted);
  return adopted;
}

export async function saveState(state) {
  const normalized = normalizeState(state);
  await transact("readwrite", store => store.put(normalized, KEY));
  return normalized;
}

export async function requestPersistentStorage() {
  if (!navigator.storage?.persist) return false;
  if (await navigator.storage.persisted?.()) return true;
  return navigator.storage.persist();
}

export function loadAppearance() {
  try {
    const parsed = JSON.parse(localStorage.getItem("repere-appearance"));
    const themes = ["original", "kawaii", "pastel", "oled", "clean"];
    const modes = ["light", "dark", "system"];
    return {
      theme: themes.includes(parsed?.theme) ? parsed.theme : "kawaii",
      mode: modes.includes(parsed?.mode) ? parsed.mode : "dark",
    };
  } catch { return { theme: "kawaii", mode: "dark" }; }
}

export function saveAppearance(value) {
  localStorage.setItem("repere-appearance", JSON.stringify(value));
}

const TARGETS_KEY = "repere-nutrition-targets";

function readLegacyTargets() {
  try { return JSON.parse(localStorage.getItem(TARGETS_KEY)); }
  catch { return null; }
}
