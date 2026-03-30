package com.ridehailing.core_api.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
open class SecurityConfig {

  @Autowired
  private lateinit var jwtAuthFilter: JwtAuthFilter

  @Autowired
  private lateinit var rateLimitFilter: RateLimitFilter

  @Bean
  open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .csrf { it.disable() }
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
      .authorizeHttpRequests {
        it.requestMatchers("/auth/**").permitAll()
          .requestMatchers("/actuator/**").permitAll()
          .requestMatchers("/ping").permitAll()
          .anyRequest().authenticated()
      }
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
      .addFilterBefore(rateLimitFilter, JwtAuthFilter::class.java)
    return http.build()
  }

  @Bean
  open fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
