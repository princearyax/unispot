package com.prince.unispot.user.application.service;

import com.prince.unispot.core.exception.ResourceNotFoundException;
import com.prince.unispot.user.domain.model.Role;
import com.prince.unispot.user.domain.model.User;
import com.prince.unispot.user.infrastructure.persistence.UserRepository;
import com.prince.unispot.user.presentation.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;

    @Transactional
    public UserSummaryResponse updateRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setRole(newRole);
        User saved = userRepository.save(user);
        return UserSummaryResponse.from(saved);
    }
}