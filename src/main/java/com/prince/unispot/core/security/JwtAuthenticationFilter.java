package com.prince.unispot.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
// import org.springframework.lang.NonNull; its deprecated
import org.jspecify.annotations.NonNull; //modern std for nonnull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
//oncePerFilter.. so that even tho interanl forward, error dispatch, it will jut run once
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        
        //fail fast in case no token or wrong format,
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); //if puclic url hit, let it pass as anon
            return; //as filter works like stack not like goto!
        }

        final String jwt = authHeader.substring(7); //beginIdx

        try {
            
            //if not already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                // Single cryptographic verification + expiration check
                Claims claims = jwtService.parseAndValidateClaims(jwt);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                if (userId != null && role != null) {
                    //Create the AuthN object. 
                    //We pass userId as the principal. Our AuditorAwareImpl uses this!
                    //it uses three things, principal, credentials, roles
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null, //no password, as jwt
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))//this is how spring implicityly check roles
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));//attaches extra details, user ip, session id

                    //populate the Security Context in this virtual thread
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
                
            }
        } catch (Exception e) {
            // Malformed token, signature exception, or expired token
            // Log it, but do not crash-- context remains empty, and Spring Security will return 403 Forbidden. otherwise 500 internal server error if not catches here
            logger.warn("JWT Validation failed: " + e.getMessage());

            //if wanna raise exception here, need to use AuthenticationEntryPoint or HandlerExceptionResolver and will forward direct to GlobalExceptionHandler from here, otherwise its just generic 403, so that UX , frontend knows what fails

            //done now!
        }

        //Continue the chain
        filterChain.doFilter(request, response);
    }
}