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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JIT-Provisioning (SCENARIOS.md, сценарий 1).
 * <p>
 * Стоит сразу после аутентификации по JWT и до контроллеров, поэтому к моменту
 * входа в бизнес-логику строка {@code accounts} гарантированно существует.
 * Это снимает необходимость проверять "а есть ли аккаунт" в каждом сервисе.
 * <p>
 * Домен вызывается через порт {@link AccountProvisioner}, а не напрямую:
 * пока задача B5 не реализована, бина нет, и фильтр просто пропускает запрос —
 * иначе приложение не поднялось бы до конца Фазы B.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JitProvisioningFilter extends OncePerRequestFilter {

    /** ObjectProvider, а не прямая инъекция: до задачи B5 реализации порта нет. */
    private final ObjectProvider<AccountProvisioner> accountProvisioner;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            provisionQuietly(jwtAuthentication.getToken().getSubject());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Провал провижининга не должен превращаться в 500 на пользовательском запросе:
     * до бизнес-логики дело всё равно дойдёт, а отсутствие строки всплывёт там
     * осмысленной доменной ошибкой.
     */
    private void provisionQuietly(String subject) {
        AccountProvisioner provisioner = accountProvisioner.getIfAvailable();
        if (provisioner == null) {
            log.debug("AccountProvisioner ещё не реализован (задача B5), JIT пропущен");
            return;
        }

        try {
            provisioner.provision(UUID.fromString(subject));
        } catch (IllegalArgumentException e) {
            log.warn("claim sub не является UUID: {}", subject);
        } catch (RuntimeException e) {
            log.error("JIT-provisioning не удался для sub={}", subject, e);
        }
    }

    /** На служебных эндпоинтах аккаунтов не бывает — не ходим в базу зря. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
