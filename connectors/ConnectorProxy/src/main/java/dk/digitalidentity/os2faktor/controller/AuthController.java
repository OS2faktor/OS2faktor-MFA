package dk.digitalidentity.os2faktor.controller;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.os2faktor.controller.model.LoginRequest;
import dk.digitalidentity.os2faktor.model.Client;
import dk.digitalidentity.os2faktor.model.enums.ClientType;
import dk.digitalidentity.os2faktor.service.LdapService;
import dk.digitalidentity.os2faktor.service.OS2faktorService;
import dk.digitalidentity.os2faktor.service.SignatureService;
import dk.digitalidentity.os2faktor.service.model.ClientSearchParams;
import dk.digitalidentity.os2faktor.service.model.SubscriptionInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class AuthController {
	private static final String SESSION_LOGIN_INFO = "SESSION_LOGIN_INFO";
	private static final String SESSION_REQUEST = "SESSION_REQUEST";
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private SignatureService signatureService;
	
	@Autowired
	private LdapService ldapService;
	
	@Autowired
	private OS2faktorService os2faktorService;
	
	@Value("${login.backend.baseurl}")
	private String baseUrl;

	@Value("${login.backend.apikey}")
	private String apiKey;
	
	@Value("${login.enabled}")
	private boolean enabled;
	
	@Value("${login.connector.requirepin}")
	private boolean requirePin;

	@GetMapping("/auth/login")
	public String login(Model model, @RequestParam(name = "request") String request, HttpServletRequest servletRequest) {
		if (!enabled) {
			throw new RuntimeException("OS2faktor login is not enabled!");
		}

		List<Client> clients = null;
		try {
			clients = getClientsFromRequest(request);
		}
		catch (Exception ex) {
			model.addAttribute("message", "Unable to parse request data: " + request);

			return "auth/error";
		}

		if (clients.size() > 0) {
			servletRequest.getSession().setAttribute(SESSION_REQUEST, request);
			
			List<Client> sortedClients = new ArrayList<Client>(clients);
			Collections.sort(sortedClients, new Comparator<Client>() {

				@Override
				public int compare(Client o1, Client o2) {
					return o1.getType().compareTo(o2.getType());
				}
			});

			model.addAttribute("clients", sortedClients);
		}
		else {
			model.addAttribute("noClients", "true");
		}
				
		return "auth/auth";
	}

	@GetMapping("/auth/init/{deviceId}")
	public String initLogin(Model model, @PathVariable("deviceId") String deviceId, HttpServletRequest request) {
		if (!enabled) {
			throw new RuntimeException("OS2faktor login is not enabled!");
		}

		String originalRequest = getRequestFromSession(request);
		if (request == null) {
			log.warn("Missing request on session!");
			model.addAttribute("message","Missing request on session!");
			return "auth/error";
		}

		List<Client> clients = null;
		try {
			clients = getClientsFromRequest(originalRequest);
		}
		catch (Exception ex) {
			model.addAttribute("message", "Unable to parse request data: " + request);

			return "auth/error";
		}
		
		boolean found = false;
		for (Client client : clients) {
			if (client.getDeviceId().equals(deviceId)) {
				found = true;
				break;
			}
		}
		
		if (!found) {
			model.addAttribute("message", "Client not owned by user: " + deviceId);

			return "auth/error";
		}

		// get a challenge for this client from the backend
		SubscriptionInfo info = os2faktorService.getChallenge(deviceId);

		// TODO: Notification class should be serializeable, so we can store it in the session
		// store the whole challenge object on the session for later verification
		request.getSession().setAttribute(SESSION_LOGIN_INFO, info);

		model.addAttribute("pollingKey", info.getPollingKey());
		model.addAttribute("challenge", info.getChallenge());
		model.addAttribute("baseUrl", baseUrl);

		return "auth/await";
	}

	@PostMapping("/auth/")
	public String verifyLogin(Model model, HttpServletRequest servletRequest) {
		// 0: error, 1: approved, 2: rejected

		if (!enabled) {
			throw new RuntimeException("OS2faktor login is not enabled!");
		}

		String request = getRequestFromSession(servletRequest);
		if (request == null) {
			log.warn("Missing request on session!");
			model.addAttribute("message","Missing request on session!");
			return "auth/error";
		}

		SubscriptionInfo info = (SubscriptionInfo) servletRequest.getSession().getAttribute(SESSION_LOGIN_INFO);
		if (info != null) {
			boolean success = os2faktorService.loginCompleted(info);

			if (success) {
				String signature = null;
				try {
					signature = signatureService.sign(request);
					return "redirect:/auth/result?success=1&signature="+signature;
				}
				catch (Exception ex) {
					log.error("Signature generation failed", ex);
					model.addAttribute("message","Signature generation failed");
					return "auth/error";
				}
			}
			else {
				return "redirect:/auth/result?success="+2;
			}
		}

		return "redirect:/auth/result?success="+0;
	}

	@GetMapping("/auth/result")
	private String resultPage(Model model, @RequestParam(name = "success") String success, HttpServletRequest servletRequest) {
		model.addAttribute("success", success);
		
		return "/auth/result";
	}

	private String getRequestFromSession(HttpServletRequest servletRequest) {
		Object o = servletRequest.getSession().getAttribute(SESSION_REQUEST);
		if (o != null && o instanceof String) {
			return (String) o;
		}

		return null;
	}

	private String getSAMAccountName(String request) throws Exception {
		byte[] raw = Base64.getDecoder().decode(request);
		LoginRequest loginRequest = objectMapper.readValue(raw, LoginRequest.class);

		if (loginRequest.getUid().contains("@")) {
			return loginRequest.getUid().substring(0, loginRequest.getUid().indexOf("@"));
		}
		else if (loginRequest.getUid().contains("\\")) {
			return loginRequest.getUid().substring(loginRequest.getUid().lastIndexOf("\\") + 1);
		}

		// default just return the full value
		return loginRequest.getUid();
	}
	
	private List<Client> getClients(String sAMAccountName) throws Exception {
		ClientSearchParams params = ldapService.getUserDetails(sAMAccountName);
		if (params == null) {
			return null;
		}
		
		// Calling backend api for list of clients
		List<Client> clients = os2faktorService.getClients(params);
		
		// filter out yubikeys
		if (clients != null) {
			clients = clients.stream().filter(c -> !c.getType().equals(ClientType.YUBIKEY)).collect(Collectors.toList());
			
			if (requirePin) {
				clients = clients.stream().filter(c -> c.isHasPincode()).collect(Collectors.toList());
			}
		}
		else {
			clients = new ArrayList<>();
		}

		return clients;
	}
	
	private List<Client> getClientsFromRequest(String request) throws Exception {
		String sAMAccountName = null;
		try {
			sAMAccountName = getSAMAccountName(request);
		}
		catch (Exception ex) {
			throw new Exception("Unable to parse request data: " + request, ex);
		}

		List<Client> clients = getClients(sAMAccountName);
		if (clients == null) {
			throw new Exception("Unable to fetch search params from AD. sAMAccountName: "+ sAMAccountName);
		}

		return clients;
	}
}
