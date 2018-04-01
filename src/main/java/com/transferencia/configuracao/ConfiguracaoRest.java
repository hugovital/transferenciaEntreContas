package com.transferencia.configuracao;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

public class ConfiguracaoRest extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		
		restConfiguration()
			.component("jetty")
			.host("localhost")
			.port(8080)
			.bindingMode(RestBindingMode.off);

	}

}
