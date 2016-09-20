'use strict';
// Service Worker
// self.addEventListener('install', function(e) {
//   e.registerForeignFetch({scopes: ['/'], origins: ['*']});
// });

var root = (function() {
  var tokens = (self.location + '').split('/');
  tokens[tokens.length - 1] = '';
  return tokens.join('/');
})();

self.addEventListener('fetch', function(event) {
 console.log("fetch " + event.request.method + " " + event.request.url + " " + event.request.mode + ", client: " + event.clientId + "," + event.client + ", context: " + event.request.context + ", referrer: " + event.request.referrer);
 // console.log("ROOT: " + root);
 if (event.request.url.startsWith(root + "resource") || event.request.referrer.startsWith(root + "resource")) {
   // http://localhost:8083/resource/warcfile:IAH-20080430204825-00000-blackbook.warc.gz%233380
   let url = event.request.url.replace(root, '');

   if (event.request.referrer !== root) {
       let ref = event.request.referrer.substring(root.length + 9, event.request.referrer.lastIndexOf('/'));
       let ts = event.request.referrer.substring(event.request.referrer.lastIndexOf('/'));
       url = 'http://localhost:8083/resource/' + encodeURIComponent(ref) + ts + '/' + encodeURIComponent(url);
       // + event.request.referrer.substring(0, event.request.referrer.lastIndexOf('/'));
   } else {
       url = 'http://localhost:8083/' + url;
   }
   console.log("New URL: " + url);
   // let request = new Request('http://localhost:8083/' + event.request.url);
   let request = new Request(url);
   event.respondWith(fetch(request).catch(function(error) {console.log('Error: ' + error);}));
 }
 // if (event.request.mode !== 'same-origin' && !event.request.url.startsWith(root)
 // && event.request.url.indexOf('googleapis.com') == -1 && event.request.url.indexOf('gstatic.com') == -1) {
 //  console.log("New URL: " + 'http://localhost:8888/' + event.request.url);
 //  let request = new Request('http://localhost:8888/' + event.request.url);
 //  event.respondWith(fetch(request));
 // }
});
// self.addEventListener('foreignfetch', function(event) {
//  console.log("foreignfetch " + event.request.url);
// });
self.addEventListener('notificationclick', function(event) {
  console.log('On notification click: ', event.notification.tag);
  event.notification.close();

  // This looks to see if the current is already open and
  // focuses if it is
  event.waitUntil(clients.matchAll({
    type: "window"
  }).then(function(clientList) {
    for (var i = 0; i < clientList.length; i++) {
      var client = clientList[i];
      if (client.url == '/' && 'focus' in client)
        return client.focus();
    }
    if (clients.openWindow)
      return clients.openWindow('/');
  }));
});