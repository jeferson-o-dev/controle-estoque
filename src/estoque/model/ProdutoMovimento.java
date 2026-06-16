package estoque.model;

import java.math.BigDecimal;

public class ProdutoMovimento {
    private String codigoBarras;
    private String nome;
    private int totalEntradas;
    private int totalSaidas;
    private int saldoLiquido;
    private BigDecimal valorEntradas;
    private BigDecimal valorSaidas;
    private BigDecimal valorLiquido;

    public ProdutoMovimento(String codigoBarras, String nome, int totalEntradas, int totalSaidas,
                            int saldoLiquido, BigDecimal valorEntradas, BigDecimal valorSaidas,
                            BigDecimal valorLiquido) {
        this.codigoBarras = codigoBarras;
        this.nome = nome;
        this.totalEntradas = totalEntradas;
        this.totalSaidas = totalSaidas;
        this.saldoLiquido = saldoLiquido;
        this.valorEntradas = valorEntradas;
        this.valorSaidas = valorSaidas;
        this.valorLiquido = valorLiquido;
    }

    // Getters
    public String getCodigoBarras() { return codigoBarras; }
    public String getNome() { return nome; }
    public int getTotalEntradas() { return totalEntradas; }
    public int getTotalSaidas() { return totalSaidas; }
    public int getSaldoLiquido() { return saldoLiquido; }
    public BigDecimal getValorEntradas() { return valorEntradas; }
    public BigDecimal getValorSaidas() { return valorSaidas; }
    public BigDecimal getValorLiquido() { return valorLiquido; }
}