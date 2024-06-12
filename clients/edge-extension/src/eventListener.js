document.addEventListener('os2faktorEvent', function(e) {
   var someInformation = 'OS2Faktor Event';

   chrome.extension.sendMessage(someInformation, function(response) {
	; // do nothing on callback
   });
}, false);

