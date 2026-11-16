package com.oih.idp.web.security;

import com.oih.idp.identity.repo.UserRepository;
import com.oih.idp.session.AuthenticationStates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final IdpAuthenticationProvider authenticationProvider;
    private final UserRepository users;
    private final PasswordSuccessHandler successHandler;

    public SecurityConfig(
            IdpAuthenticationProvider authenticationProvider,
            UserRepository users,
            PasswordSuccessHandler successHandler) {
        this.authenticationProvider = authenticationProvider;
        this.users = users;
        this.successHandler = successHandler;
    }

    @Bean
    AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        http.authenticationManager(authenticationManager)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**",
                                "/login",
                                "/error",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/saml2/idp/metadata")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/saml2/idp/sso", "/saml2/idp/slo")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/saml2/idp/sso", "/saml2/idp/slo")
                        .permitAll()
                        .requestMatchers("/setup", "/setup/**")
                        .access((authentication, context) ->
                                new AuthorizationDecision(users.count() == 0))
                        .requestMatchers("/mfa/**")
                        .authenticated()
                        .requestMatchers("/admin/**")
                        .access((authentication, context) -> new AuthorizationDecision(
                                AuthenticationStates.isFullyAuthenticated(authentication.get())
                                        && authentication.get().getAuthorities().stream()
                                                .anyMatch(a -> a.getAuthority().startsWith("ROLE_ADMIN")
                                                        || a.getAuthority().equals("ROLE_OPERATOR")
                                                        || a.getAuthority().equals("ROLE_AUDITOR"))))
                        .requestMatchers("/account/**", "/saml2/idp/initiate", "/saml2/idp/continue")
                        .access((authentication, context) ->
                                new AuthorizationDecision(AuthenticationStates.isFullyAuthenticated(authentication.get())))
                        .anyRequest()
                        .access((authentication, context) -> new AuthorizationDecision(
                                AuthenticationStates.isFullyAuthenticated(authentication.get()))))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler("/login?error")))
                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("OIHIDPSESS"))
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/saml2/idp/sso"),
                        PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/saml2/idp/slo")))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(policy ->
                                policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(policy -> policy.policy("geolocation=(), microphone=(), camera=()"))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; form-action 'self' https: http:; frame-ancestors 'none'")));
        return http.build();
    }
}
