package com.prince.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.prince.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration //source of bean definition
@EnableWebSecurity //registers SecurityFilterChain
@EnableMethodSecurity //Allows annotations like:@PreAuthorize@PostAuthorize@Secured@RolesAllowed
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    
    //password encoder, we dont use plain password but hash in dbs
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
