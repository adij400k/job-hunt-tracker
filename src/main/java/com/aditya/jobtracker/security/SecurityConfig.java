package com.aditya.jobtracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// The rules of the door: who may enter, and where.
// @Configuration = "this class creates beans". Each @Bean method makes one object for Spring.
@Configuration
public class SecurityConfig {

    // Used to hash passwords when users register, and to check them at login
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protects browser logins that use cookies. Our API has no cookies
            // (every request carries its own login), so we switch it off.
            .csrf(csrf -> csrf.disable())

            // STATELESS = the server remembers nobody between requests
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // AUTHORIZATION: the rules are checked top to bottom, the first match wins
            .authorizeHttpRequests(rules -> rules
                    .requestMatchers("/hello", "/error", "/api/auth/register").permitAll()   // open to everyone
                    // the web page itself (HTML, CSS, JS) must load BEFORE the user logs in.
                    // The data behind /api/** stays protected.
                    .requestMatchers("/", "/index.html", "/style.css", "/app.js", "/favicon.ico").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/jobs/**").hasRole("ADMIN")     // only ADMIN deletes
                    .requestMatchers(HttpMethod.POST, "/api/watcher/**").hasRole("ADMIN")   // only ADMIN starts/stops the watcher
                    .anyRequest().authenticated())                                           // everything else: logged in

            // AUTHENTICATION: the client sends "Authorization: Basic <username:password>"
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
