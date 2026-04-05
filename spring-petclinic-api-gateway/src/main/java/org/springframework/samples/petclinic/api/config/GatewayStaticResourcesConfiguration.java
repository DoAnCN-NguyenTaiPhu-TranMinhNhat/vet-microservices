package org.springframework.samples.petclinic.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Static assets via {@link ResourceHandlerRegistry} instead of {@code RouterFunctions.resources},
 * so {@link org.springframework.web.reactive.function.server.support.RouterFunctionMapping} never
 * sees resource handlers that can answer {@code POST /api/...} with 405.
 */
@Configuration
public class GatewayStaticResourcesConfiguration implements WebFluxConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/scripts/**").addResourceLocations("classpath:/static/scripts/");
		registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
		registry.addResourceHandler("/fonts/**").addResourceLocations("classpath:/static/fonts/");
		registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}
}
