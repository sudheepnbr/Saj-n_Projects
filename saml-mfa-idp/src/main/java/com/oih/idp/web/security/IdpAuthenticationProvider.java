package com.oih.idp.web.security;

import com.oih.idp.identity.AuthenticationService;
import com.oih.idp.identity.AuthenticationService.AuthResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class IdpAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationService authenticationService;

    public IdpAuthenticationProvider(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        HttpServletRequest request = currentRequest();
        AuthResult result = authenticationService.authenticate(username, password, request);
        if (!result.success()) {
            throw switch (result.failureCategory()) {
                case "LOCKED", "SOURCE_LOCKED" -> new LockedException(result.message());
                case "DISABLED" -> new DisabledException(result.message());
                default -> new BadCredentialsException(result.message());
            };
        }
        IdpUserPrincipal principal = new IdpUserPrincipal(result.user());
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
