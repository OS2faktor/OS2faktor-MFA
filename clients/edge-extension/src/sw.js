
// challenge window
var pushFlag;
self.addEventListener('install', event => {
	self.skipWaiting();
});

self.addEventListener('push', event => {
	pushFlag = true;
});

addEventListener('message', event => {
	// event is an ExtendableMessageEvent object
	event.source.postMessage(pushFlag);
	pushFlag = false;
});
