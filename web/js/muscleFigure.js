import { html, number } from "./ui.js";

// Mêmes tracés que MuscleFigure.kt (moitié droite x >= 0, y 0..1000, miroir appliqué au rendu).
// Toute modification de silhouette doit être reportée des deux côtés (voir ARCHITECTURE.md).
const BASE = [
  "M0,10 C34,10 58,34 58,70 C58,104 44,128 24,140 L28,184 L0,184 Z",
  "M0,182 C48,184 112,190 168,200 C210,208 244,220 258,240 C268,256 264,272 248,282 C230,292 212,300 200,314 C186,332 174,366 164,402 C154,436 124,456 110,474 C105,496 113,516 127,530 C107,546 79,556 53,562 C33,566 15,569 7,571 L0,571 Z",
  "M222,228 C250,220 272,228 282,250 C290,272 293,324 294,378 C295,432 293,494 289,540 C287,566 283,584 274,582 C266,579 261,562 259,542 C255,506 251,452 247,402 C243,354 237,308 231,280 C227,260 223,242 222,228 Z",
  "M6,569 C44,567 88,559 128,547 C140,583 145,625 141,667 C137,703 129,735 119,761 C115,787 115,811 110,837 C104,873 94,911 86,939 C102,947 114,963 118,985 L40,985 C44,965 54,949 70,939 C66,903 62,861 60,825 C54,793 46,767 40,747 C28,707 16,649 12,599 C11,587 11,577 12,569 Z",
];

const FRONT_LINES = [
  "M14,192 C52,196 100,204 148,216",
  "M10,326 C55,338 105,346 150,346",
  "M8,360 L8,518",
  "M8,396 C34,400 64,402 96,400",
  "M8,430 C36,434 68,436 100,434",
  "M8,464 C34,468 66,470 98,468",
  "M8,496 C32,500 58,502 82,502",
  "M102,376 C110,416 114,456 106,494",
  "M38,760 C64,764 90,762 112,754",
  "M78,792 C74,842 70,890 68,932",
  "M236,406 C250,412 264,410 278,402",
  "M262,574 C270,578 278,576 286,570",
  "M38,942 C62,946 88,942 110,934",
];

const BACK_LINES = [
  "M6,192 C10,306 10,432 6,552",
  "M36,356 C66,378 96,418 112,452",
  "M24,636 C64,646 112,646 144,634",
  "M38,760 C64,764 90,762 112,754",
  "M72,898 C76,916 78,928 78,938",
];

export const MUSCLE_REGIONS = {
  front: [
    { id: "upper_pecs", d: "M8,198 C58,204 116,212 170,226 L167,244 C115,232 60,222 8,214 Z" },
    { id: "pecs", d: "M8,246 C56,256 112,268 162,281 C175,287 179,297 175,311 C168,333 150,345 124,349 C84,353 44,343 8,327 L8,246 Z" },
    { id: "front_delts", d: "M182,210 C212,212 232,226 236,248 C239,268 233,284 221,290 C209,294 198,286 193,270 C188,252 186,230 187,218 Z" },
    { id: "side_delts", d: "M240,238 C257,244 270,258 274,274 C277,288 270,298 259,300 C250,301 243,294 241,282 C239,268 239,252 240,244 Z" },
    { id: "abs", d: "M8,360 C36,366 68,370 96,372 C102,400 104,432 100,460 C96,488 86,508 72,522 C48,528 26,526 8,520 C6,466 6,412 8,360 Z" },
    { id: "biceps", d: "M232,304 C252,310 268,326 275,350 C281,376 283,402 281,420 C268,426 254,418 247,402 C238,376 233,340 231,318 C231,310 231,306 232,304 Z" },
    { id: "forearm_flexors", d: "M250,444 C264,452 275,470 280,490 C285,512 287,534 285,552 C274,556 265,546 260,532 C253,508 249,480 248,460 C248,450 249,446 250,444 Z" },
    { id: "adductors", d: "M8,575 C17,573 27,571 38,569 C40,611 39,655 34,693 C30,717 24,733 17,743 C12,731 8,715 7,697 C6,655 6,613 8,575 Z" },
    { id: "quads", d: "M44,581 C68,577 98,567 128,553 C140,589 144,629 140,669 C136,699 127,725 115,747 C90,755 66,753 46,745 C43,703 43,655 43,611 C43,601 43,589 44,581 Z" },
  ],
  back: [
    { id: "traps", d: "M8,184 C58,186 124,196 180,212 C162,242 134,268 102,288 C74,304 44,314 18,318 C14,296 10,276 8,258 C6,232 6,206 8,184 Z" },
    { id: "rear_delts", d: "M196,216 C226,220 254,234 268,254 C275,268 269,282 256,290 C243,296 228,292 219,280 C208,264 200,242 197,226 Z" },
    { id: "lats", d: "M8,326 C44,334 88,348 126,366 C152,378 168,396 172,416 C173,436 168,454 157,468 C144,482 126,488 104,488 C70,476 34,454 8,430 C6,396 6,360 8,326 Z" },
    { id: "lower_back", d: "M8,492 C28,498 48,506 66,516 C76,530 80,542 78,552 C58,560 34,562 8,560 C6,536 6,514 8,492 Z" },
    { id: "triceps", d: "M234,308 C254,314 270,330 277,354 C283,380 285,406 283,424 C270,430 256,422 249,406 C240,380 235,342 233,320 C233,312 233,310 234,308 Z" },
    { id: "forearm_extensors", d: "M250,448 C264,456 275,474 280,494 C285,516 287,538 285,556 C274,560 265,550 260,536 C253,512 249,484 248,464 C248,454 249,450 250,448 Z" },
    { id: "glutes", d: "M8,558 C36,552 68,552 98,560 C128,568 150,582 158,602 C163,618 155,632 140,640 C112,649 80,651 52,645 C34,641 18,633 8,621 C6,599 6,577 8,558 Z" },
    { id: "hamstrings", d: "M10,654 C50,650 95,644 132,632 C146,664 152,698 148,728 C144,752 134,772 122,786 C90,792 56,790 26,782 C18,744 13,702 12,664 C12,660 11,656 10,654 Z" },
    { id: "calves", d: "M40,800 C62,792 84,796 98,810 C108,830 110,858 104,886 C98,914 90,936 78,948 C62,950 50,942 44,928 C37,896 35,860 36,828 C37,816 38,808 40,800 Z" },
  ],
};

