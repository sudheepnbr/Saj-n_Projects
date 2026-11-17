package com.oih.idp.admin.web;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.keys.KeyLifecycleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/keys")
@PreAuthorize("hasRole('ADMIN')")
public class KeyAdminController {

    private final KeyLifecycleService keys;
    private final IdpProperties properties;

    public KeyAdminController(KeyLifecycleService keys, IdpProperties properties) {
        this.keys = keys;
        this.properties = properties;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("displayName", properties.getDisplayName());
        model.addAttribute("keys", keys.all());
        return "admin/keys";
    }

    @PostMapping("/next")
    public String next() {
        keys.generateNext();
        return "redirect:/admin/keys";
    }

    @PostMapping("/activate")
    public String activate() {
        keys.activateNext();
        return "redirect:/admin/keys";
    }
}
