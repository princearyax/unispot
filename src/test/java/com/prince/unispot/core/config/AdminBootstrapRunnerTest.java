package com.prince.unispot.core.config;

import com.prince.unispot.user.domain.model.Role;
import com.prince.unispot.user.domain.model.User;
import com.prince.unispot.user.infrastructure.persistence.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) //telss junit to use mockjito
class AdminBootstrapRunnerTest {

    //mockito creating fake versions
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks //mockito creates real this then inject mocks
    private AdminBootstrapRunner runner;

    @Test
    void skipsWhenEnvVarsNotSet() {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "");

        runner.run(null);

        verifyNoInteractions(userRepository);
    }

    @Test
    void skipsWhenAdminAlreadyExists() {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "admin@unispot.dev");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "meow123");
        //controlling dependency behavior, stubbing
        when(userRepository.existsByEmail("admin@unispot.dev")).thenReturn(true);

        runner.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createsAdminWhenNotPresent() {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "admin@unispot.dev");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "meow123");
        when(userRepository.existsByEmail("admin@unispot.dev")).thenReturn(false);
        when(passwordEncoder.encode("meow123")).thenReturn("hashed");

        runner.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
        assertEquals("hashed", captor.getValue().getPasswordHash());
    }
}