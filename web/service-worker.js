try {
  importScripts('https://www.gstatic.com/firebasejs/13.0.0/firebase-app-compat.js','https://www.gstatic.com/firebasejs/13.0.0/firebase-messaging-compat.js');
  
  firebase.initializeApp({apiKey:"AIzaSyDSRsK2skCi9CV8941MyBdXWilaUREmxo0",authDomain:"janb-baytak.firebaseapp.com",projectId:"janb-baytak",storageBucket:"janb-baytak.firebasestorage.app",messagingSenderId:"304429669668",appId:"1:304429669668:web:3783fa65667c7998fce7b1",measurementId:"G-KFVZKL5KEP"});
  const janbMessaging=firebase.messaging();
  janbMessaging.onBackgroundMessage(payload=>{
   const title=payload.notification?.title||payload.data?.title||"جنب بيتك";
   const options={body:payload.notification?.body||payload.data?.body||"في جديد على جنب بيتك",icon:"./pwa-icon.svg",badge:"./pwa-icon.svg",data:{url:payload.data?.url||"./"},tag:payload.data?.tag||"janb-update"};
   self.registration.showNotification(title,options);
  });
} catch (error) {
  console.warn('Firebase Messaging could not initialize in the service worker:', error);
}

const CACHE_NAME = "janb-pwa-shell-v17";
const APP_SHELL = ["./", "./manifest.json", "./pwa-icon.svg"];
self.addEventListener("install", event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(APP_SHELL)).then(() => self.skipWaiting()));
});
self.addEventListener("activate", event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith("janb-pwa-shell-") && key !== CACHE_NAME).map(key => caches.delete(key)))).then(() => self.clients.claim()));
});
self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET" || new URL(request.url).origin !== self.location.origin) return;
  if (request.mode === "navigate") {
    event.respondWith(fetch(request).then(response => {
      if (response && response.ok) {
        const copy = response.clone();
        caches.open(CACHE_NAME).then(cache => cache.put("./", copy));
      }
      return response;
    }).catch(() => caches.match("./")));
  }
});

self.addEventListener("notificationclick", event => {
  event.notification.close();
  const target = new URL(
    event.notification.data?.url || "./",
    self.location.origin
  ).href;

  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({
      type: "window",
      includeUncontrolled: true
    });
    for (const client of windows) {
      if (client.url.startsWith(self.location.origin) && "focus" in client) {
        await client.focus();
        if ("navigate" in client) await client.navigate(target);
        return;
      }
    }
    if (self.clients.openWindow) await self.clients.openWindow(target);
  })());
});
