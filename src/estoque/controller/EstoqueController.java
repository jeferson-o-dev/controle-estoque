package estoque.controller;

import estoque.model.Produto;
import estoque.model.Movimentacao;
import estoque.database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EstoqueController {

    public EstoqueController() {
        System.out.println("=== EstoqueController (banco) iniciado ===");
        DatabaseConnection.criarTabelas();
    }

    // ---------- ADICIONAR PRODUTO ----------
    public void adicionarProduto(Produto p) {
        verificarConexao();
        if (p.getCodigoBarras() == null || p.getCodigoBarras().trim().isEmpty()) {
            throw new IllegalArgumentException("Código de barras é obrigatório.");
        }
        if (existeProdutoComCodigo(p.getCodigoBarras().trim())) {
            throw new IllegalArgumentException("Já existe um produto com esse código de barras.");
        }

        String sql = "INSERT INTO produtos (codigo_barras, nome, tipo, unidades_por_fardo, quantidade_fardos, quantidade_unidades) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, p.getCodigoBarras().trim());
            stmt.setString(2, p.getNome());
            stmt.setString(3, p.getTipo().name());
            stmt.setInt(4, p.getUnidadesPorFardo());
            stmt.setInt(5, p.getQuantidadeFardos());
            stmt.setInt(6, p.getQuantidadeUnidades());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }

            registrarMovimentacao(p, Movimentacao.TipoMovimento.ENTRADA,
                    p.getTotalUnidades(), "Produto cadastrado");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- LISTAR PRODUTOS ----------
    public List<Produto> listarProdutos() {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Produto p = new Produto();
                p.setId(rs.getInt("id"));
                p.setCodigoBarras(rs.getString("codigo_barras"));
                p.setNome(rs.getString("nome"));
                p.setTipo(Produto.TipoProduto.valueOf(rs.getString("tipo")));
                p.setUnidadesPorFardo(rs.getInt("unidades_por_fardo"));
                p.setQuantidadeFardos(rs.getInt("quantidade_fardos"));
                p.setQuantidadeUnidades(rs.getInt("quantidade_unidades"));
                lista.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ---------- REMOVER QUANTIDADE ----------
    public boolean removerQuantidade(Produto p, int unidadesSolicitadas) {
        verificarConexao();
        if (unidadesSolicitadas <= 0) return false;

        Produto atual = buscarProdutoPorId(p.getId());
        if (atual == null) return false;

        int faltam = unidadesSolicitadas;

        int usarUnidades = Math.min(atual.getQuantidadeUnidades(), faltam);
        atual.setQuantidadeUnidades(atual.getQuantidadeUnidades() - usarUnidades);
        faltam -= usarUnidades;

        while (faltam > 0 && atual.getQuantidadeFardos() > 0) {
            atual.setQuantidadeFardos(atual.getQuantidadeFardos() - 1);
            atual.setQuantidadeUnidades(atual.getQuantidadeUnidades() + atual.getUnidadesPorFardo());
            int usarDesteFardo = Math.min(atual.getQuantidadeUnidades(), faltam);
            atual.setQuantidadeUnidades(atual.getQuantidadeUnidades() - usarDesteFardo);
            faltam -= usarDesteFardo;
        }

        if (faltam > 0) {
            return false;
        }

        atualizarProdutoNoBanco(atual);
        registrarMovimentacao(atual, Movimentacao.TipoMovimento.SAIDA,
                unidadesSolicitadas, "Remoção de " + unidadesSolicitadas + " unidade(s)");
        return true;
    }

    // ---------- ADICIONAR QUANTIDADE (entrada rápida) ----------
    public void adicionarQuantidade(Produto p, int quantidade) {
        verificarConexao();
        Produto atual = buscarProdutoPorId(p.getId());
        if (atual == null) return;
        atual.setQuantidadeUnidades(atual.getQuantidadeUnidades() + quantidade);
        atualizarProdutoNoBanco(atual);
        registrarMovimentacao(atual, Movimentacao.TipoMovimento.ENTRADA, quantidade,
                "Entrada por leitor: +" + quantidade);
    }

    // ---------- ADICIONAR FARDOS ----------
    public void adicionarFardos(Produto p, int qtdFardos) {
        verificarConexao();
        Produto atual = buscarProdutoPorId(p.getId());
        if (atual == null) return;
        atual.setQuantidadeFardos(atual.getQuantidadeFardos() + qtdFardos);
        atualizarProdutoNoBanco(atual);
        registrarMovimentacao(atual, Movimentacao.TipoMovimento.ENTRADA,
                qtdFardos * atual.getUnidadesPorFardo(),
                "Entrada por leitor: +" + qtdFardos + " fardo(s) (" + (qtdFardos * atual.getUnidadesPorFardo()) + " unid.)");
    }

    // ---------- EXCLUSÃO POR CÓDIGO DE BARRAS ----------
    public boolean excluirProdutoPorCodigo(String codigo) {
        verificarConexao();
        String sql = "DELETE FROM produtos WHERE codigo_barras = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------- MOVIMENTAÇÕES ----------
    private void registrarMovimentacao(Produto p, Movimentacao.TipoMovimento tipo,
                                       int quantidade, String descricao) {
        String sql = "INSERT INTO movimentacoes (data_hora, produto_id, produto_nome, tipo, quantidade, descricao) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            stmt.setInt(2, p.getId());
            stmt.setString(3, p.getNome());
            stmt.setString(4, tipo.name());
            stmt.setInt(5, quantidade);
            stmt.setString(6, descricao);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Movimentacao> getMovimentacoes() {
        List<Movimentacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM movimentacoes ORDER BY data_hora DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Movimentacao m = new Movimentacao();
                m.setId(rs.getInt("id"));
                m.setDataHora(LocalDateTime.parse(rs.getString("data_hora"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")));
                m.setProdutoId(rs.getInt("produto_id"));
                m.setProdutoNome(rs.getString("produto_nome"));
                m.setTipo(Movimentacao.TipoMovimento.valueOf(rs.getString("tipo")));
                m.setQuantidadeUnidades(rs.getInt("quantidade"));
                m.setDescricao(rs.getString("descricao"));
                lista.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Total de movimentações lidas: " + lista.size());
        return lista;
    }

    // ---------- MÉTODOS AUXILIARES ----------
    public Produto buscarProdutoPorCodigoBarras(String codigo) {
        String sql = "SELECT * FROM produtos WHERE codigo_barras = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return carregarProduto(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean existeProdutoComCodigo(String codigo) {
        return buscarProdutoPorCodigoBarras(codigo) != null;
    }

    private Produto buscarProdutoPorId(int id) {
        String sql = "SELECT * FROM produtos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return carregarProduto(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Produto carregarProduto(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNome(rs.getString("nome"));
        p.setTipo(Produto.TipoProduto.valueOf(rs.getString("tipo")));
        p.setUnidadesPorFardo(rs.getInt("unidades_por_fardo"));
        p.setQuantidadeFardos(rs.getInt("quantidade_fardos"));
        p.setQuantidadeUnidades(rs.getInt("quantidade_unidades"));
        return p;
    }

    private void atualizarProdutoNoBanco(Produto p) {
        verificarConexao();
        String sql = "UPDATE produtos SET quantidade_fardos = ?, quantidade_unidades = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getQuantidadeFardos());
            stmt.setInt(2, p.getQuantidadeUnidades());
            stmt.setInt(3, p.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void verificarConexao() {
        if (!DatabaseConnection.isConnectionAvailable()) {
            throw new RuntimeException("Sem conexão com o banco de dados.");
        }
    }
}