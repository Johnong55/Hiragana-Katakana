const CACHE_NAME = 'kana-n5-offline-v1';
const OFFLINE_ASSETS = [
  './',
  './index.html',
  './style.css',
  './app.js',
  './manifest.json',
  './assets/kana_strokes.json',
  './assets/kana_vocab.json',
  './assets/memes/meme_shiba.jpg',
  './assets/memes/meme_capybara.jpg',
  './assets/memes/meme_cat_angry.jpg',
  './assets/memes/meme_cat_blep.jpg',
  './assets/memes/meme_cat_grumpy.jpg',
  './assets/memes/meme_cat_reading.jpg',
  './assets/memes/meme_cat_surprised.jpg',
  './assets/memes/meme_duck.jpg',
  './assets/memes/meme_hamster.jpg',
  './assets/memes/meme_red_panda.jpg'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(OFFLINE_ASSETS))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request).then((cached) => {
      return (
        cached ||
        fetch(event.request).then((resp) => {
          const clone = resp.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          return resp;
        })
      );
    })
  );
});
