export function html(value) {
  return String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

export function icon(name) {
  const paths = {
    settings: "⚙", play: "▶", add: "+", edit: "✎", delete: "⌫", download: "⇩", back: "‹",
    close: "×", check: "✓", pause: "Ⅱ", timer: "◷", calendar: "▣", more: "•••", upload: "⇧",
  };
  return `<span class="icon" aria-hidden="true">${paths[name] ?? name}</span>`;
}

export function button(label, action, { kind = "primary", iconName = null, disabled = false, extra = "" } = {}) {
  return `<button class="btn ${kind}" data-action="${action}" ${disabled ? "disabled" : ""} ${extra}>${iconName ? icon(iconName) : ""}<span>${html(label)}</span></button>`;
}

export function emptyState(emoji, title, text, action = "", label = "") {
  return `<section class="empty-state"><div class="empty-emoji">${emoji}</div><h3>${html(title)}</h3><p>${html(text)}</p>${action ? button(label, action, { kind: "secondary", iconName: "add" }) : ""}</section>`;
}

export function dialog({ title, content, actions = "", wide = false }) {
  return `<div class="dialog-backdrop"><section class="dialog ${wide ? "wide" : ""}" role="dialog" aria-modal="true" aria-label="${html(title)}" data-dialog><header><h2>${html(title)}</h2><button class="icon-btn" data-action="dismiss-dialog" aria-label="Fermer">${icon("close")}</button></header><div class="dialog-content">${content}</div>${actions ? `<footer>${actions}</footer>` : ""}</section></div>`;
}

export function toast(message, kind = "success", action = null) {
  const old = document.querySelector(".toast");
  old?.remove();
  const element = document.createElement("div");
  element.className = `toast ${kind}`;
  element.append(document.createTextNode(message));
  if (action) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = action.label;
    button.addEventListener("click", () => { element.remove(); action.run(); });
    element.append(button);
  }
  document.body.append(element);
  setTimeout(() => element.remove(), 2600);
}

export async function downloadFile(name, content, type) {
  const file = typeof File !== "undefined" ? new File([content], name, { type }) : null;
  const coarse = globalThis.matchMedia?.("(pointer: coarse)").matches ?? false;
  if (file && coarse && navigator.share && navigator.canShare?.({ files: [file] })) {
    try { await navigator.share({ files: [file], title: name }); return; }
    catch (error) { if (error?.name === "AbortError") return; }
  }
  try {
    const url = URL.createObjectURL(new Blob([content], { type }));
    const link = document.createElement("a");
    link.href = url; link.download = name;
    document.body.append(link); link.click(); link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (error) { toast(`Export impossible : ${error.message}`, "error"); }
}

export function formatDate(date) {
  try { return new Intl.DateTimeFormat("fr-FR", { weekday: "short", day: "numeric", month: "short", year: "numeric" }).format(new Date(`${date}T12:00:00`)); }
  catch { return date; }
}

export function formatTime(timestamp) {
  return new Intl.DateTimeFormat("fr-FR", { hour: "2-digit", minute: "2-digit" }).format(new Date(timestamp));
}

export function number(value, digits = 1) {
  return new Intl.NumberFormat("fr-FR", { maximumFractionDigits: digits, minimumFractionDigits: digits }).format(value);
}

export function catSvg() {
  return `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4.8 9.1 6.5.5 11 6.7Z" fill="#63433f"/><path d="M13 6.7 17.5.5 19.2 9.1Z" fill="#63433f"/><circle cx="12" cy="13.2" r="8.6" fill="#ffe9c9"/><ellipse cx="12" cy="13.5" rx="5" ry="6" fill="#63433f"/><circle cx="9.6" cy="11.5" r="1.3" fill="#77c7f2"/><circle cx="14.4" cy="11.5" r="1.3" fill="#77c7f2"/><circle cx="9.6" cy="11.5" r=".6" fill="#2b1d25"/><circle cx="14.4" cy="11.5" r=".6" fill="#2b1d25"/><circle cx="12" cy="15.1" r=".85" fill="#f2a0b7"/></svg>`;
}
