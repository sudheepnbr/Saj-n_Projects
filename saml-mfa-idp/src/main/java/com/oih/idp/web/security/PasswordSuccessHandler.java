package com.oih.idp.web.security;

import com.oih.idp.identity.IdentityService;
import com.oih.idp.identity.domain.User;
import com.oih.idp.mfa.MfaService;
import com.oih.idp.session.MfaPendingAuthenticationToken;
import com.oih.idp.session.SsoSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class PasswordSuccessHandler implements AuthenticationSuccessHandler {

    private final MfaService mfa;
    private final IdentityService identity;
    private final SsoSessionService sso;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public PasswordSuccessHandler(MfaService mfa, IdentityService identity, SsoSessionService sso) {
        this.mfa = mfa;
        this.identity = identity;
        this.sso = sso;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        request.changeSessionId();
        IdpUserPrincipal principal = (IdpUserPrincipal) authentication.getPrincipal();
        User user = identity.requireUser(principal.getUserId());
        boolean enrolled = mfa.isEnrolled(user.getId());
        if (mfa.platformEnabled() || enrolled) {
            boolean enrollment = !enrolled;
            var pending = new MfaPendingAuthenticationToken(principal, enrollment);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(pending);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, request, response);
            response.sendRedirect(enrollment ? "/mfa/enroll" : "/mfa/verify");
            return;
        }
        sso.establish(principal, request);
        Object saved = request.getSession().getAttribute("SAML_CONTINUE");
        response.sendRedirect(saved instanceof String url ? url : "/account");
    }
}
