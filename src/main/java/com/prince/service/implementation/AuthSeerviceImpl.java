package com.prince.service.implementation;

import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prince.dto.request.LoginRequest;
import com.prince.dto.request.RegisterRequest;
import com.prince.dto.response.AuthResponse;
import com.prince.model.User;
import com.prince.model.enums.Role;
import com.prince.model.enums.UserStatus;
import com.prince.repository.UserRepository;
import com.prince.security.JwtUtils;
import com.prince.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthSeerviceImpl implements AuthService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional //ensures db operations are atomic
    public AuthResponse register(RegisterRequest request) {
        //if user already exists with the mail
        if(userRepository.existsByEmail(request.email())){
            //sud be custom exception handled by globalHandler
            throw new IllegalArgumentException("Email is already taken");
        }

        //map dto to entity and hash the password
        User user = User.builder()
                        .email(request.email())
                        .password(passwordEncoder.encode(request.password()))
                        .status(UserStatus.PENDING)
                        .roles(Set.of(Role.ROLE_USER))
                        .build();
        
        //save to db
        userRepository.save(user);

        return new AuthResponse(null, "Wait for approval");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        //authenticate the user
        //this line gonna tigger CustomUserDetailsService -> loadUserByUsername
        // It automatically checks the password hash & checks user status.
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email(), 
                request.password()
            )
        );

        //generate jwt if we reach here, user eligible
        String jwtToken = jwtUtils.generateToken(request.email());

        return new AuthResponse(jwtToken, "Login success");
    }
}
