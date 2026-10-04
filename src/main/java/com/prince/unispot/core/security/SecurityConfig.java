package com.prince.unispot.core.security;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration //modrn componrnt based, explicitly build and return SecurityFilterChain bean
@EnableWebSecurity //completely handover web security configuration to this class
@EnableMethodSecurity //enables @PreAuthorize on controller methods, by activating AOP
@RequiredArgsConstructor //automatically dependency injected too
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Value ("${unispot.security.cors.allowed-origins}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            //cors added
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            //well jwt immune so
            .csrf(AbstractHttpConfigurer::disable)
            
            //defining endpoint authZ, rules evaluated top to bot, so specific ones above
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll() // browser preflight, before any auth check
                .requestMatchers("/api/v1/auth/**").permitAll() // 4 registration and login
                .requestMatchers(HttpMethod.GET, "/api/v1/places/**").permitAll() // for viewing
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll() 
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") 
                .anyRequest().authenticated() // Everything else (POST, DELETE) requires a valid JWT
            )
            
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint)//401: no / invalid / expired token
                .accessDeniedHandler(restAccessDeniedHandler)// 403: authenticated, wrong role
            )

            // place it exactly b4 Spring's default username/password filter, so that they see its already authN
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList()); // frontend origin for cors
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true); //for refresh token cookie
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
