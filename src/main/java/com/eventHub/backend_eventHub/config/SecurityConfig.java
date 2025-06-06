package com.eventHub.backend_eventHub.config;

import com.eventHub.backend_eventHub.auth.jwt.JwtAuthenticationFilter;
import com.eventHub.backend_eventHub.auth.jwt.JwtEntryPoint;
import com.eventHub.backend_eventHub.auth.service.UserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private JwtEntryPoint jwtEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permitir preflights CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ========== ENDPOINTS PÚBLICOS ==========
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/password/**", "/actuator/health", "/actuator/info").permitAll()

                        // ========== CATEGORÍAS ==========
                        // Endpoints públicos de categorías (lectura)
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/search").permitAll()

                        // Endpoints administrativos de categorías (requieren ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/categories/{id}/toggle-status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/categories/admin/all").hasRole("ADMIN")

                        // ========== EVENTOS PÚBLICOS ==========
                        // Endpoints públicos de eventos (sin autenticación)
                        .requestMatchers(HttpMethod.POST, "/api/events/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/featured").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/upcoming").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/recent").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/{id}").permitAll()

                        // ========== SUB-EVENTOS PÚBLICOS ==========
                        .requestMatchers(HttpMethod.GET, "/api/subevents/by-event/{eventId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/subevents/{id}").permitAll()

                        // ========== EVENTOS AUTENTICADOS ==========
                        // Endpoints que requieren ROLE_USUARIO
                        .requestMatchers(HttpMethod.POST, "/api/events/search-authenticated").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/events/{id}/authenticated").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/events/my-created").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/events/as-subcreator").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/events/{id}").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/{id}").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/events/{id}/invite-subcreator").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/events/accept-invitation/{invitationId}").hasRole("USUARIO")

                        // ========== SUB-EVENTOS AUTENTICADOS ==========
                        .requestMatchers(HttpMethod.POST, "/api/subevents").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/subevents/{id}").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/subevents/{id}").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/subevents/my-created").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/subevents/{id}/registrations").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/subevents/{id}/stats").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.PATCH, "/api/subevents/{id}/status").hasRole("USUARIO")

                        // ========== INSCRIPCIONES ==========
                        .requestMatchers(HttpMethod.POST, "/api/inscriptions/register").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/inscriptions/register-subevent").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/inscriptions/cancel/{eventoId}").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/inscriptions/cancel-subevent/{subeventoId}").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/inscriptions/my-registrations").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/inscriptions/my-subevent-registrations").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/inscriptions/check/{eventoId}").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/inscriptions/event/{eventoId}").hasRole("USUARIO")

                        // ========== INVITACIONES ==========
                        .requestMatchers(HttpMethod.GET, "/api/invitations/pending").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/invitations/all").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/invitations/reject/{invitationId}").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.GET, "/api/invitations/count").hasRole("USUARIO")

                        // ========== ADMINISTRACIÓN DE EVENTOS ==========
                        // Endpoints administrativos de eventos (requieren ADMIN o SUBADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/admin/events").hasAnyRole("ADMIN", "SUBADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/events/by-block-status").hasAnyRole("ADMIN", "SUBADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/events/{id}/toggle-block").hasAnyRole("ADMIN", "SUBADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/events/{id}/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/events/statistics").hasAnyRole("ADMIN", "SUBADMIN")

                        // ========== ENDPOINTS ADMINISTRATIVOS GENERALES ==========
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ========== USUARIOS AUTENTICADOS ==========
                        .requestMatchers("/auth/check-auth").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/me", "/users/me/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/me", "/users/me/**").authenticated()
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        // Por defecto, todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userAuthService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:5173",
                "https://event-hub-rose-omega.vercel.app/",
                "admin-eventhub.vercel.app",
                "http://127.0.0.1:54285",
                "http://localhost:5174"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}