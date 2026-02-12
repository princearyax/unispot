package com.prince.security;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prince.model.User;
import com.prince.model.enums.UserStatus;
import com.prince.repository.UserRepository;

import lombok.RequiredArgsConstructor;

//spring security doesn't know my repo User, it needd some kind of translator, this service will look up User in my db and create UserDetails that spring security can understand
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService  implements UserDetailsService{

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // optimise
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // TODO Auto-
        

        //find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("user not found with email" + email));

        //Enforce admin approval
        if(user.getStatus() == UserStatus.PENDING){
            throw new DisabledException("Account is pending for admin approval");
        }

        //Enforce banned account
        if(user.getStatus() == UserStatus.BANNED){
            throw new DisabledException("Account is banned");
        }

        //map roles to authorities in spring world
        //spring security expects roles to be granted authorities
        Collection<GrantedAuthority> authorities = user.getRoles()
            .stream().map(role -> new SimpleGrantedAuthority(role.name()))
            .collect(Collectors.toList());
       
        //return spring's user details implementation
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            authorities
        );
    }

}
