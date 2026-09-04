import { html, number } from "./ui.js";

// Mêmes tracés que MuscleFigure.kt (moitié droite x >= 0, y 0..1000, miroir appliqué au rendu).
// Toute modification de silhouette doit être reportée des deux côtés (voir ARCHITECTURE.md).
const BASE = [
  "M0,14 C32,14 56,38 56,72 C56,104 40,128 20,140 L24,184 L0,184 Z",
  "M0,182 C54,184 122,192 180,204 C220,212 250,224 261,244 C269,260 263,274 249,282 C231,292 213,300 201,314 C187,332 177,366 167,402 C157,436 129,456 115,474 C110,496 118,516 132,530 C112,546 84,556 58,562 C38,566 18,569 8,571 L0,571 Z",
  "M220,226 C252,218 278,228 288,252 C296,274 299,328 299,384 C299,440 297,498 292,544 C289,572 281,588 270,585 C261,581 256,564 254,544 C250,508 246,454 242,404 C238,356 232,310 226,282 C222,260 220,240 220,226 Z",
  "M8,567 C48,565 94,557 134,545 C146,581 151,623 147,665 C143,701 135,733 125,759 C121,785 121,809 116,835 C110,871 100,909 92,937 C108,945 120,961 124,983 L46,983 C50,963 60,947 76,937 C72,901 68,859 66,823 C60,791 52,765 46,745 C34,705 22,647 18,597 C17,585 17,575 18,567 Z",
]

const FRONT_LINES = [
  "M16,192 C54,196 102,204 148,216",
  "M14,324 C58,336 106,344 148,344",
  "M12,360 L12,516",
  "M12,394 C38,398 66,400 92,398",
  "M12,428 C40,432 70,434 96,432",
  "M12,462 C38,466 68,468 94,466",
  "M98,374 C106,414 110,454 102,492",
  "M40,758 C66,762 92,760 112,752",
  "M80,790 C76,840 72,888 70,930",
  "M234,404 C248,410 262,408 276,400",
  "M260,566 C268,570 276,568 284,562",
  "M42,940 C64,944 88,940 108,932",
]

const BACK_LINES = [
  "M8,192 C12,306 12,432 8,552",
  "M38,354 C68,376 98,416 114,450",
  "M28,634 C66,644 112,644 142,632",
  "M40,758 C66,762 92,760 112,752",
  "M74,896 C78,914 80,926 80,936",
]

export const MUSCLE_REGIONS = {
  front: [
    { id: "upper_pecs", d: "M12,198 C62,204 118,212 166,226 L162,242 C112,230 60,220 12,212 Z" },
    { id: "pecs", d: "M12,244 C60,254 114,266 158,279 C172,285 176,296 171,310 C163,332 145,344 119,348 C81,352 42,342 12,326 L12,244 Z" },
    { id: "front_delts", d: "M180,208 C208,208 230,222 236,245 C240,265 234,281 222,287 C210,291 199,283 194,267 C189,249 187,228 188,216 Z" },
    { id: "side_delts", d: "M240,236 C258,242 272,256 276,272 C279,286 272,296 261,298 C252,299 245,292 243,280 C241,266 240,250 241,242 Z" },
    { id: "abs", d: "M12,358 C40,364 70,368 94,370 C100,398 102,430 98,458 C94,486 84,506 70,520 C46,526 26,524 12,518 C10,464 10,410 12,358 Z" },
    { id: "biceps", d: "M230,300 C250,306 266,322 273,346 C279,372 281,398 279,416 C266,422 252,414 245,398 C236,372 231,336 229,314 C229,306 229,302 230,300 Z" },
    { id: "forearm_flexors", d: "M248,440 C262,448 273,466 278,486 C283,508 285,530 283,548 C272,552 263,542 258,528 C251,504 247,476 246,456 C246,446 247,442 248,440 Z" },
    { id: "adductors", d: "M12,573 C21,571 31,569 42,567 C44,609 43,653 38,691 C34,715 28,731 21,741 C16,729 12,713 11,695 C9,653 9,611 12,573 Z" },
    { id: "quads", d: "M48,579 C72,575 100,565 128,551 C140,587 144,627 140,667 C136,697 129,723 117,745 C92,753 68,751 48,743 C45,701 44,653 45,609 C45,599 46,587 48,579 Z" },
  ],
  back: [
    { id: "traps", d: "M12,182 C62,184 128,194 182,210 C164,240 136,266 104,286 C76,302 46,312 22,316 C18,294 14,274 12,256 C10,230 10,204 12,182 Z" },
    { id: "rear_delts", d: "M194,212 C224,216 252,230 266,250 C273,264 267,278 254,286 C241,292 226,288 217,276 C206,260 198,238 195,222 Z" },
    { id: "lats", d: "M12,324 C48,332 92,346 130,364 C156,376 172,394 176,414 C177,434 172,452 161,466 C148,480 130,486 108,484 C74,472 38,450 12,426 C10,392 10,358 12,324 Z" },
    { id: "lower_back", d: "M12,490 C32,496 52,504 70,514 C80,528 84,540 82,550 C62,558 38,560 12,558 C10,534 10,512 12,490 Z" },
    { id: "triceps", d: "M232,304 C252,310 268,326 275,350 C281,376 283,402 281,420 C268,426 254,418 247,402 C238,376 233,338 231,316 C231,308 231,306 232,304 Z" },
    { id: "forearm_extensors", d: "M248,444 C262,452 273,470 278,490 C283,512 285,534 283,552 C272,556 263,546 258,532 C251,508 247,480 246,460 C246,450 247,446 248,446 Z" },
    { id: "glutes", d: "M12,556 C40,550 72,550 100,558 C126,566 142,580 148,598 C152,612 146,624 134,631 C106,640 76,642 50,636 C32,632 20,624 12,612 C10,593 10,573 12,556 Z" },
    { id: "hamstrings", d: "M12,656 C52,652 94,648 128,636 C142,668 146,702 142,732 C138,756 130,776 120,788 C90,794 58,792 28,784 C20,746 15,704 14,666 C14,662 13,658 12,656 Z" },
    { id: "calves", d: "M44,796 C66,788 88,792 102,806 C112,826 114,854 108,882 C102,910 94,932 82,944 C66,946 54,938 48,924 C41,892 39,856 40,824 C41,812 42,804 44,796 Z" },
  ],
};

