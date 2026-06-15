package estoque.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Produto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private String codigoBarras;
    private String nome;
    private TipoProduto tipo;
    private int unidadesPorFardo;
    private int quantidadeFardos;
    private int quantidadeUnidades;
    private int quantidadeFardosInicial;
    private int quantidadeUnidadesInicial;
    private Integer categoriaId;
    private BigDecimal precoUnitario;       // 🆕 campo de preço

    private transient String categoriaNome;
    
    public enum TipoProduto { UNITARIO, FARDO }
    
    public Produto() {}
    
    public int getTotalUnidades() {
        return quantidadeFardos * unidadesPorFardo + quantidadeUnidades;
    }

    // Getters e Setters
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoProduto getTipo() { return tipo; }
    public void setTipo(TipoProduto tipo) { this.tipo = tipo; }

    public int getUnidadesPorFardo() { return unidadesPorFardo; }
    public void setUnidadesPorFardo(int unidadesPorFardo) { this.unidadesPorFardo = unidadesPorFardo; }

    public int getQuantidadeFardos() { return quantidadeFardos; }
    public void setQuantidadeFardos(int quantidadeFardos) { this.quantidadeFardos = quantidadeFardos; }

    public int getQuantidadeUnidades() { return quantidadeUnidades; }
    public void setQuantidadeUnidades(int quantidadeUnidades) { this.quantidadeUnidades = quantidadeUnidades; }
    
    public int getQuantidadeFardosInicial() { return quantidadeFardosInicial; }
    public void setQuantidadeFardosInicial(int quantidadeFardosInicial) { this.quantidadeFardosInicial = quantidadeFardosInicial; }
    
    public int getQuantidadeUnidadesInicial() { return quantidadeUnidadesInicial; }
    public void setQuantidadeUnidadesInicial(int quantidadeUnidadesInicial) { this.quantidadeUnidadesInicial = quantidadeUnidadesInicial; }
    
    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNome() { return categoriaNome; }
    public void setCategoriaNome(String categoriaNome) { this.categoriaNome = categoriaNome; }

    // 🆕 Getter e Setter do preço
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    @Override
    public String toString() {
        return codigoBarras + " - " + nome + " (Estoque: " + getTotalUnidades() + ")";
    }
}