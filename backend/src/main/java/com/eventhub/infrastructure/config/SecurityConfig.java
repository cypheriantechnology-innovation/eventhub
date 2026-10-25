package com.eventhub.infrastructure.config;

import com.eventhub.infrastructure.security.headers.SecurityHeadersFilter;
import com.eventhub.infrastructure.security.ratelimit.RateLimitingFilter;
import com.eventhub.infrastructure.security.ratelimit.SlidingWindowRateLimiter;
import com.eventhub.infrastructure.security.waf.WafFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public WafFilter wafFilter(ObjectMapper objectMapper) {
        return new WafFilter(objectMapper);
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter(SlidingWindowRateLimiter rateLimiter, ObjectMapper objectMapper) {
        return new RateLimitingFilter(rateLimiter, objectMapper);
    }

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Bean
    public org.springframework.web.filter.ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new org.springframework.web.filter.ShallowEtagHeaderFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   com.eventhub.infrastructure.security.tracing.CorrelationIdFilter correlationIdFilter,
                                                   WafFilter wafFilter,
                                                   RateLimitingFilter rateLimitingFilter,
                                                   SecurityHeadersFilter securityHeadersFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/events/**",
                                "/api/audit-logs/**",
                                "/api/auth/**",
                                "/actuator/health/**",
                                "/actuator/info/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                                "/actuator/metrics/**",
                                "/actuator/prometheus/**"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                // Register Defense-in-Depth filters in strict order
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(wafFilter, com.eventhub.infrastructure.security.tracing.CorrelationIdFilter.class)
                .addFilterAfter(rateLimitingFilter, WafFilter.class)
                .addFilterAfter(securityHeadersFilter, RateLimitingFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .roles("ACTUATOR_ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
