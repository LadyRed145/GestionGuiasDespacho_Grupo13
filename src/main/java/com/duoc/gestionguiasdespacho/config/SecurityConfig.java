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
import java.util.List;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        .requestMatchers("/api/guias/*/descargar")
                        .hasAnyAuthority(
                                SecurityRoles.ROLE_GUIA_DOWNLOAD,
                                SecurityRoles.ROLE_GUIA_DESPACHO,
                                "SCOPE_" + SecurityRoles.GUIA_DOWNLOAD,
                                "SCOPE_" + SecurityRoles.GUIA_DESPACHO
                        )

                        .requestMatchers("/api/guias/**")
                        .hasAnyAuthority(
                                SecurityRoles.ROLE_GUIA_DESPACHO,
                                "SCOPE_" + SecurityRoles.GUIA_DESPACHO
                        )

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, extractAuthorities(jwt));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        addRolesFromClaim(jwt, authorities, "roles");
        addRolesFromClaim(jwt, authorities, "groups");
        addRolesFromClaim(jwt, authorities, "extension_roles");
        addRolesFromClaim(jwt, authorities, "extension_Roles");

        addScopes(jwt, authorities, "scp");
        addScopes(jwt, authorities, "scope");

        return authorities;
    }

    private void addRolesFromClaim(Jwt jwt, Set<GrantedAuthority> authorities, String claimName) {
        Object claim = jwt.getClaim(claimName);

        if (claim instanceof Collection<?> values) {
            values.stream()
                    .map(String::valueOf)
                    .map(this::normalizeRole)
                    .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }

        if (claim instanceof String value && !value.isBlank()) {
            List.of(value.split("[, ]+")).stream()
                    .map(this::normalizeRole)
                    .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }
    }

    private void addScopes(Jwt jwt, Set<GrantedAuthority> authorities, String claimName) {
        Object claim = jwt.getClaim(claimName);

        if (claim instanceof String value && !value.isBlank()) {
            List.of(value.split(" ")).stream()
                    .filter(scope -> !scope.isBlank())
                    .map(scope -> "SCOPE_" + scope.trim().toUpperCase())
                    .forEach(scope -> authorities.add(new SimpleGrantedAuthority(scope)));
        }
    }

    private String normalizeRole(String role) {
        String cleanRole = role.trim().toUpperCase();

        if (cleanRole.startsWith("ROLE_")) {
            return cleanRole;
        }

        return "ROLE_" + cleanRole;
    }
}
