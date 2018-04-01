package com.transferencia.servicos;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import com.transferencia.dao.ContasDAO;
import com.transferencia.dataObjects.Operacao;

public class DesfazCreditoEmConta extends RouteBuilder {
	
	public static int vezes = 0;

	@Override
	public void configure() throws Exception {

		rest("/conta/desfazCredito")
			.post()
			.route()
			
				.convertBodyTo(String.class)
				.log("Desfaz Crédito recebido: ${body}")			
				
				.process( ex -> {
					if (DesfazCreditoEmConta.vezes < 3){
						DesfazCreditoEmConta.vezes++; 
						throw new Exception("Erro ao desfazer crédito");
					}
				})				

				.unmarshal().json( JsonLibrary.Jackson, Operacao.class )
				.process( ex -> {
					
					Operacao operacao = ex.getIn().getBody(Operacao.class);
					ContasDAO contasDAO = new ContasDAO();
					contasDAO.desfazCreditoEmContaCorrente( 
							operacao.getIdTransacao(), 
							operacao.getIdConta(), 
							operacao.getValor() );
					
				} )
				.setHeader(Exchange.HTTP_RESPONSE_CODE, simple("200"));

	}

}