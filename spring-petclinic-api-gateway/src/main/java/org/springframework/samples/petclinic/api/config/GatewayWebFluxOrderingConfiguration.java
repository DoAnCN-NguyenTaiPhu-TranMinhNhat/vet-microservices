package org.springframework.samples.petclinic.api.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;

/**
 * {@link RouterFunctionMapping} defaults to order {@code -1} and runs before
 * {@link RoutePredicateHandlerMapping} (default {@code 1}), which can yield 405 on {@code POST /api/...}.
 * We force the gateway mapping to highest precedence and push every {@link RouterFunctionMapping} after it.
 */
@Configuration
public class GatewayWebFluxOrderingConfiguration {

	@Bean
	static BeanPostProcessor gatewayWebFluxHandlerMappingOrderPostProcessor() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof RoutePredicateHandlerMapping mapping) {
					mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
				}
				else if (bean instanceof RouterFunctionMapping mapping) {
					mapping.setOrder(100);
				}
				return bean;
			}
		};
	}
}
