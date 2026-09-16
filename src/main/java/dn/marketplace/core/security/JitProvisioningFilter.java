package dn.marketplace.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JIT-Provisioning (SCENARIOS.md, сценарий 1).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JitProvisioningFilter extends OncePerRequestFilter {

    private final ObjectProvider<AccountProvisioner> accountProvisioner;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            provisionQuietly(jwtAuthentication.getToken());
        }

        filterChain.doFilter(request, response);
    }

    private void provisionQuietly(Jwt jwt) {
        AccountProvisioner provisioner = accountProvisioner.getIfAvailable();
        if (provisioner == null) {
            log.debug("AccountProvisioner недоступен, JIT пропущен");
            return;
        }

        String subject = jwt.getSubject();
        try {
            String username = jwt.getClaimAsString("preferred_username");
            if (username == null || username.isBlank()) {
                // По дизайну claim есть всегда; если его нет — сломан mapper клиента в Keycloak
                log.warn("В JWT нет preferred_username, как username используется sub={}", subject);
                username = subject;
            }
            provisioner.provision(UUID.fromString(subject), username);
        } catch (IllegalArgumentException e) {
            log.warn("claim sub не является UUID: {}", subject);
        } catch (RuntimeException e) {
            log.error("JIT-provisioning не удался для sub={}", subject, e);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
