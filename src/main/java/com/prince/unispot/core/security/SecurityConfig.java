package com.prince.unispot.core.security;

import lombok.RequiredArgsConstructor;
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


@Configuration //modrn componrnt based, explicitly build and return SecurityFilterChain bean
@EnableWebSecurity //completely handover web security configuration to this class
@EnableMethodSecurity //enables @PreAuthorize on controller methods, by activating AOP
@RequiredArgsConstructor //automatically dependency injected too
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            //sud ensure cors policy to protect refresh route tho
            //set some allowed methods/origin/headers etc in corsConfigurationSource()

            
            //well jwt immune so
            .csrf(AbstractHttpConfigurer::disable)
            
            //defining endpoint authZ, rules evaluated top to bot, so specific ones above
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll() // 4 registration and login
                .requestMatchers(HttpMethod.GET, "/api/v1/places/**").permitAll() // for viewing
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll() 
                .anyRequest().authenticated() // Everything else (POST, DELETE) requires a valid JWT
            )
            
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // place it exactly b4 Spring's default username/password filter, so that they see its already authN
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