export function heatFraction(value, max) {
  if (!Number.isFinite(value) || value <= 0 || !Number.isFinite(max) || max <= 0) return 0;
  return Math.min(1, value / max);
}

const heatFill = fraction => `color-mix(in srgb, var(--primary) ${Math.round(6 + 74 * fraction)}%, var(--surface-2))`;

export function renderMuscleFigure({ muscles, rows, view, selectedId }) {
  const byId = new Map(rows.map(row => [row.muscle.id, row]));
  const max = Math.max(0, ...rows.map(row => row.weightedSets));
  const regions = view === "back" ? MUSCLE_REGIONS.back : MUSCLE_REGIONS.front;
  const lines = view === "back" ? BACK_LINES : FRONT_LINES;
  const half = shapes => `<g>${shapes}</g><g transform="scale(-1,1)">${shapes}</g>`;
  const base = BASE.map(d => `<path d="${d}" fill="var(--surface-2)" stroke="var(--outline)" stroke-width="2"/>`).join("");
  const zones = regions.map(({ id, d }) => {
    const fraction = heatFraction(byId.get(id)?.weightedSets ?? 0, max);
    const selected = selectedId === id;
    return `<path d="${d}" fill="${heatFill(fraction)}" stroke="${selected ? "var(--success)" : "var(--outline)"}" stroke-width="${selected ? 5 : 2}" data-action="select-muscle" data-id="${id}"><title>${html(byId.get(id)?.muscle.name ?? id)}</title></path>`;
  }).join("");
  const decor = lines.map(d => `<path d="${d}" fill="none" stroke="var(--outline)" stroke-width="2"/>`).join("");
  const selected = selectedId ? byId.get(selectedId) : null;
  const selectedName = muscles.find(m => m.id === selectedId)?.name ?? selected?.muscle.name;
  const detail = !selectedId ? "" : !selected || selected.weightedSets <= 0
    ? `<p class="subtitle">${html(selectedName ?? selectedId)} : aucune série pondérée sur la période.</p>`
    : `<div class="list-item"><div class="grow"><strong>${html(selectedName ?? selectedId)} — ${number(selected.weightedSets)} séries pond.</strong><small>${selected.averageReps == null ? "—" : `${number(selected.averageReps)} reps`} · RIR ${selected.averageRir == null ? "—" : number(selected.averageRir)}</small></div></div>`;
  return `<section class="card"><div class="card-title"><h2>Carte musculaire</h2></div><p class="subtitle">Touche une zone pour le détail</p><div class="tabs"><button class="${view === "front" ? "active" : ""}" data-action="muscle-view" data-view="front">Avant</button><button class="${view === "back" ? "active" : ""}" data-action="muscle-view" data-view="back">Dos</button></div><svg class="muscle-figure" viewBox="-330 -10 660 1020" role="img" data-action="select-muscle" data-id="">${half(base + zones + decor)}</svg><div class="heat-legend"><div class="heat-bar"></div><div class="heat-labels"><span>Faible</span><span>Élevé</span></div></div>${detail}</section>`;
}
