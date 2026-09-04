const CACHE = "repere-web-v1.11.1";
const BASE = new URL("./", self.location).pathname;
const ASSETS = [
  "./", "./index.html", "./styles.css", "./manifest.webmanifest", "./assets/icon.svg",
  "./assets/icon-180.png", "./assets/icon-512.png",
  "./assets/nunito-variable.ttf", "./assets/fredoka-variable.ttf",
  "./js/app.js", "./js/seed.js", "./js/state.js", "./js/store.js", "./js/rules.js",
  "./js/exporters.js", "./js/charts.js", "./js/ui.js", "./js/nutritionView.js"
].map(path => new URL(path, self.location).pathname);

self.addEventListener("install", event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(ASSETS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(key => key !== CACHE).map(key => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", event => {
  if (event.request.method !== "GET" || new URL(event.request.url).origin !== self.location.origin) return;
  event.respondWith(
    caches.match(event.request).then(cached => cached || fetch(event.request).then(response => {
      if (response.ok) caches.open(CACHE).then(cache => cache.put(event.request, response.clone()));
      return response;
    }).catch(() => caches.match(BASE)))
  );
});
