package org.synyx.urlaubsverwaltung.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.synyx.urlaubsverwaltung.person.PersonService;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.security.config.http.SessionCreationPolicy.NEVER;

@Configuration
class RestApiSecurityConfig {

    private final PersonService personService;

    RestApiSecurityConfig(PersonService personService) {
        this.personService = personService;
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/api/**", "/api/", "/api")
            .authorizeHttpRequests(authorizeHttpRequests ->
                authorizeHttpRequests
                    .requestMatchers(GET, "/api/**", "/api/", "/api").permitAll()
                    .anyRequest().authenticated()
            ).sessionManagement(
                sessionManagement -> sessionManagement.sessionCreationPolicy(NEVER)
            ).oauth2ResourceServer(
                oauth2ResourceServer -> oauth2ResourceServer.jwt(
                    jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(new JwtToPersonGrantedAuthoritiesConverter(personService)))
            ).build();
    }

}
