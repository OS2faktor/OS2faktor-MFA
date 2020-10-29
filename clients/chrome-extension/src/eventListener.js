document.addEventListener('os2faktorEvent', function(e) {
   var someInformation = 'Hello World';

   chrome.extension.sendMessage(someInformation, function(response) {
	; // do nothing on callback
   });
}, false);

