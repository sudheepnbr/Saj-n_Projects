package com.oih.idp.admin.web;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.sp.ServiceProviderRegistry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/sp")
@PreAuthorize("hasRole('ADMIN')")
public class SpAdminController {

    private final ServiceProviderRegistry registry;
    private final IdpProperties properties;

    public SpAdminController(ServiceProviderRegistry registry, IdpProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("displayName", properties.getDisplayName());
        model.addAttribute("sps", registry.list());
        return "admin/sp";
    }

    @PostMapping
    public String create(
            @RequestParam String entityId,
            @RequestParam String displayName,
            @RequestParam String acs,
            @RequestParam(required = false) String slo,
            @RequestParam(defaultValue = "false") boolean wantSigned,
            @RequestParam(defaultValue = "false") boolean encrypt,
            RedirectAttributes redirect) {
        registry.register(entityId, displayName, acs, slo, wantSigned, encrypt, null, null);
        redirect.addFlashAttribute("info", "Service provider registered. ACS allow-list is exact-match.");
        return "redirect:/admin/sp";
    }
}
