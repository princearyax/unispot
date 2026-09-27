package com.prince.unispot.core.security;

import tools.jackson.databind.json.JsonMapper;
import com.prince.unispot.core.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

//runs inside security filter chain, before dispatcher servlet, so restControlleradvice wont see.
//handles missing,malform unauthenticated req hits endpoint requiring auth
// writing similar ErrorResponse so consistent
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper; //Spring Boot 4 auto-configures this

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ErrorResponse body = new ErrorResponse(
            request.getRequestURI(), "Unauthorized","Authentication is required to access this resource.", HttpStatus.UNAUTHORIZED.value(), Instant.now()
        );
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}