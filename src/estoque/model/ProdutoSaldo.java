package estoque.model;

import java.math.BigDecimal;

public class ProdutoSaldo {
    private String codigoBarras;
    private String nome;
    private int fardos;
    private int unidadesAvulsas;
    private int totalUnidades;
    private BigDecimal precoUnitario;
    private BigDecimal valorTotal;

    public ProdutoSaldo(String codigoBarras, String nome, int fardos, int unidadesAvulsas,
                        int totalUnidades, BigDecimal precoUnitario, BigDecimal valorTotal) {
        this.codigoBarras = codigoBarras;
        this.nome = nome;
        this.fardos = fardos;
        this.unidadesAvulsas = unidadesAvulsas;
        this.totalUnidades = totalUnidades;
        this.precoUnitario = precoUnitario;
        this.valorTotal = valorTotal;
    }

    public String getCodigoBarras() { return codigoBarras; }
    public String getNome() { return nome; }
    public int getFardos() { return fardos; }
    public int getUnidadesAvulsas() { return unidadesAvulsas; }
    public int getTotalUnidades() { return totalUnidades; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getValorTotal() { return valorTotal; }
}