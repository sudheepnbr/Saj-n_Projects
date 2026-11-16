package com.oih.idp.account.web;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.IdentityException;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import com.oih.idp.identity.domain.User;
import com.oih.idp.mfa.MfaService;
import com.oih.idp.sp.ServiceProvider;
import com.oih.idp.sp.ServiceProviderRegistry;
import com.oih.idp.web.security.IdpUserPrincipal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
    private final ServiceProviderRegistry registry;
    private final MfaService mfa;

    public AccountController(
            IdentityService identity,
            IdpProperties properties,
            ServiceProviderRegistry registry,
            MfaService mfa) {
        this.identity = identity;
        this.properties = properties;
        this.registry = registry;
        this.mfa = mfa;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String account(@AuthenticationPrincipal IdpUserPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        User user = identity.requireUser(principal.getUserId());
        List<AppLink> applications = registry.list().stream()
                .filter(ServiceProvider::isEnabled)
                .map(sp -> new AppLink(sp.getEntityId(), sp.getDisplayName()))
                .toList();
        model.addAttribute("displayName", properties.getDisplayName());
        model.addAttribute("user", new ProfileView(user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName()));
        model.addAttribute("applications", applications);
        model.addAttribute(
                "admin",
                user.hasRole(Role.ADMIN) || user.hasRole(Role.OPERATOR) || user.hasRole(Role.AUDITOR));
        model.addAttribute("mfaEnrolled", mfa.isEnrolled(user.getId()));
        model.addAttribute("mfaPlatform", mfa.platformEnabled());
        return "account/home";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal IdpUserPrincipal principal,
            @RequestParam @Email String email,
            @RequestParam @NotBlank String firstName,
            @RequestParam @NotBlank String lastName,
            RedirectAttributes redirect) {
        if (principal == null) {
            return "redirect:/login";
        }
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
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            identity.changePassword(principal.getUserId(), currentPassword, newPassword);
            redirect.addFlashAttribute("info", "Password changed.");
        } catch (IdentityException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/account";
    }

    public record ProfileView(String username, String email, String firstName, String lastName) {}

    public record AppLink(String entityId, String displayName) {}
}
