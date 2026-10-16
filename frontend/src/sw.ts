/// <reference lib="webworker" />

import {handleNotificationClick, handlePush, handlePushSubscriptionChange} from './pwa/pushHandlers';

const sw = self as unknown as ServiceWorkerGlobalScope;

sw.addEventListener('install', (event) => {
    event.waitUntil(sw.skipWaiting());
});

sw.addEventListener('activate', (event) => {
    event.waitUntil(sw.clients.claim());
});

sw.addEventListener('push', (event) => handlePush(sw, event));

sw.addEventListener('notificationclick', (event) => handleNotificationClick(sw, event));

sw.addEventListener('pushsubscriptionchange', (event) =>
    handlePushSubscriptionChange(sw, event as unknown as Parameters<typeof handlePushSubscriptionChange>[1]));
