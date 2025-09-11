//"Shortcut" for displaying challenges
document.addEventListener('os2faktorEvent', function(e) {
   chrome.runtime.sendMessage({ os2faktorEvent: true }, function(response) {
	   // do nothing on callback
   });
}, false);
