package com.prince.unispot.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig {
    // this simple class gonna boot up the auditing engine at startup
    //nd to get id it'll look at auditorAwareImpl
}