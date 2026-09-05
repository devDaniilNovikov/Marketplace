package dn.marketplace.core.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Достаёт роли из claim {@code realm_access.roles} токена Keycloak
 * и превращает их в {@code ROLE_*} для {@code hasRole(...)}.
 * <p>
 * Реализует решение №4 (строгий SSOT): роли живут только в токене, в таблице
 * {@code accounts} колонки {@code role} нет. Единственный источник правды по
 * правам — Keycloak; монолит владеет лишь бизнес-статусом аккаунта.
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);
        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get(ROLES);
        if (!(roles instanceof Collection<?> roleCollection)) {
            return List.of();
        }

        return roleCollection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                // служебные роли Keycloak в доменную модель прав не пускаем
                .filter(role -> !role.startsWith("default-roles"))
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();
    }
}
