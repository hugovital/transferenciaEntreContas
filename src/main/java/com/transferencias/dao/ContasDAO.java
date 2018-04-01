package com.transferencias.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.transferencia.servicos.TransferenciaEntreContrasOrquestrador;

public class ContasDAO {
	
	private String diretorio = TransferenciaEntreContrasOrquestrador.DIRETORIO_PARA_ARQUIVOS;
	
	public void creditoEmContaCorrente(String idTransacao, String idConta, Double valor) throws Exception {
		
		//verifica se existe uma marcação de desfazer essa transação (cenários extremos onde o desfazimento por ventura rode antes do 'fazimento'
		if (!existeDesfazimentoTransacaoDeCredito(idTransacao)){

			//verifica se a esta transação de credito já não foi realizada			
			if (!existeTransacaoCredito(idTransacao)){

				Double valorCorrente = getContaValor(idConta);
				valorCorrente += valor;
				salvarValorConta(idConta, valorCorrente);
				
				salvarTransacaoCredito( idTransacao );
				
			}
			
		}
		
	}

	public void debitoEmContaCorrente(String idTransacao, String idConta, Double valor) throws Exception {
		
		//verifica se existe uma marcação de desfazer essa transação (cenários extremos onde o desfazimento por ventura rode antes do 'fazimento'
		if (!existeDesfazimentoTransacaoDeDebito(idTransacao)){

			//verifica se a esta transação de débito já não foi realizada			
			if (!existeTransacaoDebito(idTransacao)){

				Double valorCorrente = getContaValor(idConta);
				valorCorrente -= valor;
				salvarValorConta(idConta, valorCorrente);
				
				salvarTransacaoDebito( idTransacao );
				
			}
			
		}		
		
	}
	
	public void desfazCreditoEmContaCorrente(String idTransacao, String idConta, Double valor) throws Exception {	
		
		//verifica se a esta transação de crédito já foi realizada
		if (existeTransacaoCredito(idTransacao)){		

			//verifica se não existe uma marcação de desfazer essa transação (ou seja, a transação já foi desfeita)						
			if (!existeDesfazimentoTransacaoDeCredito(idTransacao)){	

				Double valorCorrente = getContaValor(idConta);
				valorCorrente -= valor;
				salvarValorConta(idConta, valorCorrente);
				
				salvarTransacaoDesfazCredito( idTransacao );
				
			}
			
		}
		
	}

	public void desfazDebitoEmContaCorrente(String idTransacao, String idConta, Double valor) throws Exception {
		
		//verifica se a esta transação de débito já foi realizada
		if (existeTransacaoDebito(idTransacao)){		

			//verifica se não existe uma marcação de desfazer essa transação (ou seja, a transação já foi desfeita)						
			if (!existeDesfazimentoTransacaoDeDebito(idTransacao)){	

				Double valorCorrente = getContaValor(idConta);
				valorCorrente += valor;
				salvarValorConta(idConta, valorCorrente);
				
				salvarTransacaoDesfazDebito( idTransacao );
				
			}
			
		}		
		
	}
	
	public boolean existeDesfazimentoTransacaoDeCredito(String idTransacao) throws Exception {		
		File f = new File( diretorio + "transacoes_credito_desfeitas.txt" );
		return existeNaLista(f, idTransacao);
	}
	
	public boolean existeTransacaoCredito(String idTransacao) throws Exception {		
		File f = new File( diretorio + "transacoes_credito.txt" );
		return existeNaLista(f, idTransacao);
	}
	
	public boolean existeDesfazimentoTransacaoDeDebito(String idTransacao) throws Exception {		
		File f = new File( diretorio + "transacoes_debito_desfeitas.txt" );
		return existeNaLista(f, idTransacao);
	}
	
	public boolean existeTransacaoDebito(String idTransacao) throws Exception {		
		File f = new File( diretorio + "transacoes_debito.txt" );
		return existeNaLista(f, idTransacao);
	}	
	
	private boolean existeNaLista(File f, String valor) throws Exception {
		List<String> lista = carregarLista(f);
		return lista.contains(valor);
	}
	
	private void salvarTransacaoCredito(String idTransacao) throws Exception {		
		File f = new File( diretorio + "transacoes_credito.txt" );
		salvarLista(f, idTransacao);
	}
	
	private void salvarTransacaoDebito(String idTransacao) throws Exception {		
		File f = new File( diretorio + "transacoes_debito.txt" );
		salvarLista(f, idTransacao);
	}
	
	private void salvarTransacaoDesfazDebito(String idTransacao) throws Exception {		
		File f = new File( diretorio + "transacoes_debito_desfeitas.txt" );
		salvarLista(f, idTransacao);
	}
	
	private void salvarTransacaoDesfazCredito(String idTransacao) throws Exception {		
		File f = new File( diretorio + "transacoes_credito_desfeitas.txt" );
		salvarLista(f, idTransacao);
	}	
	
	private Double getContaValor(String idConta) throws Exception {
		
		File f = new File( diretorio + "conta_" + idConta + ".txt" );
		
		if (f.exists()){
			
			String s = lerConteudoArquivo(f);
			return Double.parseDouble(s);

		} else {
			
			salvarConteudoArquivo(f, "0");
			return 0.0;

		}

	}
	
	
	private void salvarLista(File f, String valor) throws Exception {
		
		List<String> lista = carregarLista(f);
		lista.add(valor);
		salvarLista(f, lista);
		
	}
	
	private void salvarLista(File f, List<String> lista) throws Exception {
		
		StringBuilder sb = new StringBuilder();
		for(String s : lista){
			if (!s.trim().equals(""))
				sb.append(s).append("\r\n");
		}
		salvarConteudoArquivo(f, sb.toString());
		
	}	
	
	private List<String> carregarLista(File f) throws Exception {
		
		List<String> retorno = new ArrayList<String>();
		
		if (!f.exists())
			
			f.createNewFile();
		
		else {
			
			String cont = lerConteudoArquivo(f);
			String[] partes = cont.split("\r\n");
			for(String p : partes)
				retorno.add(p);
			
		}
		
		
		return retorno;
		
		
	}	
	
	private void salvarValorConta(String idConta, Double valor) throws Exception {
		
		File f = new File( diretorio + "conta_" + idConta + ".txt" );
		salvarConteudoArquivo(f, Double.toString(valor));
		
	}
	
	private String lerConteudoArquivo(File f) throws Exception {
		
		FileInputStream fis = new FileInputStream( f );	
		int size = new Long ( f.length() ).intValue();		
		byte[] b = new byte[ size ];		
		fis.read(b);		
		fis.close();
		
		return new String(b);
		
	}
	
	private void salvarConteudoArquivo(File f, String conteudo) throws Exception {
		FileOutputStream fos = new FileOutputStream( f );			
		fos.write(conteudo.getBytes());
		fos.close();
	}

}
