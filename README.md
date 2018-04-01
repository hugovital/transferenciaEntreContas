# transferenciaEntreContas

Este é um projeto que têm por objetivo demonstrar, a partir de um caso de uso real (transferência de valores entre duas contas), como funciona o mecanismo de compensação do Apache Camel, ligado à um repositório (banco de dados).

Este projeto possui 4 serviços unitários:

* DebitoEmConta
* CreditoEmConta
* DesfazDebitoEmConta
* DesfazCreditoEmConta

Como os nomes dizem, esses serviços servem para debitar e creditar valores em contas correntes, e os inversos: reverter as operações realizadas.

Por simplicidade, não utilizei um banco de dados para realizar essas operações, mas sim arquivos .txt, que serão criados no diretório especificado na rota principal (ver classe TransferenciaEntreContrasOrquestrador)

Esse projeto também possui um serviço orquetrador:

* TransferenciaEntreContasOrquestrador

Esse serviço recebe um JSON com o número da conta para débito, o número da conta para crédito, e um valor. A partir disso, ele irá chamar o serviço de débito e o de crédito, orquestrando assim a transferência de valores.

Caso algum erro ocorra, esse serviço orquestrador irá chamar os serviços de desfazimento acima. Como queremos que sempre o desfazimento ocorra, configuramos um Repositório (nesse exemplo, Hawtdb, bem simples), para que o Apache Camel consiga guardar as informações. Desse modo, caso ocorra erro no desfazimento, o Apache Camel irá disparar novamente, eternamente, os serviços de desfazimento, até que consiga obter sucesso em ambos. Esse mecanismo é implementado através do pattern aggregate, do Camel.

Para que tenhamos sucesso, é importante que todos os serviços acima (DebitoEmConta, CreditoEmConta, DesfazDebitoEmConta, DesfazCreditoEmConta) sejam IDEMPOTENTES, isto é, caso sejam chamados várias vezes, não repitam a operação. Vários tratamentos foram adicionados na classe DAO (ContasDAO) para evidenciar esse modelo. Leia mais sobre serviços IDEMPOTENTES abaixo e sua necessidade para ORQUESTRAÇÕES.

Isto posto, da forma como o serviço TransferenciaEntreContasOrquestrador está construído neste exemplo, sempre retornará erro. 

Há um "Thread.sleep" em CreditoEmConta, para forçar um timeout. Há também um "throw Exception" em DesfazCreditoEmConta, para mostrar como o Camel tenta várias vezes chamar os desfazimento quando encontra erros no desfazimento. Repare que esse "throw Expetion" em DesfazCreditoEmConta têm um contador, que ao atingir 3 deixa de executar a exceção, permitindo que o camel efetue a compensação. Dessa forma, tudo volta ao normal, ao estado antes de ocorrer problemas.

Você pode inclusive parar o processo java e reiniciar. Ao fazer isso, irá notar que o Apache Camel recupera os dados do banco e continua tentando chamar os desfazimentos, até obter sucesso.

Para ver o sucesso, entre na pasta de arquivos, e veja os valores: estão todos com 0.0, pois foram estornados corretamente. A transação foi marcada em todos os arquivos (débito, credito, desfazCredito e desfazDebito).

Uma última nota importante: os serviços precisam ser modelados com IDEMPOTENCIA para funcionarem. O serviço orquestrador gera um ID da TRANSACAO que é repassado para os serviços, que através desses geram seus controles. Sem essa IDEMPOTENCIA ou IDTRANSACAO, é muito difícil garantir que o serviço unitário não será chamado duas vezes (teríamos que adicionar muitos 'if's na rota orquestradora).

---------------------------------

Conceitos Gerais de Orquestração com Desfazimento
=================================================

Para casos de desfazimento, é importante considerarmos sempre 3 pontos nas rotas:

	* Serviços unitários idempotentes;
	* Serviço orquestrador;
	* Mecanismo de 'background';

1) Serviços unitários idempotentes: Idempotente significa que você pode chamar um serviço várias vezes, o retorno será sempre o mesmo. Em termos práticos, o serviço 'testa' se a transação ou seu desfazimento já foram efetuados. Caso não tenham sido, roda o serviço. Caso algo exista, não faz nada. Um serviço de crédito em conta, por exemplo:

//importante testar se a compensação foi feita antes da transação principal, para cenário complexos de timeouts e enfileiramentos grandes
if ( banco nao contem desfazimento de idTransacao )

	if ( banco contem idTransacao )
	
		não faz nada;
		
	else {

		grava idTransacao no banco;
		credita valor no banco;
	}

Um serviço de desfazimento de crédito:

if ( banco não contem desfazimento de idTransacao )
	if ( banco contem idTransacao não estornado) {
		estorna valor na conta;
		marca idTransacao como estornado
	} else
		não faz nada;

É possível ver acima que podemos chamar ambos os serviços várias vezes, e que cada operação só será executada uma vez. Isso é imprescindível para garantir o estado das transações.


2) Serviço orquestrador: Aqui temos um serviço 'mestre' que irá chamar os serviços untários, e em havendo erro, chamará os desfazimentos. No camel, é a rota orquestrador principal. Este deve também gerar um ID que será passado para os serviços, para que estes consigam se organizar internamente:

	fazer:	
		gerarIdTransacao();
		chamaServicoDebitoEmConta( idTransacao );
		chamaServicoCreditoEmConta( idTransacao );
	emCasoDeErro (erro de negócio, erro técnico, timeout):
		chamaServicoDesfazDebitoEmConta( idTransacao  );
		chamaServicoDesfazCreditoEmConta( idTransacao );


3) Mecanismo 'background': Um mecanismo capaz de monitorar, em background, quais transações foram concluídas, quais deram erros, quais caíram (máquina caiu) e precisam ser retomadas. No camel, esse mecanismo se faz através do "aggregate", configurado com repositório (banco de dados) para compensações.

------

Existem outros patterns para transações distribuidas que não envolvem orquestração, tais como: Coreografia, Saga Pattern, Event Sourcing, etc. Procure se aprofundar neles, são importantes para cenários microservices ;)