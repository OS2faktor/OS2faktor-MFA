package dk.digitalidentity.os2faktor.controller;

import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.digitalidentity.os2faktor.controller.model.LocalClientListResult;
import dk.digitalidentity.os2faktor.controller.model.SsnAssignmentForm;
import dk.digitalidentity.os2faktor.dao.ClientDao;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.dao.model.LocalClient;
import dk.digitalidentity.os2faktor.security.RequireAdminRole;
import dk.digitalidentity.os2faktor.security.SecurityUtil;
import dk.digitalidentity.os2faktor.service.ClientService;
import dk.digitalidentity.os2faktor.service.LocalClientService;
import dk.digitalidentity.os2faktor.service.SsnService;
import dk.digitalidentity.os2faktor.service.StatisticService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AdminController {

	@Autowired
	protected ClientDao clientDao;

	@Autowired
	private LocalClientService localClientService;
	
	@Autowired
	private SsnService ssnService;
	
	@Autowired
	private StatisticService statisticService;

	@Autowired
	private ClientService clientService;
	
	@GetMapping("/")
	public String adminIndex(HttpServletRequest request) {
		String host = request.getHeader("host");
		if ("frontend.os2faktor.dk".equals(host)) {
			return "redirect:/ui/desktop/selfservice";
		}

		return "redirect:/admin";
	}
	
	@GetMapping("/login")
	public String loginPage() {
		return "admin/login";
	}

	@RequireAdminRole
	@GetMapping("/admin/list")
	public String listPage(Model model) {
		String cvr = SecurityUtil.getUser().getCvr();

		model.addAttribute("clients",
				localClientService.getByCvr(cvr).stream()
						.map(lc -> new LocalClientListResult(lc, clientService.getByDeviceId(lc.getDeviceId())))
						.collect(Collectors.toList()));
		
		return "admin/list";
	}
	
	@RequireAdminRole
	@GetMapping("/admin/statistics")
	public String statisticsPage(Model model) {
		String cvr = SecurityUtil.getUser().getCvr();

		model.addAttribute("statistics", statisticService.getAllByCvr(cvr));
		
		return "admin/statistics";
	}
	
	@RequireAdminRole
	@GetMapping("/admin/delete/{deviceId}")
	public String deletePage(Model model, @PathVariable("deviceId") String deviceId, RedirectAttributes redirectAttrs) {
		String cvr = SecurityUtil.getUser().getCvr();
		LocalClient localClient = localClientService.getByDeviceIdAndCvr(deviceId, cvr);
		if (localClient != null) {
			localClientService.deleteByDeviceId(deviceId);

			redirectAttrs.addFlashAttribute("notify", "success");
		}
		else {
			redirectAttrs.addFlashAttribute("notify", "failure");
		}

		return "redirect:/admin";
	}
	
	@RequireAdminRole
	@GetMapping("/admin")
	public String adminPage(Model model, @RequestParam(name = "OS2faktorID", required = false) String os2faktorId) {
		os2faktorId = normalizeOS2FaktorId(os2faktorId);
		model.addAttribute("os2faktorId", os2faktorId);

		if (os2faktorId.length() > 0) {
			Client client = clientDao.findByDeviceId(os2faktorId);
			if (client == null) {
				model.addAttribute("error", "html.admin.noresult");
			}
			else if (client.getUser() != null) {
				client = null;
				model.addAttribute("error", "html.admin.alreadymapped");
			}

			String cvr = SecurityUtil.getUser().getCvr();
			LocalClient localClient = localClientService.getByDeviceIdAndCvr(os2faktorId, cvr);
			if (localClient != null) {
				model.addAttribute("cpr", "**********");
			}

			// at this point the client is not NemID registered, but it might
			// actually be registered locally at the municipality to a SSN
			// already. As we do not store those in cleartext, we cannot show
			// the existing SSN in the UI (nor should we), so for now we simply
			// show a blank field, and allow overwriting the value
	
			model.addAttribute("client", client);
		}
		
		return "admin/admin";
	}

	@RequireAdminRole
	@PostMapping("/admin")
	public String assignSsn(Model model, SsnAssignmentForm ssnAssignmentForm) throws Exception {
		String os2faktorId = normalizeOS2FaktorId(ssnAssignmentForm.getOs2faktorId());
		model.addAttribute("os2faktorId", os2faktorId);

		Client client = clientDao.findByDeviceId(os2faktorId);
		if (client == null) {
			model.addAttribute("error", "html.admin.noresult");
		}
		else if (client.getUser() != null) {
			client = null;
			model.addAttribute("error", "html.admin.alreadymapped");
		}

		if (client == null) {
			return "admin/admin";
		}
		
		String ssn = ssnAssignmentForm.getSsn();
		if (ssn != null) {
			ssn = normalizeSsn(ssn);
		}
		
		if (ssn == null) {
			model.addAttribute("client", client);
			model.addAttribute("error", "html.admin.invalidssn");

			return "admin/admin";
		}

		String cvr = SecurityUtil.getUser().getCvr();
		LocalClient localClient = localClientService.getByDeviceIdAndCvr(os2faktorId, cvr);
		if (localClient == null) {
			localClient = new LocalClient();
			localClient.setCvr(cvr);
			localClient.setDeviceId(os2faktorId);
		}
		localClient.setAdminUserName(SecurityUtil.getUser().getName());
		localClient.setAdminUserUuid(SecurityUtil.getUser().getUuid());
		localClient.setTs(new Date());
		localClient.setSsn(ssnService.encryptAndEncodeSsn(ssn));
		localClientService.save(localClient);

		return "admin/success";
	}

	private String normalizeOS2FaktorId(String os2faktorId) {
		if (os2faktorId == null) {
			return "";
		}

		if (os2faktorId.length() == 12) {
			os2faktorId = os2faktorId.substring(0, 3) + "-" +
						  os2faktorId.substring(3, 6) + "-" +
						  os2faktorId.substring(6, 9) + "-" +
						  os2faktorId.substring(9, 12);
		}

		return os2faktorId;
	}
	
	private String normalizeSsn(String ssn) {
		if (ssn == null) {
			return null;
		}
		
		ssn = ssn.replace("-", "");
		
		if (ssn.length() != 10) {
			return null;
		}

		for (char c : ssn.toCharArray()) {
			if (!Character.isDigit(c)) {
				return null;
			}
		}

		String day = ssn.substring(0, 2);
		String month = ssn.substring(2, 4);

		try {
			long dayValue = Long.parseLong(day);
			long monthValue = Long.parseLong(month);
			
			if (dayValue < 0 || dayValue > 31) {
				return null;
			}
			else if (monthValue < 0 || monthValue > 12) {
				return null;
			}
		}
		catch (Exception ex) {
			return null;
		}

		return ssn;
	}
}
