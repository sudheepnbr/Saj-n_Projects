package com.oih.idp.web;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.IdentityException;
import com.oih.idp.identity.SetupService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SetupController {

    private final SetupService setupService;
    private final IdpProperties properties;

    public SetupController(SetupService setupService, IdpProperties properties) {
        this.setupService = setupService;
        this.properties = properties;
    }

    @GetMapping("/setup")
    public String setup(Model model) {
        if (!setupService.setupRequired()) {
            return "redirect:/login";
        }
        model.addAttribute("displayName", properties.getDisplayName());
        return "setup/setup";
    }

    @PostMapping("/setup")
    public String complete(
            @RequestParam @NotBlank String token,
            @RequestParam @NotBlank String username,
            @RequestParam @Email String email,
            @RequestParam @NotBlank String firstName,
            @RequestParam @NotBlank String lastName,
            @RequestParam @NotBlank String password,
            Model model) {
        if (!setupService.setupRequired()) {
            return "redirect:/login";
        }
        try {
            setupService.createFirstAdmin(token, username, email, firstName, lastName, password);
            return "redirect:/login?setup";
        } catch (IdentityException ex) {
            model.addAttribute("displayName", properties.getDisplayName());
            model.addAttribute("error", ex.getMessage());
            return "setup/setup";
        }
    }
}
