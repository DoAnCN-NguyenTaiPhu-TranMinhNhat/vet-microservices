package org.springframework.samples.petclinic.api.boundary.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Serves the Angular shell via {@link org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping}
 * (order 0) instead of a {@link org.springframework.web.reactive.function.server.RouterFunction}, so
 * {@link org.springframework.web.reactive.function.server.support.RouterFunctionMapping} (order -1) can stay empty and
 * never intercept {@code POST /api/...} with 405.
 */
@RestController
public class SpaIndexController {

	@Value("classpath:/static/index.html")
	private Resource indexHtml;

	@GetMapping(value = { "/", "/index.html" }, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<Resource> spaIndex() {
		return Mono.just(indexHtml);
	}
}