export function heatFraction(value, max) {
  if (!Number.isFinite(value) || value <= 0 || !Number.isFinite(max) || max <= 0) return 0;
  return Math.min(1, value / max);
}

export const heatFill = fraction => `color-mix(in srgb, var(--primary) ${Math.round(100 * Math.min(1, Math.max(0, fraction)))}%, var(--surface-2))`;

function figureSvg(uid, regions, lines, fills) {
  const half = shapes => `<g>${shapes}</g><g transform="scale(-1,1)">${shapes}</g>`;
  const base = BASE.map(d => `<path d="${d}" fill="url(#mg-base-${uid})" stroke="var(--outline)" stroke-width="3"/>`).join("");
  const zones = regions.map(({ id, d }) => {
    const selected = fills.selectedId === id;
    return `<path d="${d}" fill="${fills.fill(id)}" stroke="${selected ? "var(--success)" : "var(--outline)"}" stroke-width="${selected ? 6 : 3}"${selected ? ` style="filter:drop-shadow(0 0 6px var(--success))"` : ""} data-action="select-muscle" data-id="${id}"><title>${html(fills.name(id))}</title></path>`;
  }).join("");
  const decor = lines.map(d => `<path d="${d}" fill="none" stroke="var(--outline)" stroke-width="2.5"/>`).join("");
  return `<svg class="muscle-figure" viewBox="-330 -10 660 1020" role="img" data-action="select-muscle" data-id=""><defs><linearGradient id="mg-base-${uid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="var(--surface-2)"/><stop offset="1" stop-color="var(--surface-3)"/></linearGradient></defs>${half(base + zones + decor)}</svg>`;
}

export function renderMuscleFigure({ muscles, rows, selectedId }) {
  const byId = new Map(rows.map(row => [row.muscle.id, row]));
  const max = Math.max(0, ...rows.map(row => row.weightedSets));
  const fills = {
    selectedId,
    fill: id => heatFill(heatFraction(byId.get(id)?.weightedSets ?? 0, max)),
    name: id => byId.get(id)?.muscle.name ?? muscles.find(m => m.id === id)?.name ?? id,
  };
  const selected = selectedId ? byId.get(selectedId) : null;
  const selectedName = muscles.find(m => m.id === selectedId)?.name ?? selected?.muscle.name;
  const detail = !selectedId ? "" : !selected || selected.weightedSets <= 0
    ? `<p class="subtitle">${html(selectedName ?? selectedId)} : aucune série pondérée sur la période.</p>`
    : `<div class="list-item"><div class="grow"><strong>${html(selectedName ?? selectedId)} — ${number(selected.weightedSets)} séries pond.</strong><small>${selected.averageReps == null ? "—" : `${number(selected.averageReps)} reps`} · RIR ${selected.averageRir == null ? "—" : number(selected.averageRir)}</small></div></div>`;
  const view = (uid, label, regions, lines) => `<div class="muscle-view"><div class="muscle-svg-wrap">${figureSvg(uid, regions, lines, fills)}</div><div class="muscle-caption">${label}</div></div>`;
  return `<section class="card"><div class="card-title"><h2>Carte musculaire</h2></div><p class="subtitle">Touche une zone pour le détail</p><div class="muscle-views">${view("front", "Avant", MUSCLE_REGIONS.front, FRONT_LINES)}${view("back", "Dos", MUSCLE_REGIONS.back, BACK_LINES)}</div><div class="heat-legend"><div class="heat-bar"></div><div class="heat-labels"><span>Faible</span><span>Élevé</span></div></div>${detail}</section>`;
}
