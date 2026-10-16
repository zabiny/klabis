const t=self;t.addEventListener("install",i=>{i.waitUntil(t.skipWaiting())});t.addEventListener("activate",i=>{i.waitUntil(t.clients.claim())});
