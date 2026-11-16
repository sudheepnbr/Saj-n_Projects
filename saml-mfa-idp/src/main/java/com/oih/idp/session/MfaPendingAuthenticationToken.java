package com.oih.idp.session;

import com.oih.idp.web.security.IdpUserPrincipal;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class MfaPendingAuthenticationToken extends AbstractAuthenticationToken {

    public static final String AUTHORITY = "MFA_PENDING";

    private final IdpUserPrincipal principal;
    private final boolean enrollment;

    public MfaPendingAuthenticationToken(IdpUserPrincipal principal, boolean enrollment) {
        super(List.of(new SimpleGrantedAuthority(AUTHORITY)));
        this.principal = principal;
        this.enrollment = enrollment;
        setAuthenticated(true);
    }

    public boolean isEnrollment() {
        return enrollment;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public IdpUserPrincipal getPrincipal() {
        return principal;
    }
}
