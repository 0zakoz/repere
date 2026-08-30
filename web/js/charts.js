const escape = value => String(value).replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);

export function lineChart(series, { unit = "", target = null, empty = "Pas encore de données" } = {}) {
  const points = series.flatMap(item => item.points.map(point => point.value)).filter(Number.isFinite);
  if (!points.length) return `<div class="chart-empty">${escape(empty)}</div>`;
  let min = Math.min(...points, ...(target ? [target[0]] : []));
  let max = Math.max(...points, ...(target ? [target[1]] : []));
  const pad = Math.max((max - min) * .18, unit === "kg" ? 1 : 2);
  min = Math.max(0, min - pad); max += pad;
  const dates = [...new Set(series.flatMap(item => item.points.map(point => point.date)))].sort();
  const x = date => dates.length === 1 ? 50 : 8 + 84 * dates.indexOf(date) / (dates.length - 1);
  const y = value => 88 - 76 * (value - min) / Math.max(1, max - min);
  const colors = ["var(--chart-1)", "var(--chart-2)", "var(--chart-3)", "var(--chart-4)"];
  const targetBand = target ? `<rect x="8" y="${y(target[1])}" width="84" height="${Math.max(1, y(target[0]) - y(target[1]))}" rx="2" fill="var(--success)" opacity=".12"/>` : "";
  const grid = [12, 50, 88].map(value => `<line x1="8" y1="${value}" x2="92" y2="${value}" stroke="var(--outline)" opacity=".3" stroke-width=".5"/>`).join("");
  const lines = series.map((item, index) => {
    const color = colors[index % colors.length];
    const path = item.points.map((point, i) => `${i ? "L" : "M"}${x(point.date)},${y(point.value)}`).join(" ");
    const dots = item.points.map(point => `<circle cx="${x(point.date)}" cy="${y(point.value)}" r="1.7" fill="${color}"><title>${escape(point.date)} : ${point.value} ${unit}</title></circle>`).join("");
    return `<path d="${path}" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>${dots}`;
  }).join("");
  return `<div class="chart"><div class="chart-axis"><span>${format(max)} ${unit}</span><span>${format((min + max) / 2)} ${unit}</span><span>${format(min)} ${unit}</span></div><svg viewBox="0 0 100 100" preserveAspectRatio="none" role="img">${grid}${targetBand}${lines}</svg><div class="chart-dates"><span>${shortDate(dates[0])}</span><span>${dates.length > 1 ? shortDate(dates.at(-1)) : ""}</span></div>${series.length > 1 ? `<div class="legend">${series.map((item, i) => `<span><i style="background:${colors[i % colors.length]}"></i>${escape(item.label)}</span>`).join("")}</div>` : ""}</div>`;
}

export function horizontalBars(items, maxValue = null) {
  const max = maxValue ?? Math.max(1, ...items.map(item => item.value));
  return `<div class="bars">${items.map((item, index) => `<div class="bar-row"><div class="bar-label"><span>${escape(item.label)}</span><strong>${format(item.value)}</strong></div><div class="bar-track"><div class="bar-fill c${index % 4}" style="width:${Math.max(item.value ? 3 : 0, item.value / max * 100)}%"></div></div>${item.meta ? `<small>${escape(item.meta)}</small>` : ""}</div>`).join("")}</div>`;
}

const format = value => new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 1 }).format(value);
const shortDate = value => value ? value.split("-").reverse().join("/").slice(0, 8) : "";
