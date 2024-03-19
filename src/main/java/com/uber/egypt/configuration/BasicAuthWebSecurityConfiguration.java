package com.uber.egypt.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class BasicAuthWebSecurityConfiguration {

    private final FileConfigurationReader configurationReader;

    @Autowired
    public BasicAuthWebSecurityConfiguration(FileConfigurationReader configurationReader) {
        this.configurationReader = configurationReader;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.
                authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable).
                httpBasic(Customizer.withDefaults()).
                build();
    }

    @Bean
    public UserDetailsService users() {
        var userName = configurationReader.getUserName();
        var password = configurationReader.getPassword();

        var passwordEncoder = new BCryptPasswordEncoder();
        var encryptedPassword = passwordEncoder.encode(password);

        var user = User.builder()
                .username(userName)
                .password(encryptedPassword)
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }
}
