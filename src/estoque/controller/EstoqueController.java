package estoque.controller;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
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

        String sql = "INSERT INTO produtos (codigo_barras, nome, tipo, unidades_por_fardo, " +
                "quantidade_fardos, quantidade_unidades, quantidade_fardos_inicial, " +
                "quantidade_unidades_inicial, categoria_id, preco_unitario) VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, p.getCodigoBarras().trim());
                stmt.setString(2, p.getNome());
                stmt.setString(3, p.getTipo().name());
                stmt.setInt(4, p.getUnidadesPorFardo());
                stmt.setInt(5, p.getQuantidadeFardos());
                stmt.setInt(6, p.getQuantidadeUnidades());
                stmt.setInt(7, p.getQuantidadeFardos());
                stmt.setInt(8, p.getQuantidadeUnidades());

                if (p.getCategoriaId() != null) {
                    stmt.setInt(9, p.getCategoriaId());
                } else {
                    stmt.setNull(9, java.sql.Types.INTEGER);
                }

                if (p.getPrecoUnitario() != null) {
                    stmt.setBigDecimal(10, p.getPrecoUnitario());
                } else {
                    stmt.setNull(10, java.sql.Types.DECIMAL);
                }

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setId(rs.getInt(1));
                    }
                }
            }

            // Registra a movimentação inicial COM o preço
            registrarMovimentacao(conn, p, Movimentacao.TipoMovimento.ENTRADA,
                    p.getTotalUnidades(), "Produto cadastrado", p.getPrecoUnitario());

            conn.commit();
        } catch (SQLException e) {
            System.err.println("Erro em adicionarProduto: " + e.getMessage());
            throw new RuntimeException("Falha ao cadastrar o produto. Tente novamente.", e);
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

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            Produto atual = buscarProdutoPorId(conn, p.getId());
            if (atual == null) {
                conn.rollback();
                return false;
            }

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
                conn.rollback();
                return false;
            }

            atualizarProdutoNoBanco(conn, atual);
            registrarMovimentacao(conn, atual, Movimentacao.TipoMovimento.SAIDA,
                    unidadesSolicitadas, "Remoção de " + unidadesSolicitadas + " unidade(s)",
                    atual.getPrecoUnitario());

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro em removerQuantidade: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ---------- ADICIONAR QUANTIDADE (entrada rápida) ----------
    public void adicionarQuantidade(Produto p, int quantidade) {
        verificarConexao();
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            Produto atual = buscarProdutoPorId(conn, p.getId());
            if (atual == null) {
                conn.rollback();
                return;
            }

            atual.setQuantidadeUnidades(atual.getQuantidadeUnidades() + quantidade);
            atualizarProdutoNoBanco(conn, atual);
            registrarMovimentacao(conn, atual, Movimentacao.TipoMovimento.ENTRADA, quantidade,
                    "Entrada por leitor: +" + quantidade, atual.getPrecoUnitario());

            conn.commit();
        } catch (SQLException e) {
            System.err.println("Erro em adicionarQuantidade: " + e.getMessage());
            throw new RuntimeException("Falha ao adicionar quantidade. Tente novamente.", e);
        }
    }

    // ---------- ADICIONAR FARDOS ----------
    public void adicionarFardos(Produto p, int qtdFardos) {
        verificarConexao();
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            Produto atual = buscarProdutoPorId(conn, p.getId());
            if (atual == null) {
                conn.rollback();
                return;
            }

            atual.setQuantidadeFardos(atual.getQuantidadeFardos() + qtdFardos);
            int totalUnidadesAdicionadas = qtdFardos * atual.getUnidadesPorFardo();
            atualizarProdutoNoBanco(conn, atual);
            registrarMovimentacao(conn, atual, Movimentacao.TipoMovimento.ENTRADA,
                    totalUnidadesAdicionadas,
                    "Entrada por leitor: +" + qtdFardos + " fardo(s) (" + totalUnidadesAdicionadas + " unid.)",
                    atual.getPrecoUnitario());

            conn.commit();
        } catch (SQLException e) {
            System.err.println("Erro em adicionarFardos: " + e.getMessage());
            throw new RuntimeException("Falha ao adicionar fardos. Tente novamente.", e);
        }
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

    // Nova versão que recebe a conexão e o preço unitário
    private void registrarMovimentacao(Connection conn, Produto p, Movimentacao.TipoMovimento tipo,
            int quantidade, String descricao, BigDecimal precoUnitario) throws SQLException {
        String sql = "INSERT INTO movimentacoes (data_hora, produto_id, produto_nome, tipo, quantidade, descricao, preco_unitario) VALUES (GETDATE(), ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getId());
            stmt.setString(2, p.getNome());
            stmt.setString(3, tipo.name());
            stmt.setInt(4, quantidade);
            stmt.setString(5, descricao);
            if (precoUnitario != null) {
                stmt.setBigDecimal(6, precoUnitario);
            } else {
                stmt.setNull(6, java.sql.Types.DECIMAL);
            }
            stmt.executeUpdate();
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
                m.setPrecoUnitario(rs.getBigDecimal("preco_unitario")); // carrega o preço também
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
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return carregarProduto(rs);
                }
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

    private Produto buscarProdutoPorId(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM produtos WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return carregarProduto(rs);
                }
            }
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
        p.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
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

    private void atualizarProdutoNoBanco(Connection conn, Produto p) throws SQLException {
        String sql = "UPDATE produtos SET quantidade_fardos = ?, quantidade_unidades = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getQuantidadeFardos());
            stmt.setInt(2, p.getQuantidadeUnidades());
            stmt.setInt(3, p.getId());
            stmt.executeUpdate();
        }
    }

    private void verificarConexao() {
        if (!DatabaseConnection.isConnectionAvailable()) {
            throw new RuntimeException("Sem conexão com o banco de dados.");
        }
    }

    // GET MOVIMENTACOES POR PERIODO SEM FILTRO
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

    // ESTADO DO ESTOQUE NA DATA (ORIGINAL) – AGORA COM PREÇO HISTÓRICO
    public List<ProdutoSaldo> getEstadoEstoqueNaData(LocalDate data) {
        List<ProdutoSaldo> resultado = new ArrayList<>();
        String sql = """
            SELECT p.id, p.codigo_barras, p.nome, p.tipo, p.unidades_por_fardo,
                   COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END), 0) AS saldo,
                   COALESCE(
                       (SELECT TOP 1 m2.preco_unitario FROM movimentacoes m2
                        WHERE m2.produto_id = p.id AND m2.data_hora < ? AND m2.preco_unitario IS NOT NULL
                        ORDER BY m2.data_hora DESC),
                       p.preco_unitario
                   ) AS preco_unitario,
                   COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END), 0) *
                   COALESCE(
                       (SELECT TOP 1 m2.preco_unitario FROM movimentacoes m2
                        WHERE m2.produto_id = p.id AND m2.data_hora < ? AND m2.preco_unitario IS NOT NULL
                        ORDER BY m2.data_hora DESC),
                       p.preco_unitario,
                       0
                   ) AS valor_total
            FROM produtos p
            LEFT JOIN movimentacoes m ON p.id = m.produto_id AND m.data_hora < ?
            WHERE EXISTS (
                SELECT 1 FROM movimentacoes m3
                WHERE m3.produto_id = p.id AND m3.data_hora < ?
            )
            GROUP BY p.id, p.codigo_barras, p.nome, p.tipo, p.unidades_por_fardo, p.preco_unitario
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String dataLimite = data.plusDays(1).atStartOfDay()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            // 4 parâmetros na ordem: subconsulta preço, subconsulta valor_total, LEFT JOIN, EXISTS
            stmt.setString(1, dataLimite);
            stmt.setString(2, dataLimite);
            stmt.setString(3, dataLimite);
            stmt.setString(4, dataLimite);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String codigo = rs.getString("codigo_barras");
                String nome = rs.getString("nome");
                int unidadesPorFardo = rs.getInt("unidades_por_fardo");
                int saldo = rs.getInt("saldo");
                BigDecimal preco = rs.getBigDecimal("preco_unitario");
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");

                int fardos = 0;
                int unidadesAvulsas = 0;
                if (unidadesPorFardo > 0) {
                    fardos = saldo / unidadesPorFardo;
                    unidadesAvulsas = saldo % unidadesPorFardo;
                } else {
                    unidadesAvulsas = saldo;
                }
                resultado.add(new ProdutoSaldo(codigo, nome, fardos, unidadesAvulsas, saldo, preco, valorTotal));
            }
        } catch (SQLException e) {
            System.err.println("Erro em getEstadoEstoqueNaData (original): " + e.getMessage());
            throw new RuntimeException("Erro ao consultar estado do estoque. Tente novamente.", e);
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

    // Relatório de movimentações com filtro (mantido, sem alteração)
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
            sql += " AND LOWER(m.produto_nome) LIKE LOWER(?)";
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
            throw new RuntimeException("Erro ao consultar movimentações. Tente novamente.", e);
        }
        return lista;
    }

    // Estado do estoque na data COM FILTROS – AGORA COM PREÇO HISTÓRICO
    public List<ProdutoSaldo> getEstadoEstoqueNaData(LocalDate data, String nomeProduto, Integer categoriaId, boolean apenasZerados) {
        List<ProdutoSaldo> resultado = new ArrayList<>();
        String base = """
            SELECT p.id, p.codigo_barras, p.nome, p.tipo, p.unidades_por_fardo,
                   COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END), 0) AS saldo,
                   COALESCE(
                       (SELECT TOP 1 m2.preco_unitario FROM movimentacoes m2
                        WHERE m2.produto_id = p.id AND m2.data_hora < ? AND m2.preco_unitario IS NOT NULL
                        ORDER BY m2.data_hora DESC),
                       p.preco_unitario
                   ) AS preco_unitario,
                   COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.quantidade ELSE -m.quantidade END), 0) *
                   COALESCE(
                       (SELECT TOP 1 m2.preco_unitario FROM movimentacoes m2
                        WHERE m2.produto_id = p.id AND m2.data_hora < ? AND m2.preco_unitario IS NOT NULL
                        ORDER BY m2.data_hora DESC),
                       p.preco_unitario,
                       0
                   ) AS valor_total
            FROM produtos p
            LEFT JOIN movimentacoes m ON p.id = m.produto_id AND m.data_hora < ?
            WHERE 1=1
            """;
        List<Object> params = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dataLimite = data.plusDays(1).atStartOfDay().format(fmt);

        // Parâmetro 1: subconsulta do preço
        // Parâmetro 2: subconsulta do valor_total
        // Parâmetro 3: LEFT JOIN
        params.add(dataLimite); // índice 1
        params.add(dataLimite); // índice 2
        params.add(dataLimite); // índice 3

        if (nomeProduto != null && !nomeProduto.trim().isEmpty()) {
            base += " AND LOWER(p.nome) LIKE LOWER(?)";
            params.add("%" + nomeProduto.trim() + "%");
        }
        if (categoriaId != null) {
            base += " AND p.categoria_id = ?";
            params.add(categoriaId);
        }
        base += " GROUP BY p.id, p.codigo_barras, p.nome, p.tipo, p.unidades_por_fardo, p.preco_unitario";

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
                BigDecimal preco = rs.getBigDecimal("preco_unitario");
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");

                int fardos = 0;
                int unidadesAvulsas = 0;
                if (unidadesPorFardo > 0) {
                    fardos = saldo / unidadesPorFardo;
                    unidadesAvulsas = saldo % unidadesPorFardo;
                } else {
                    unidadesAvulsas = saldo;
                }
                resultado.add(new ProdutoSaldo(codigo, nome, fardos, unidadesAvulsas, saldo, preco, valorTotal));
            }
        } catch (SQLException e) {
            System.err.println("Erro em getEstadoEstoqueNaData (com filtros): " + e.getMessage());
            throw new RuntimeException("Erro ao consultar estado do estoque. Tente novamente.", e);
        }
        return resultado;
    }
}