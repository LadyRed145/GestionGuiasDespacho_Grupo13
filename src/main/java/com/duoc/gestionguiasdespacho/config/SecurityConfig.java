package com.duoc.gestionguiasdespacho.config;

import com.duoc.gestionguiasdespacho.security.SecurityRoles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf ->
                        csrf.disable()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/guias/*/descargar"
                        )
                        .hasAnyAuthority(
                                SecurityRoles.ROLE_GUIA_DOWNLOAD,
                                SecurityRoles.ROLE_GUIA_DESPACHO,
                                "SCOPE_"
                                        + SecurityRoles.GUIA_DOWNLOAD,
                                "SCOPE_"
                                        + SecurityRoles.GUIA_DESPACHO
                        )

                        .requestMatchers(
                                "/api/guias/cola/**"
                        )
                        .hasAnyAuthority(
                                SecurityRoles.ROLE_GUIA_DESPACHO,
                                "SCOPE_"
                                        + SecurityRoles.GUIA_DESPACHO
                        )

                        .requestMatchers(
                                "/api/guias/**"
                        )
                        .hasAnyAuthority(
                                SecurityRoles.ROLE_GUIA_DESPACHO,
                                "SCOPE_"
                                        + SecurityRoles.GUIA_DESPACHO
                        )

                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken>
    jwtAuthenticationConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities =
                    extractAuthorities(jwt);

            return new JwtAuthenticationToken(
                    jwt,
                    authorities
            );
        };
    }

    private Collection<GrantedAuthority>
    extractAuthorities(
            Jwt jwt
    ) {
        Set<GrantedAuthority> authorities =
                new HashSet<>();

        addRolesFromClaim(
                jwt,
                authorities,
                "roles"
        );

        addRolesFromClaim(
                jwt,
                authorities,
                "groups"
        );

        addRolesFromClaim(
                jwt,
                authorities,
                "extension_roles"
        );

        addRolesFromClaim(
                jwt,
                authorities,
                "extension_Roles"
        );

        addScopes(
                jwt,
                authorities,
                "scp"
        );

        addScopes(
                jwt,
                authorities,
                "scope"
        );

        return Set.copyOf(authorities);
    }

    private void addRolesFromClaim(
            Jwt jwt,
            Set<GrantedAuthority> authorities,
            String claimName
    ) {
        Object claim = jwt.getClaim(
                claimName
        );

        if (claim instanceof Collection<?> values) {
            for (Object value : values) {
                agregarRol(
                        authorities,
                        value
                );
            }

            return;
        }

        if (claim instanceof String value) {
            agregarRolesDesdeTexto(
                    authorities,
                    value
            );
        }
    }

    private void agregarRolesDesdeTexto(
            Set<GrantedAuthority> authorities,
            String valor
    ) {
        if (valor.isBlank()) {
            return;
        }

        String[] roles =
                valor.split("[,\\s]+");

        for (String role : roles) {
            agregarRol(
                    authorities,
                    role
            );
        }
    }

    private void agregarRol(
            Set<GrantedAuthority> authorities,
            Object valor
    ) {
        if (valor == null) {
            return;
        }

        String role =
                String.valueOf(valor).trim();

        if (role.isBlank()) {
            return;
        }

        String roleNormalizado =
                normalizarRol(role);

        authorities.add(
                new SimpleGrantedAuthority(
                        roleNormalizado
                )
        );
    }

    private void addScopes(
            Jwt jwt,
            Set<GrantedAuthority> authorities,
            String claimName
    ) {
        Object claim = jwt.getClaim(
                claimName
        );

        if (claim instanceof String value) {
            agregarScopesDesdeTexto(
                    authorities,
                    value
            );

            return;
        }

        if (claim instanceof Collection<?> values) {
            for (Object value : values) {
                agregarScope(
                        authorities,
                        value
                );
            }
        }
    }

    private void agregarScopesDesdeTexto(
            Set<GrantedAuthority> authorities,
            String valor
    ) {
        if (valor.isBlank()) {
            return;
        }

        String[] scopes =
                valor.split("[,\\s]+");

        for (String scope : scopes) {
            agregarScope(
                    authorities,
                    scope
            );
        }
    }

    private void agregarScope(
            Set<GrantedAuthority> authorities,
            Object valor
    ) {
        if (valor == null) {
            return;
        }

        String scope =
                String.valueOf(valor)
                        .trim()
                        .toUpperCase();

        if (scope.isBlank()) {
            return;
        }

        String autoridad =
                scope.startsWith("SCOPE_")
                        ? scope
                        : "SCOPE_" + scope;

        authorities.add(
                new SimpleGrantedAuthority(
                        autoridad
                )
        );
    }

    private String normalizarRol(
            String role
    ) {
        String roleLimpio =
                role.trim()
                        .toUpperCase();

        if (roleLimpio.startsWith("ROLE_")) {
            return roleLimpio;
        }

        return "ROLE_" + roleLimpio;
    }
}
