package dk.digitalidentity.os2faktor.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import dk.digitalidentity.os2faktor.controller.ControllerUtil.PageTarget;
import dk.digitalidentity.os2faktor.controller.model.ErrorType;
import dk.digitalidentity.os2faktor.controller.model.FailedFlow;
import dk.digitalidentity.os2faktor.controller.model.PinRegistration;
import dk.digitalidentity.os2faktor.controller.validators.PinValidator;
import dk.digitalidentity.os2faktor.dao.model.Client;
import dk.digitalidentity.os2faktor.security.ClientSecurityFilter;

@Controller
public class PinController extends BaseController {

	@Autowired
	private PinValidator pinValidator;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(pinValidator);
	}

	@GetMapping("/ui/pin/register")
	public String registerPinGet(Model model, HttpServletRequest request) {
		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.PIN, request);
		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		Client client = clientOrErrorPage.client;
		if (!StringUtils.isEmpty(client.getPincode())) {
			return ControllerUtil.handleError(model, FailedFlow.PIN, ErrorType.PIN_ALREADY_ASSIGNED, "Client " + client.getDeviceId() + " has already registered a PIN number.", PageTarget.APP);
		}

		PinRegistration pinRegistration = new PinRegistration();

		model.addAttribute("registration", pinRegistration);

		return "pin/register";
	}

	@PostMapping(value = "/ui/pin/register")
	public String registerPinPost(@ModelAttribute("registration") @Valid PinRegistration pinRegistration, BindingResult bindingResult, HttpServletRequest request, Model model) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("registration", pinRegistration);

			return "pin/register";
		}

		ClientOrErrorPage clientOrErrorPage = authenticateClient(model, FailedFlow.PIN, request);

		if (clientOrErrorPage.errorPage != null) {
			return clientOrErrorPage.errorPage;
		}

		Client client = clientOrErrorPage.client;
		if (!StringUtils.isEmpty(client.getPincode())) {
			return ControllerUtil.handleError(model, FailedFlow.PIN, ErrorType.PIN_ALREADY_ASSIGNED, "Client " + client.getDeviceId() + " has already registered a PIN number.", PageTarget.APP);
		}
		
		client.setPincode(pinRegistration.getPin());
		clientDao.save(client);
		
		// logout client, the operation has completed successfully,
		// and should the client attempt to register a new pin, we want to
		// make sure they authenticate first
		request.getSession().removeAttribute(ClientSecurityFilter.SESSION_CLIENT);

		return "redirect:/ui/pin/successPage?status=true";
	}

	@RequestMapping("/ui/pin/successPage")
	public String successPage() {
		return "pin/success";
	}
}
