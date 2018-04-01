package com.transferencia.servicos;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import com.transferencia.dao.ContasDAO;
import com.transferencia.dataObjects.Operacao;

public class DesfazDebitoEmConta extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		rest("/conta/desfazDebito")
			.post()
			.route()
			
				.convertBodyTo(String.class)
				.log("Desfaz Débito recebido: ${body}")
			
				.unmarshal().json( JsonLibrary.Jackson, Operacao.class )
				.process( ex -> {
					
					Operacao operacao = ex.getIn().getBody(Operacao.class);
					ContasDAO contasDAO = new ContasDAO();
					contasDAO.desfazDebitoEmContaCorrente(
							operacao.getIdTransacao(), 
							operacao.getIdConta(), 
							operacao.getValor() );
					
				} )
				.setHeader(Exchange.HTTP_RESPONSE_CODE, simple("200"));

	}

}