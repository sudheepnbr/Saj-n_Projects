package com.oih.idp.admin.web;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.IdentityException;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.Role;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','AUDITOR')")
public class AdminController {

    private final IdentityService identity;
    private final IdpProperties properties;

    public AdminController(IdentityService identity, IdpProperties properties) {
        this.identity = identity;
        this.properties = properties;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("displayName", properties.getDisplayName());
        model.addAttribute("users", identity.listUsers());
        return "admin/list";
    }

    @GetMapping("/users/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("displayName", properties.getDisplayName());
        return "admin/create";
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String create(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String password,
            @RequestParam(defaultValue = "USER") String role,
            RedirectAttributes redirect) {
        try {
            Set<String> roles = new LinkedHashSet<>();
            roles.add(Role.USER);
            roles.add(role);
            identity.createUser(username, email, firstName, lastName, password, roles);
            redirect.addFlashAttribute("info", "User created.");
            return "redirect:/admin";
        } catch (IdentityException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @PostMapping("/users/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public String disable(@PathVariable UUID id, RedirectAttributes redirect) {
        identity.disable(id);
        redirect.addFlashAttribute("info", "User disabled.");
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public String enable(@PathVariable UUID id, RedirectAttributes redirect) {
        identity.enable(id);
        redirect.addFlashAttribute("info", "User enabled.");
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public String unlock(@PathVariable UUID id, RedirectAttributes redirect) {
        identity.unlock(id);
        redirect.addFlashAttribute("info", "User unlocked.");
        return "redirect:/admin";
    }
}
