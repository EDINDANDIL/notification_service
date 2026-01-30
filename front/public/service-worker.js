/* eslint-env serviceworker */

self.addEventListener('push', function (event) {
    console.log('🔥 PUSH EVENT received:', event);

    event.waitUntil(
        (async function () {
            let data = { title: '📢 Уведомление', body: 'Новое сообщение' };

            try {
                if (event.data) {
                    try {
                        const json = event.data.json();
                        data = Object.assign(data, json);
                    } catch (err) {
                        data.body = event.data.text() || data.body;
                    }
                }

                await self.registration.showNotification(data.title, {
                    body: data.body,
                    vibrate: [100, 50, 100],
                });
                console.log('✅ Notification shown');
            } catch (err) {
                console.error('💥 Push handler error:', err);
            }
        })()
    );
});

self.addEventListener('notificationclick', function (event) {
    console.log('🖱️ Notification clicked');
    event.notification.close();

    event.waitUntil(
        (async function () {
            const url = '/';
            const clientsList = await self.clients.matchAll({
                type: 'window',
                includeUncontrolled: true,
            });
            const client = clientsList.find((c) => c.url === url);

            if (client) return client.focus();
            return self.clients.openWindow(url);
        })()
    );
});