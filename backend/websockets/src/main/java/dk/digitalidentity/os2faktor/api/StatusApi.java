package dk.digitalidentity.os2faktor.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.service.SocketHandler;

@RestController
public class StatusApi {
	
	@Autowired
	private SocketHandler socketHandler;
	
	@GetMapping("/api/status")
	public ResponseEntity<?> getStatus() {
		return new ResponseEntity<>(socketHandler.countConnections(), HttpStatus.OK);
	}
}
