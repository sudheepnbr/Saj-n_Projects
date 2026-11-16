package com.oih.idp.mfa.web;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.identity.IdentityException;
import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.User;
import com.oih.idp.mfa.MfaService;
import com.oih.idp.session.MfaPendingAuthenticationToken;
import com.oih.idp.session.SsoSessionService;
import com.oih.idp.web.security.IdpUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mfa")
public class MfaController {

    private final MfaService mfa;
    private final IdentityService identity;
    private final SsoSessionService sso;
    private final IdpProperties properties;

    public MfaController(
            MfaService mfa, IdentityService identity, SsoSessionService sso, IdpProperties properties) {
        this.mfa = mfa;
        this.identity = identity;
        this.sso = sso;
        this.properties = properties;
    }

    @GetMapping("/enroll")
    public String enroll(
            @AuthenticationPrincipal IdpUserPrincipal principal, HttpServletRequest request, Model model) {
        User user = identity.requireUser(principal.getUserId());
        var enrollment = mfa.startEnrollment(user);
        request.getSession().setAttribute("MFA_ENROLL_SECRET", enrollment.otpauthUri());
        model.addAttribute("displayName", properties.getDisplayName());
        model.addAttribute("otpauth", enrollment.otpauthUri());
        model.addAttribute("qr", enrollment.qrPngBase64());
        model.addAttribute("recoveryCodes", enrollment.recoveryCodes());
        return "mfa/enroll";
    }

    @PostMapping("/enroll")
    public String confirmEnroll(
            @AuthenticationPrincipal IdpUserPrincipal principal,
            @RequestParam String code,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        User user = identity.requireUser(principal.getUserId());
        try {
            mfa.confirmEnrollment(user, code);
            complete(principal, request, response);
            Object saved = request.getSession().getAttribute("SAML_CONTINUE");
            return "redirect:" + (saved instanceof String url ? url : "/account");
        } catch (IdentityException ex) {
            model.addAttribute("displayName", properties.getDisplayName());
            model.addAttribute("error", ex.getMessage());
            return "mfa/enroll";
        }
    }

    @GetMapping("/verify")
    public String verify(Model model) {
        model.addAttribute("displayName", properties.getDisplayName());
        return "mfa/verify";
    }

    @PostMapping("/verify")
    public String check(
            @AuthenticationPrincipal IdpUserPrincipal principal,
            @RequestParam String code,
            @RequestParam(defaultValue = "false") boolean recovery,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        User user = identity.requireUser(principal.getUserId());
        boolean ok = recovery ? mfa.consumeRecoveryCode(user, code) : mfa.verifyTotp(user, code);
        if (!ok) {
            model.addAttribute("displayName", properties.getDisplayName());
            model.addAttribute("error", "Invalid code.");
            return "mfa/verify";
        }
        if (recovery && properties.getMfa().isRecoveryRequiresReset()) {
            var pending = new MfaPendingAuthenticationToken(principal, true);
            SecurityContextHolder.getContext().setAuthentication(pending);
            return "redirect:/mfa/enroll";
        }
        complete(principal, request, response);
        Object saved = request.getSession().getAttribute("SAML_CONTINUE");
        return "redirect:" + (saved instanceof String url ? url : "/account");
    }

    private void complete(IdpUserPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        request.changeSessionId();
        var full = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(full);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        sso.establish(principal, request);
    }
}
