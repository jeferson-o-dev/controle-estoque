package estoque.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import estoque.database.DatabaseConnection;
import estoque.model.Categoria;
import estoque.model.Movimentacao;
import estoque.model.Produto;
import estoque.model.ProdutoSaldo;

public class EstoqueController {

    public EstoqueController() {
        System.out.println("=== EstoqueController (banco) iniciado ===");
        DatabaseConnection.criarTabelas();
        DatabaseConnection.verificarColunas();
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

        String sql = "INSERT INTO produtos (codigo_barras, nome, tipo, unidades_por_fardo, quantidade_fardos, quantidade_unidades, quantidade_fardos_inicial, quantidade_unidades_inicial, categoria_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, p.getCodigoBarras().trim());
            stmt.setString(2, p.getNome());
            stmt.setString(3, p.getTipo().name());
            stmt.setInt(4, p.getUnidadesPorFardo());
            stmt.setInt(5, p.getQuantidadeFardos());
            stmt.setInt(6, p.getQuantidadeUnidades());
            stmt.setInt(7, p.getQuantidadeFardos());      // inicial
            stmt.setInt(8, p.getQuantidadeUnidades());    // inicial

            if (p.getCategoriaId() != null) {
                stmt.setInt(9, p.getCategoriaId());
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }

            registrarMovimentacao(p, Movimentacao.TipoMovimento.ENTRADA,
                    p.getTotalUnidades(), "Produto cadastrado");

        } catch (SQLException e) {
            System.err.println("Erro em adicionarProduto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- LISTAR PRODUTOS ----------
    public List<Produto> listarProdutos() {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT p.*, c.nome AS categoria_nome FROM produtos p LEFT JOIN categorias c ON p.categoria_id = c.id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Produto p = carregarProduto(rs);
                p.setCategoriaNome(rs.getString("categoria_nome"));
                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erro em listarProdutos: " + e.getMessage());
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
            System.err.println("Erro em excluirProdutoPorCodigo: " + e.getMessage());
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
            System.err.println("Erro em registrarMovimentacao: " + e.getMessage());
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
                m.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
                m.setProdutoId(rs.getInt("produto_id"));
                m.setProdutoNome(rs.getString("produto_nome"));
                m.setTipo(Movimentacao.TipoMovimento.valueOf(rs.getString("tipo")));
                m.setQuantidadeUnidades(rs.getInt("quantidade"));
                m.setDescricao(rs.getString("descricao"));
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Erro em getMovimentacoes: " + e.getMessage());
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
            System.err.println("Erro em buscarProdutoPorCodigoBarras: " + e.getMessage());
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
            System.err.println("Erro em buscarProdutoPorId: " + e.getMessage());
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
        p.setQuantidadeFardosInicial(rs.getInt("quantidade_fardos_inicial"));
        p.setQuantidadeUnidadesInicial(rs.getInt("quantidade_unidades_inicial"));
        p.setCategoriaId(rs.getObject("categoria_id", Integer.class));
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
            System.err.println("Erro em atualizarProdutoNoBanco: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void verificarConexao() {
        if (!DatabaseConnection.isConnectionAvailable()) {
            throw new RuntimeException("Sem conexão com o banco de dados.");
        }
    }

    public List<Movimentacao> getMovimentacoesPorPeriodo(LocalDate inicio, LocalDate fim) {
        List<Movimentacao> lista = new ArrayList<>();
        String sql = "SELECT * FROM movimentacoes WHERE data_hora >= ? AND data_hora < ? ORDER BY data_hora";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, inicio.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            stmt.setString(2, fim.plusDays(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Movimentacao m = new Movimentacao();
                m.setId(rs.getInt("id"));
                m.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
                m.setProdutoId(rs.getInt("produto_id"));
                m.setProdutoNome(rs.getString("produto_nome"));
                m.setTipo(Movimentacao.TipoMovimento.valueOf(rs.getString("tipo")));
                m.setQuantidadeUnidades(rs.getInt("quantidade"));
                m.setDescricao(rs.getString("descricao"));
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Erro em getMovimentacoesPorPeriodo (sem filtros): " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    // Dentro de estoque/controller/EstoqueController.java
    public List<ProdutoSaldo> getEstadoEstoqueNaData(LocalDate data) {
        List<ProdutoSaldo> resultado = new ArrayList<>();
        String sql = """
                    SELECT p.id, p.codigo_barras, p.nome, p.tipo, p.unidades_por_fardo,
                           COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END), 0) AS saldo
                    FROM produtos p
                    LEFT JOIN movimentacoes m ON p.id = m.produto_id AND m.data_hora < ?
                    WHERE EXISTS (
                        SELECT 1 FROM movimentacoes m2
                        WHERE m2.produto_id = p.id AND m2.data_hora < ?
                    )
                    GROUP BY p.id, p.codigo_barras, p.nome, p.tipo, p.unidades_por_fardo
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, data.plusDays(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            stmt.setString(2, data.plusDays(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String codigo = rs.getString("codigo_barras");
                String nome = rs.getString("nome");
                int unidadesPorFardo = rs.getInt("unidades_por_fardo");
                int saldo = rs.getInt("saldo");

                int fardos = 0;
                int unidadesAvulsas = 0;
                if (unidadesPorFardo > 0) {
                    fardos = saldo / unidadesPorFardo;
                    unidadesAvulsas = saldo % unidadesPorFardo;
                } else {
                    unidadesAvulsas = saldo;
                }
                resultado.add(new ProdutoSaldo(codigo, nome, fardos, unidadesAvulsas, saldo));
            }
        } catch (SQLException e) {
            System.err.println("Erro em getEstadoEstoqueNaData (original): " + e.getMessage());
            e.printStackTrace();
        }
        return resultado;
    }

    // ---------- CATEGORIAS ----------
    public List<Categoria> listarCategorias() {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT id, nome FROM categorias ORDER BY nome";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Categoria(rs.getInt("id"), rs.getString("nome")));
            }
        } catch (SQLException e) {
            System.err.println("Erro em listarCategorias: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public Categoria adicionarCategoria(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório.");
        }
        String sql = "INSERT INTO categorias (nome) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nome.trim());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Categoria(rs.getInt(1), nome.trim());
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                throw new IllegalArgumentException("Já existe uma categoria com esse nome.");
            }
            System.err.println("Erro em adicionarCategoria: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean removerCategoria(int id) {
        // Verifica se há produtos usando a categoria
        String sqlCheck = "SELECT COUNT(*) FROM produtos WHERE categoria_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlCheck)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new IllegalArgumentException("Não é possível excluir: há produtos vinculados.");
            }
        } catch (SQLException e) {
            System.err.println("Erro na verificação de produtos vinculados em removerCategoria: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        String sql = "DELETE FROM categorias WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar categoria em removerCategoria: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Relatório de movimentações com filtro
    public List<Movimentacao> getMovimentacoesPorPeriodo(LocalDate inicio, LocalDate fim, String nomeProduto, Integer categoriaId) {
        List<Movimentacao> lista = new ArrayList<>();
        String sql = """
        	    SELECT m.* FROM movimentacoes m
        	    LEFT JOIN produtos p ON m.produto_id = p.id
        	    WHERE m.data_hora >= ? AND m.data_hora < ?
        	    """;
        List<Object> params = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        params.add(inicio.atStartOfDay().format(fmt));
        params.add(fim.plusDays(1).atStartOfDay().format(fmt));

        if (nomeProduto != null && !nomeProduto.trim().isEmpty()) {
            sql += " AND LOWER(p.nome) LIKE LOWER(?)";
            params.add("%" + nomeProduto.trim() + "%");
        }
        if (categoriaId != null) {
            sql += " AND p.categoria_id = ?";
            params.add(categoriaId);
        }
        sql += " ORDER BY m.data_hora";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object val = params.get(i);
                if (val instanceof String) stmt.setString(i+1, (String) val);
                else if (val instanceof Integer) stmt.setInt(i+1, (Integer) val);
                else stmt.setObject(i+1, val);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Movimentacao m = new Movimentacao();
                m.setId(rs.getInt("id"));
                m.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
                m.setProdutoId(rs.getInt("produto_id"));
                m.setProdutoNome(rs.getString("produto_nome"));
                m.setTipo(Movimentacao.TipoMovimento.valueOf(rs.getString("tipo")));
                m.setQuantidadeUnidades(rs.getInt("quantidade"));
                m.setDescricao(rs.getString("descricao"));
                lista.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Erro em getMovimentacoesPorPeriodo (com filtros): " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    // Estado do estoque na data com filtros
    public List<ProdutoSaldo> getEstadoEstoqueNaData(LocalDate data, String nomeProduto, Integer categoriaId, boolean apenasZerados) {
        List<ProdutoSaldo> resultado = new ArrayList<>();
        String base = """
            SELECT p.id, p.codigo_barras, p.nome, p.tipo, p.unidades_por_fardo,
                   COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END), 0) AS saldo
            FROM produtos p
            LEFT JOIN movimentacoes m ON p.id = m.produto_id AND m.data_hora < ?
            WHERE 1=1
            """;
        List<Object> params = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dataLimite = data.plusDays(1).atStartOfDay().format(fmt);
        params.add(dataLimite);

        if (nomeProduto != null && !nomeProduto.trim().isEmpty()) {
            base += " AND LOWER(p.nome) LIKE LOWER(?)";
            params.add("%" + nomeProduto.trim() + "%");
        }
        if (categoriaId != null) {
            base += " AND p.categoria_id = ?";
            params.add(categoriaId);
        }
        base += " GROUP BY p.id, p.codigo_barras, p.nome, p.tipo, p.unidades_por_fardo";

        if (apenasZerados) {
            base += " HAVING COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END), 0) = 0";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(base)) {
            for (int i = 0; i < params.size(); i++) {
                Object val = params.get(i);
                if (val instanceof String) stmt.setString(i + 1, (String) val);
                else if (val instanceof Integer) stmt.setInt(i + 1, (Integer) val);
                else stmt.setObject(i + 1, val);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String codigo = rs.getString("codigo_barras");
                String nome = rs.getString("nome");
                int unidadesPorFardo = rs.getInt("unidades_por_fardo");
                int saldo = rs.getInt("saldo");

                int fardos = 0;
                int unidadesAvulsas = 0;
                if (unidadesPorFardo > 0) {
                    fardos = saldo / unidadesPorFardo;
                    unidadesAvulsas = saldo % unidadesPorFardo;
                } else {
                    unidadesAvulsas = saldo;
                }
                resultado.add(new ProdutoSaldo(codigo, nome, fardos, unidadesAvulsas, saldo));
            }
        } catch (SQLException e) {
            System.err.println("Erro em getEstadoEstoqueNaData (com filtros): " + e.getMessage());
            e.printStackTrace();
        }
        return resultado;
    }
}