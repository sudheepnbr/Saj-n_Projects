package com.oih.idp.account.web;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.IdentityException;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.User;
import com.oih.idp.web.security.IdpUserPrincipal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final IdentityService identity;
    private final IdpProperties properties;

    public AccountController(IdentityService identity, IdpProperties properties) {
        this.identity = identity;
        this.properties = properties;
    }

    @GetMapping
    public String account(@AuthenticationPrincipal IdpUserPrincipal principal, Model model) {
        User user = identity.requireUser(principal.getUserId());
        model.addAttribute("displayName", properties.getDisplayName());
        model.addAttribute("user", user);
        return "account/home";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal IdpUserPrincipal principal,
            @RequestParam @Email String email,
            @RequestParam @NotBlank String firstName,
            @RequestParam @NotBlank String lastName,
            RedirectAttributes redirect) {
        try {
            identity.updateProfile(principal.getUserId(), email, firstName, lastName);
            redirect.addFlashAttribute("info", "Profile updated.");
        } catch (IdentityException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/account";
    }

    @PostMapping("/password")
    public String changePassword(
            @AuthenticationPrincipal IdpUserPrincipal principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            RedirectAttributes redirect) {
        try {
            identity.changePassword(principal.getUserId(), currentPassword, newPassword);
            redirect.addFlashAttribute("info", "Password changed.");
        } catch (IdentityException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/account";
    }
}
