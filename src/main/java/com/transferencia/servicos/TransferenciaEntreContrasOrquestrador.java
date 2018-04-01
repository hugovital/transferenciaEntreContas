package com.transferencia.servicos;

import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hawtdb.HawtDBAggregationRepository;
import org.apache.camel.model.dataformat.JsonLibrary;

import com.transferencia.dataObjects.Transacao;
import com.transferencias.strategy.AgregacaoDaCompensacaoStrategy;
import com.transferencias.strategy.DescartarRetornoStrategy;

public class TransferenciaEntreContrasOrquestrador extends RouteBuilder {

	//Diretorio para salvar os arquivos. Alterar para um diretorio na sua máquina antes de rodas
	public static final String DIRETORIO_PARA_ARQUIVOS = "c:/users/hugo/desktop/transacao/";
	
	
	/*
	 * Para rodar esse projeto, fazer um post para: http://localhost:8080/conta/transferencia/
	 * Exemplo de JSON para o POST: 
	 * {"contaDebito":"12345","contaCredito":"98765","valor":100.00} 
	 */

	@Override
	public void configure() throws Exception {

		rest("/conta/transferencia")		
			.post()
			.route()
			
			.convertBodyTo(String.class)
			.log("Entrada Recebida: ${body}")
			
			.unmarshal().json( JsonLibrary.Jackson, Transacao.class )
		
			//orquestracao
			.to("direct:servicoGerarId")
				.to("direct:servicoAgregacao")
				
			.enrich("direct:servicoDebitoConta", new DescartarRetornoStrategy() )
				.to("direct:servicoAgregacao")
			
			.enrich("direct:servicoCreditoConta", new DescartarRetornoStrategy() )
				.to("direct:servicoAgregacao")
		
			.to("direct:transacaoComSucesso");
			//fim orquestracao


		from("direct:servicoAgregacao")
			.aggregate( simple(" ${body.id}" ), new AgregacaoDaCompensacaoStrategy() )

				.aggregationRepository( getBancoConfig() )				

				.completionSize( 3 )
				.completionTimeout( 4000 )

			.to("direct:fimAgregacao");

		from("direct:fimAgregacao")
		
			.log("Motivo fim da agregação: ${header.CamelAggregatedCompletedBy}")
			
			.choice()
			
				.when( simple("${header.CamelAggregatedCompletedBy} == 'timeout'") )
					.to("direct:chamarDesfazimentos")

				.when( simple("${header.CamelAggregatedCompletedBy} == 'size'") )
					// não faz nada pois houve sucesso

			.endChoice();
		

		from("direct:servicoGerarId")
			.process( ex -> {
				Transacao transacao = ex.getIn().getBody(Transacao.class);
				transacao.setId( gerarID() );
			} );

		
		from("direct:servicoDebitoConta")
			.transform().simple( "${body.operacaoDebito()} ")
			.marshal().json( JsonLibrary.Jackson )
			.setHeader(Exchange.HTTP_METHOD, constant("POST"))
			.to("http4://localhost:8080/conta/debito?bridgeEndpoint=true&httpClient.SocketTimeout=2000");

		from("direct:servicoCreditoConta")
			.transform().simple( "${body.operacaoCredito()} ")
			.marshal().json( JsonLibrary.Jackson )
			.setHeader(Exchange.HTTP_METHOD, constant("POST"))
			.to("http4://localhost:8080/conta/credito?bridgeEndpoint=true&httpClient.SocketTimeout=2000");

		from("direct:chamarDesfazimentos")
			.log("Iniciando os Desfazimentos: ${body}")
			.enrich("direct:desfazerDebito", new DescartarRetornoStrategy() )
			.enrich("direct:desfazerCredito", new DescartarRetornoStrategy() );
		
		from("direct:desfazerDebito")
			.transform().simple( "${body.operacaoDebito()} ")
			.marshal().json( JsonLibrary.Jackson )
			.setHeader(Exchange.HTTP_METHOD, constant("POST"))
			.to("http4://localhost:8080/conta/desfazDebito?bridgeEndpoint=true&httpClient.SocketTimeout=2000");
		
		from("direct:desfazerCredito")
			.transform().simple( "${body.operacaoCredito()} ")
			.marshal().json( JsonLibrary.Jackson )
			.setHeader(Exchange.HTTP_METHOD, constant("POST"))
			.to("http4://localhost:8080/conta/desfazCredito?bridgeEndpoint=true&httpClient.SocketTimeout=2000");
		
		from("direct:transacaoComSucesso")
			.log("Servico Transacao com Sucesso")
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant("200"))
			.transform().simple( "${body.sucesso()}");
		
	}
	
	public String gerarID(){
		return UUID.randomUUID().toString();		
	}
	
	public HawtDBAggregationRepository getBancoConfig(){
		
		HawtDBAggregationRepository hawtDBRepo = new HawtDBAggregationRepository("repo1", DIRETORIO_PARA_ARQUIVOS + "transferenciaEntreContas.dat");
		hawtDBRepo.setRecoveryInterval(10 * 1000); //intervalo que tenta enviar o serviço novamente, no caso, 10 segundos
		return hawtDBRepo;
		
	}
		

}
