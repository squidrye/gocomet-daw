package com.ridehailing.core_api.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WebConfig : WebMvcConfigurer {

  @org.springframework.beans.factory.annotation.Autowired
  private lateinit var schemaValidationInterceptor: SchemaValidationInterceptor

  override fun addCorsMappings(registry: CorsRegistry) {
    registry.addMapping("/**")
      .allowedOrigins("http://localhost:5173", "http://localhost:3000")
      .allowedMethods("*")
      .allowedHeaders("*")
      .allowCredentials(true)
  }

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(schemaValidationInterceptor)
  }
}
