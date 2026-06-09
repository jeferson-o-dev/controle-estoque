package estoque.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Movimentacao implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum TipoMovimento { ENTRADA, SAIDA }
    
    private int id;
    private LocalDateTime dataHora;
    private int produtoId;
    private String produtoNome;
    private TipoMovimento tipo;
    private int quantidadeUnidades;
    private String descricao;
    
    // Construtores
    public Movimentacao() {}
    
    public Movimentacao(int produtoId, String produtoNome, TipoMovimento tipo, 
                        int quantidadeUnidades, String descricao) {
        this.dataHora = LocalDateTime.now();
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.tipo = tipo;
        this.quantidadeUnidades = quantidadeUnidades;
        this.descricao = descricao;
    }
    
    // Getters e Setters (gerados automaticamente - você pode gerar com Source > Generate Getters and Setters)
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    
    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }
    
    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }
    
    public TipoMovimento getTipo() { return tipo; }
    public void setTipo(TipoMovimento tipo) { this.tipo = tipo; }
    
    public int getQuantidadeUnidades() { return quantidadeUnidades; }
    public void setQuantidadeUnidades(int quantidadeUnidades) { this.quantidadeUnidades = quantidadeUnidades; }
    
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
}