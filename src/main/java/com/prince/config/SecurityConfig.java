package com.prince.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    //authentication provider
    //spring core's auth logic
    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    //authentication manager bean
    //used later in login service to trigger the auth process
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }

    //core security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
            //disable csrf as gonna use jwt not browser cookie
            .csrf(AbstractHttpConfigurer::disable)

            //route authorization rules, two * mean anything under the path
            .authorizeHttpRequests(auth -> auth
                //auth endpoints ar punlic(login, register)
                .requestMatchers("/api/v1/auth/**").permitAll()

                //anon users can browse places, get req
                .requestMatchers(HttpMethod.GET, "/api/v1/places/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/comments/**").permitAll()

                //admnin only end-points
                .requestMatchers("api/v1/admin/**").hasRole("ADMIN")

                //everything else requires authentication
                .anyRequest().authenticated()
            )

            //enforce statelessness
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            //register outr authProvider
            .authenticationProvider(authenticationProvider())

            //insert custom jwt filter b4 standard namePass filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
