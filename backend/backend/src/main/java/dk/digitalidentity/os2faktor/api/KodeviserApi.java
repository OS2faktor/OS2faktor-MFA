package dk.digitalidentity.os2faktor.api;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.os2faktor.api.dto.HardwareTokenDTO;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.HardwareToken;
import dk.digitalidentity.os2faktor.dao.model.LoginServiceProvider;
import dk.digitalidentity.os2faktor.dao.model.enums.RegistrationStatus;
import dk.digitalidentity.os2faktor.security.AuthorizedLoginServiceProviderHolder;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.HardwareTokenService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
public class KodeviserApi {

	@Autowired
	private ClientService clientService;

	@Autowired
	private HardwareTokenService hardwareTokenService;

	@GetMapping("/api/login/kodeviser/device")
	public ResponseEntity<?> getHardwareToken(@RequestParam("serial") String serial) {
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		HardwareTokenDTO result = new HardwareTokenDTO();

		HardwareToken token = hardwareTokenService.getBySerialnumber(serial);
		if (token == null) {
			// device not found
			result.setFound(false);
			result.setStatus(RegistrationStatus.DEVICE_NOT_FOUND);
		}
		else {
			result.setFound(true);
			
			if (!token.isRegistered() || !StringUtils.hasLength(token.getClientDeviceId())) {
				// device found but it's not registered
				result.setStatus(RegistrationStatus.DEVICE_NOT_REGISTERED);
			}
			else {
				String cvr = loginServiceProvider.getCvr();
				
				if (!Objects.equals(token.getRegisteredToCvr(), cvr)) {
					// device belongs to other municipality
					result.setStatus(RegistrationStatus.REGISTERED_TO_SOMEONE_ELSE);
				}
				else {
					result.setStatus(RegistrationStatus.REGISTERED_TO_YOU);
				}
			}
		}
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@GetMapping("/api/login/kodeviser/deregister")
	public ResponseEntity<?> deregister(@RequestParam("serial") String serial) {
		LoginServiceProvider loginServiceProvider = AuthorizedLoginServiceProviderHolder.getLoginServiceProvider();
		if (loginServiceProvider == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		if (serial != null) {
			HardwareToken token = hardwareTokenService.getBySerialnumber(serial);

			if (token == null) {
				// device not found, but that is ok
				return new ResponseEntity<>(HttpStatus.OK);
			}
			else {
				if (!token.isRegistered() || token.getClientDeviceId() == null) {
					// device found but it's not registered, also ok
					return new ResponseEntity<>(HttpStatus.OK);
				}
				else {
					String cvr = loginServiceProvider.getCvr();
					if (!Objects.equals(token.getRegisteredToCvr(), cvr)) {
						// device belongs to other municipality
						log.warn("Attempting to deregister " + serial + ", but caller is from " + cvr + ", and token is registered to " + token.getRegisteredToCvr());
						return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
					}
					else {
						// Disable the client
						Client client = clientService.getByDeviceId(token.getClientDeviceId());
						if (client != null) {
							client.setDisabled(true);
							clientService.save(client);
						}

						// Clear the hardware token assignment
						token.setRegistered(false);
						token.setRegisteredToCpr(null);
						token.setRegisteredToCvr(null);
						token.setClientDeviceId(null);

						hardwareTokenService.save(token);
					}
				}
			}
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
