package com.oih.idp.session;

import org.springframework.security.core.Authentication;

public final class AuthenticationStates {

    private AuthenticationStates() {
    }

    public static boolean isMfaPending(Authentication authentication) {
        return authentication instanceof MfaPendingAuthenticationToken;
    }

    public static boolean isFullyAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !isMfaPending(authentication);
    }
}
