
package com.eventHub.backend_eventHub.config;

import com.eventHub.backend_eventHub.auth.jwt.JwtAuthenticationFilter;
import com.eventHub.backend_eventHub.auth.jwt.JwtEntryPoint;
import com.eventHub.backend_eventHub.auth.service.UserAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserAuthService userAuthService;
    private final JwtEntryPoint jwtEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtEntryPoint))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ========== PERMITIR PRE-FLIGHTS CORS ==========
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ========== ENDPOINTS PÚBLICOS BÁSICOS ==========
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/password/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll() // AGREGADO: Permitir página de error

                        // ========== ENDPOINTS PÚBLICOS DE EVENTOS (SIN AUTENTICACIÓN) ==========
                        // Estos endpoints están disponibles para usuarios NO AUTENTICADOS

                        // Búsqueda y listados públicos
                        .requestMatchers(HttpMethod.POST, "/api/events/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/featured").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/upcoming").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/recent").permitAll()

                        // Detalle de eventos públicos (específico por ID)
                        .requestMatchers(HttpMethod.GET, "/api/events/*").permitAll() // CORREGIDO: era {id}

                        // Sub-eventos públicos
                        .requestMatchers(HttpMethod.GET, "/api/subevents/by-event/*").permitAll() // CORREGIDO: era /**
                        .requestMatchers(HttpMethod.GET, "/api/subevents/*").permitAll() // CORREGIDO: era {id}

                        // AGREGADO: Categorías públicas (necesario para el frontend)
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/*").permitAll()

                        // ========== ENDPOINTS DE USUARIO AUTENTICADO ==========
                        // Requieren JWT válido y rol USUARIO

                        // PERFIL DE USUARIO
                        .requestMatchers(HttpMethod.GET, "/users/me").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/users/me").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PATCH, "/users/me").hasRole("USUARIO") // AGREGADO

                        // GESTIÓN DE EVENTOS (CRUD)
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/events/*").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/*").hasRole("USUARIO")

                        // ENDPOINTS ESPECÍFICOS DE EVENTOS AUTENTICADOS
                        .requestMatchers(HttpMethod.POST, "/api/events/search-authenticated").hasRole("USUARIO") // AGREGADO
                        .requestMatchers(HttpMethod.GET, "/api/events/*/authenticated").hasRole("USUARIO") // AGREGADO
                        .requestMatchers(HttpMethod.GET, "/api/events/my-created").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/events/as-subcreator").hasRole("USUARIO")

                        // GESTIÓN DE SUBCREADORES
                        .requestMatchers(HttpMethod.POST, "/api/events/*/invite-subcreator").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/events/accept-invitation/*").hasRole("USUARIO")

                        // GESTIÓN DE SUB-EVENTOS
                        .requestMatchers(HttpMethod.POST, "/api/subevents").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/subevents/*").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/subevents/*").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PATCH, "/api/subevents/*/status").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/subevents/my-created").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/subevents/*/registrations").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/subevents/*/stats").hasRole("USUARIO")

                        // INSCRIPCIONES (TODAS REQUIEREN AUTENTICACIÓN)
                        .requestMatchers(HttpMethod.POST, "/api/inscriptions/register").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/inscriptions/register-subevent").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/inscriptions/cancel/*").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/inscriptions/cancel-subevent/*").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/inscriptions/my-registrations").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/inscriptions/my-subevent-registrations").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/inscriptions/check/*").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/inscriptions/event/*").hasRole("USUARIO")

                        // ========== ENDPOINTS DE ADMINISTRACIÓN ==========

                        // ADMINISTRACIÓN DE EVENTOS (ADMIN y SUBADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/admin/events").hasAnyRole("ADMIN", "SUBADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/events/by-block-status").hasAnyRole("ADMIN", "SUBADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/events/*/toggle-block").hasAnyRole("ADMIN", "SUBADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/events/statistics").hasAnyRole("ADMIN", "SUBADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/events/*/status").hasRole("ADMIN") // Solo ADMIN puede cambiar estado

                        // GESTIÓN DE USUARIOS (SOLO ADMIN)
                        .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/users/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/search").hasRole("ADMIN")

                        // PANEL ADMINISTRATIVO GENERAL
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ========== FALLBACK ==========
                        // Cualquier otra ruta requiere autenticación básica
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userAuthService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Configurar orígenes permitidos desde properties
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Headers permitidos
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        // Headers expuestos
        configuration.setExposedHeaders(List.of("Authorization"));

        // Permitir credenciales
        configuration.setAllowCredentials(true);

        // Tiempo de cache para preflight
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}