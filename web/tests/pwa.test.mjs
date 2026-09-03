import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

test("le manifeste configure une application autonome dans le scope GitHub Pages", async () => {
  const manifest = JSON.parse(await readFile(new URL("../manifest.webmanifest", import.meta.url), "utf8"));
  assert.equal(manifest.display,"standalone");
  assert.equal(manifest.start_url,"./");
  assert.equal(manifest.scope,"./");
  assert.ok(manifest.icons.some(icon => icon.purpose.includes("maskable")));
});

test("le service worker précharge le cœur applicatif et les polices", async () => {
  const worker = await readFile(new URL("../sw.js", import.meta.url), "utf8");
  for (const asset of ["index.html","styles.css","js/app.js","nunito-variable.ttf","fredoka-variable.ttf"]) {
    assert.match(worker,new RegExp(asset.replaceAll(".","\\.")));
  }
});

test("le service worker ne référence que des fichiers existants", async () => {
  const root = new URL("../", import.meta.url);
  const worker = await readFile(new URL("../sw.js", import.meta.url), "utf8");
  const refs = [...new Set([...worker.matchAll(/"\.\/[^"]*"/g)].map(match => match[0].slice(1, -1)))];
  assert.ok(refs.length > 10);
  for (const ref of refs) {
    if (ref === "./") continue;
    const target = new URL(ref, root);
    assert.ok((await readFile(target)).length >= 0, `Asset manquant : ${ref}`);
  }
});

test("l'icône est un lapin blanc sur fond rose aux bonnes tailles", async () => {
  const svg = await readFile(new URL("../assets/icon.svg", import.meta.url), "utf8");
  assert.match(svg, /fill="#ff9fc8"/);
  assert.match(svg, /fill="#ffffff"/);
  for (const [file, size] of [["../assets/icon-180.png", 180], ["../assets/icon-512.png", 512]]) {
    const bytes = await readFile(new URL(file, import.meta.url));
    assert.equal(bytes.subarray(1, 4).toString(), "PNG");
    assert.equal(bytes.readUInt32BE(16), size);
    assert.equal(bytes.readUInt32BE(20), size);
  }
});

test("toutes les icônes du manifeste existent avec une 512 any", async () => {
  const root = new URL("../", import.meta.url);
  const manifest = JSON.parse(await readFile(new URL("../manifest.webmanifest", import.meta.url), "utf8"));
  for (const icon of manifest.icons) {
    assert.ok((await readFile(new URL(icon.src, root))).length > 0, `Icône manquante : ${icon.src}`);
  }
  assert.ok(manifest.icons.some(icon => (icon.sizes ?? "").split(" ").includes("512x512") && (icon.purpose ?? "").includes("any")));
});
