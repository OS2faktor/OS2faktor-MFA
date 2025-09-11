package dk.digitalidentity.os2faktor.controller;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class LoginControllerAdvice {

    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseBody
    public String handleFailedLogin1(Model model, final InsufficientAuthenticationException ex) {
    	model.addAttribute("message", ex.getMessage());
    	
        return "redirect:/login-error";
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseBody
    public String handleFailedLogin2(Model model, final InsufficientAuthenticationException ex) {
    	model.addAttribute("message", ex.getMessage());
    	
        return "redirect:/login-error";
    }
}
