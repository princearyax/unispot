package com.prince.unispot.core.config;

import com.prince.unispot.user.domain.model.Role;
import com.prince.unispot.user.domain.model.User;
import com.prince.unispot.user.infrastructure.persistence.UserRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${unispot.admin.bootstrap-email:}")
    private String bootstrapEmail;

    @Value("${unispot.admin.bootstrap-password:}")
    private String bootstrapPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapEmail == null || bootstrapEmail.isBlank()
                || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set — skipping admin bootstrap.");
            return;
        }

        if (userRepository.existsByEmail(bootstrapEmail)) {
            log.info("Bootstrap admin already exists — skipping.", bootstrapEmail);
            return;
        }

        User admin = User.builder()
                .email(bootstrapEmail)
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.warn("Bootstrap admin account created for Prince Arya. Rotate this password after first login.", bootstrapEmail);
    }
}