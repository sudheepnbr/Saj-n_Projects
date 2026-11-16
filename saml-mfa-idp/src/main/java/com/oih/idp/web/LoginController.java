package com.oih.idp.web;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.SetupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final IdpProperties properties;
    private final SetupService setupService;

    public LoginController(IdpProperties properties, SetupService setupService) {
        this.properties = properties;
        this.setupService = setupService;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        if (setupService.setupRequired()) {
            return "redirect:/setup";
        }
        model.addAttribute("displayName", properties.getDisplayName());
        Object exception = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (exception instanceof AuthenticationException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        if (request.getParameter("logout") != null) {
            model.addAttribute("info", "You have been signed out.");
        }
        return "login/login";
    }

    @GetMapping("/")
    public String home() {
        if (setupService.setupRequired()) {
            return "redirect:/setup";
        }
        return "redirect:/account";
    }
}
