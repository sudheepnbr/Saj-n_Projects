package com.oih.idp.ops;

import com.oih.idp.config.IdpProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SamlBodySizeFilter extends OncePerRequestFilter {

    private final IdpProperties properties;

    public SamlBodySizeFilter(IdpProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI() != null
                && request.getRequestURI().startsWith("/saml2/")
                && request.getContentLength() > properties.getSaml().getMaxBodyBytes()) {
            response.sendError(400, "SAML request rejected");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
