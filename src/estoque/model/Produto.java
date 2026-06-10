package estoque.model;

import java.io.Serializable;

public class Produto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private String codigoBarras;
    private String nome;
    private TipoProduto tipo;
    private int unidadesPorFardo; // usado apenas se tipo == FARDO
    private int quantidadeFardos;  // fardos fechados em estoque
    private int quantidadeUnidades; // unidades avulsas
    private int quantidadeFardosInicial;
    private int quantidadeUnidadesInicial;
    
    public enum TipoProduto { UNITARIO, FARDO }
    
    // Construtores (o padrão já basta)
    public Produto() {}
    
    // Método para obter total de unidades (incluindo fardos fechados)
    public int getTotalUnidades() {
        return quantidadeFardos * unidadesPorFardo + quantidadeUnidades;
    }

    // Getters e Setters (já existentes, mantidos)
    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoProduto getTipo() {
        return tipo;
    }

    public void setTipo(TipoProduto tipo) {
        this.tipo = tipo;
    }

    public int getUnidadesPorFardo() {
        return unidadesPorFardo;
    }

    public void setUnidadesPorFardo(int unidadesPorFardo) {
        this.unidadesPorFardo = unidadesPorFardo;
    }

    public int getQuantidadeFardos() {
        return quantidadeFardos;
    }

    public void setQuantidadeFardos(int quantidadeFardos) {
        this.quantidadeFardos = quantidadeFardos;
    }

    public int getQuantidadeUnidades() {
        return quantidadeUnidades;
    }

    public void setQuantidadeUnidades(int quantidadeUnidades) {
        this.quantidadeUnidades = quantidadeUnidades;
    }
    
    public int getQuantidadeFardosInicial() {
        return quantidadeFardosInicial;
    }
    public void setQuantidadeFardosInicial(int quantidadeFardosInicial) {
        this.quantidadeFardosInicial = quantidadeFardosInicial;
    }
    public int getQuantidadeUnidadesInicial() {
        return quantidadeUnidadesInicial;
    }
    public void setQuantidadeUnidadesInicial(int quantidadeUnidadesInicial) {
        this.quantidadeUnidadesInicial = quantidadeUnidadesInicial;
    }

    // Método toString para exibição amigável (usado no JComboBox e no leitor)
    @Override
    public String toString() {
        return codigoBarras + " - " + nome + " (Estoque: " + getTotalUnidades() + ")";
    }
}