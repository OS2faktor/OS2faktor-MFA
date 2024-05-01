package dk.digitalidentity.os2faktor.api;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.dto.NSISClientDTO;
import dk.digitalidentity.os2faktor.api.dto.NSISDetailsDTO;
import dk.digitalidentity.os2faktor.dao.ServerDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.HardwareToken;
import dk.digitalidentity.os2faktor.dao.model.LocalClient;
import dk.digitalidentity.os2faktor.dao.model.Server;
import dk.digitalidentity.os2faktor.dao.model.User;
import dk.digitalidentity.os2faktor.dao.model.enums.ClientType;
import dk.digitalidentity.os2faktor.dao.model.enums.NSISLevel;
import dk.digitalidentity.os2faktor.security.AuthorizedServerHolder;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.HardwareTokenService;
import dk.digitalidentity.os2faktor.service.LocalClientService;
import dk.digitalidentity.os2faktor.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class NSISServerApi {
	
	@Autowired
	private ClientService clientService;
	
	@Autowired
	private ServerDao serverDao;

	@Autowired
	private LocalClientService localClientService;

	@Autowired
	private UserService userService;
	
	@Autowired
	private HardwareTokenService hardwareTokenService;
	
	@Value("${os2faktor.frontend.baseurl}")
	private String frontendBaseUrl;
	
	@GetMapping("/api/server/nsis/clients")
	public ResponseEntity<?> getClients(
			@RequestParam(required = false, value = "ssn") String encodedSsn,
			@RequestParam(required = false, value = "deviceId") String deviceId,
			@RequestHeader("connectorVersion") String connectorVersion) {

		// should not happen, but better safe than NullPointer ;)
		Server server = AuthorizedServerHolder.getServer();
		if (server == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		
		if (server.getConnectorVersion() == null && connectorVersion != null || !server.getConnectorVersion().equals(connectorVersion)) {
			if (connectorVersion.length() > 32) {
				return new ResponseEntity<>("ConnectorVersion header must not be longer than 32 characters!", HttpStatus.BAD_REQUEST);
			}

			server.setConnectorVersion(connectorVersion);
			serverDao.save(server);
		}

		HashSet<NSISClientDTO> clients = new HashSet<>();

		try {
			if (encodedSsn != null && encodedSsn.length() > 0) {
				addClientsByEncodedSsn(encodedSsn, server.getMunicipality().getCvr(), clients);
			}
			if (deviceId != null) {
				addClientsByDeviceId(deviceId, server.getMunicipality().getCvr(), clients);
			}
		}
		catch (Exception ex) {
			log.error("Bad request from server " + server.getName(), ex);

			return new ResponseEntity<>("Unexpected error processing payload", HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(clients, HttpStatus.OK);
	}

	@PostMapping("/api/server/nsis/{deviceId}/details")
	public ResponseEntity<?> details(@PathVariable("deviceId") String deviceId) {
		Client client = clientService.getByDeviceId(deviceId);
		if (client == null) {
			return ResponseEntity.badRequest().body("Incorrect deviceId");
		}
		NSISDetailsDTO nsisDetailsDTO = new NSISDetailsDTO(client);

		return ResponseEntity.ok(nsisDetailsDTO);
	}

	private void addClientsByEncodedSsn(String encodedSsn, String cvr, HashSet<NSISClientDTO> clients) {
		User user = userService.getByEncodedSsn(encodedSsn);

		if (user != null && user.getClients() != null && user.getClients().size() > 0) {
			for (Client client : user.getClients()) {
				if (client.isDisabled() || client.isLocked()) {
					continue;
				}

				NSISClientDTO clientDTO = new NSISClientDTO();
				clientDTO.setDeviceId(client.getDeviceId());
				clientDTO.setName(client.getName());
				clientDTO.setType(client.getType());
				clientDTO.setNsisLevel(client.getNsisLevel());
				clientDTO.setPrime(client.isPrime());
				clientDTO.setRoaming(client.isRoaming());
				clientDTO.setLocked(client.isLocked());
				clientDTO.setLockedUntil(client.getLockedUntil());
				
				if (client.getPincode() != null && client.getPincode().length() > 0) {
					clientDTO.setHasPincode(true);
				}
				else if (client.getType().equals(ClientType.YUBIKEY)) {
					// for now the decision is that YubiKeys have a pincode build in due to its physical nature
					clientDTO.setHasPincode(true);
				}
				else if (client.getType().equals(ClientType.TOTP)) {
					// for now the decision is that YubiKeys have a pincode build in due to its physical nature
					clientDTO.setHasPincode(true);
				}
				else if (client.getType().equals(ClientType.TOTPH)) {
					HardwareToken hardwareToken = hardwareTokenService.getByClient(client.getDeviceId());
					if (hardwareToken != null) {
						clientDTO.setSerialnumber(hardwareToken.getSerialnumber());
					}
				}
				
				clients.add(clientDTO);
			}
		}
		
		List<LocalClient> localClients = localClientService.getByEncodedSsnAndCvr(encodedSsn, cvr);
		if (localClients != null && localClients.size() > 0) {
			for (LocalClient localClient : localClients) {
				Client client = clientService.getByDeviceId(localClient.getDeviceId());
				if (client != null) {
					if (client.isDisabled() || client.isLocked()) {
						continue;
					}

					// already added - skip
					if (clients.stream().anyMatch(c -> Objects.equals(c.getDeviceId(), client.getDeviceId()))) {
						continue;
					}
					
					NSISClientDTO clientDTO = new NSISClientDTO();
					clientDTO.setDeviceId(client.getDeviceId());
					clientDTO.setName(client.getName());
					clientDTO.setType(client.getType());
					clientDTO.setLocked(client.isLocked());

					if (clientDTO.isLocked()) {
						clientDTO.setLockedUntil(client.getLockedUntil());
					}

					if (localClient.getNsisLevel() != null) {
						clientDTO.setNsisLevel(NSISLevel.valueOf(localClient.getNsisLevel()));
					}
					else {
						clientDTO.setNsisLevel(NSISLevel.NONE);
					}

					if (client.getPincode() != null && client.getPincode().length() > 0) {
						clientDTO.setHasPincode(true);
					}
					else if (client.getType().equals(ClientType.YUBIKEY)) {
						// for now the decision is that YubiKeys have a pincode build in due to its physical nature
						clientDTO.setHasPincode(true);
					}
					else if (client.getType().equals(ClientType.TOTP)) {
						// for now the decision is that YubiKeys have a pincode build in due to its physical nature
						clientDTO.setHasPincode(true);
					}
					else if (client.getType().equals(ClientType.TOTPH)) {
						HardwareToken hardwareToken = hardwareTokenService.getByClient(client.getDeviceId());
						if (hardwareToken != null) {
							clientDTO.setSerialnumber(hardwareToken.getSerialnumber());
						}
					}

					clients.add(clientDTO);
				}
			}
		}
	}
	
	private void addClientsByDeviceId(String deviceId, String cvr, HashSet<NSISClientDTO> clients) {
		// already added - skip
		if (clients.stream().anyMatch(c -> Objects.equals(c.getDeviceId(), deviceId))) {
			return;
		}

		Client client = clientService.getByDeviceId(deviceId);
		if (client != null) {
			NSISClientDTO clientDTO = new NSISClientDTO();
			clientDTO.setDeviceId(client.getDeviceId());
			clientDTO.setHasPincode(client.isHasPincode());
			clientDTO.setName(client.getName());
			clientDTO.setType(client.getType());
			clientDTO.setNsisLevel(client.getNsisLevel());
			clientDTO.setPrime(client.isPrime());
			clientDTO.setRoaming(client.isRoaming());
			clientDTO.setLocked(client.isLocked());
			
			if (clientDTO.isLocked()) {
				clientDTO.setLockedUntil(client.getLockedUntil());
			}

			if (client.getPincode() != null && client.getPincode().length() > 0) {
				clientDTO.setHasPincode(true);
			}
			else if (client.getType().equals(ClientType.YUBIKEY)) {
				// for now the decision is that YubiKeys have a pincode build in due to its physical nature
				clientDTO.setHasPincode(true);
			}
			else if (client.getType().equals(ClientType.TOTP)) {
				// for now the decision is that YubiKeys have a pincode build in due to its physical nature
				clientDTO.setHasPincode(true);
			}
			else if (client.getType().equals(ClientType.TOTPH)) {
				HardwareToken hardwareToken = hardwareTokenService.getByClient(client.getDeviceId());
				if (hardwareToken != null) {
					clientDTO.setSerialnumber(hardwareToken.getSerialnumber());
				}
			}

			clients.add(clientDTO);
		}
	}
}
