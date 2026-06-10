package estoque.model;

public class ProdutoSaldo {
    private String codigoBarras;
    private String nome;
    private int fardos;
    private int unidadesAvulsas;
    private int totalUnidades;

    public ProdutoSaldo(String codigoBarras, String nome, int fardos, int unidadesAvulsas, int totalUnidades) {
        this.codigoBarras = codigoBarras;
        this.nome = nome;
        this.fardos = fardos;
        this.unidadesAvulsas = unidadesAvulsas;
        this.totalUnidades = totalUnidades;
    }

    public String getCodigoBarras() { return codigoBarras; }
    public String getNome() { return nome; }
    public int getFardos() { return fardos; }
    public int getUnidadesAvulsas() { return unidadesAvulsas; }
    public int getTotalUnidades() { return totalUnidades; }
}