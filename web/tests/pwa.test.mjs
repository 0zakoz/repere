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
