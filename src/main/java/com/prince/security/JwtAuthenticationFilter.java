package com.prince.security;
//front door of app, checks header, (Authorization) extract token and tells Sprring SecurityContext who it really is and their roles

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; //so that one api call just executes it once, no performance bottleneck

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component // makes this Spring bean so can DI
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private final JwtUtils jwtUtils;
    //inject interface, spring gonna find actual
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    )throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String userEmail;
        final String jwtToken;

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return; //stop executing rest of filter
        }

        jwtToken = authHeader.substring(7);
        userEmail = jwtUtils.extractUsername(jwtToken);

        if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null){ //we have email but unauthenticated user
            //fetch user from db, userDetailsService, gonna enforce account status too
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            //validate token
            if(jwtUtils.validateToken(jwtToken, userDetails.getUsername())){
                //create official SpringSecurityAuth Token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,   //password is null as jwt based
                    userDetails.getAuthorities() //roles e.g. Role_Admin
                );

                //extra details, ip add. session id. for audit/logging
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                //now putting it in securityContext so taht rest appln can know
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        }

        //pass to next filter
        filterChain.doFilter(request, response);
        
    }
}


// UsernamePasswordAuthenticationToken is the spring's official id card like, we take USerDetails inside, hand it to SecurityContextholder(which hold user for entire http req)
