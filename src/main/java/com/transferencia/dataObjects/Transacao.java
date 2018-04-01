package com.transferencia.dataObjects;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties("id")
public class Transacao implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String id;
	
	private String contaDebito;
	
	private String contaCredito;
	
	private Double valor;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getContaDebito() {
		return contaDebito;
	}

	public void setContaDebito(String contaDebito) {
		this.contaDebito = contaDebito;
	}

	public String getContaCredito() {
		return contaCredito;
	}

	public void setContaCredito(String contaCredito) {
		this.contaCredito = contaCredito;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public Operacao operacaoDebito(){
		
		Operacao operacao = new Operacao();
		operacao.setIdTransacao( this.id );
		operacao.setIdConta( this.contaDebito );		
		operacao.setValor( this.valor );
		
		return operacao;

	}
	
	public Operacao operacaoCredito(){
		
		Operacao operacao = new Operacao();
		operacao.setIdTransacao( this.id );
		operacao.setIdConta( this.contaCredito );		
		operacao.setValor( this.valor );
		
		return operacao;

	}

	public String sucesso(){		
		String s = "{\"idTransacao:\":\"" + this.id + "\",\"mensagem\":\"Transação processada com sucesso.\"}";
		return s;		
	}


}
