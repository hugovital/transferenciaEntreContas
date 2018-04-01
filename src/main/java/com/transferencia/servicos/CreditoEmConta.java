package com.transferencia.servicos;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import com.transferencia.dataObjects.Operacao;
import com.transferencias.dao.ContasDAO;

public class CreditoEmConta extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		rest("/conta/credito")
			.post()
			.route()
			
				.convertBodyTo(String.class)
				.log("Crédito recebido: ${body}")

				.unmarshal().json( JsonLibrary.Jackson, Operacao.class )
				.process( ex -> {
					
					Operacao operacao = ex.getIn().getBody(Operacao.class);
					ContasDAO contasDAO = new ContasDAO();
					contasDAO.creditoEmContaCorrente( 
							operacao.getIdTransacao(), 
							operacao.getIdConta(), 
							operacao.getValor() );
					
				} )
				.setHeader(Exchange.HTTP_RESPONSE_CODE, simple("200"))
				.process( ex -> {
					try {
						Thread.sleep(3000);
					} catch (Exception ex1){
					}
				} );

	}

}
