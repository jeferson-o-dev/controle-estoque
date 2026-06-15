package estoque.database;

import java.sql.*;

public class DatabaseConnection {

    // Configurações do SQL Server (padrão local; no servidor, ajuste as variáveis de ambiente)
    private static final String HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String PORT = System.getenv().getOrDefault("DB_PORT", "1433");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "estoque");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "estoque_app");
    private static final String PASS = System.getenv().getOrDefault("DB_PASS", "123");

    private static final String URL = String.format(
        "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true;",
        HOST, PORT, DB_NAME
    );

    // ========== CONEXÃO ==========

    public static Connection getConnection() throws SQLException {
        System.out.println("Conectando ao SQL Server: " + URL);
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ========== CRIAÇÃO DE TABELAS ==========

    public static void criarTabelas() {
        String sqlProdutos = """
            CREATE TABLE produtos (
                id INT IDENTITY(1,1) PRIMARY KEY,
                codigo_barras VARCHAR(50) NOT NULL UNIQUE,
                nome VARCHAR(100) NOT NULL,
                tipo VARCHAR(20) NOT NULL,
                unidades_por_fardo INT,
                quantidade_fardos INT,
                quantidade_unidades INT,
                quantidade_fardos_inicial INT,
                quantidade_unidades_inicial INT
            )
        """;

        String sqlMovimentacoes = """
            CREATE TABLE movimentacoes (
                id INT IDENTITY(1,1) PRIMARY KEY,
                data_hora DATETIME2 NOT NULL,
                produto_id INT NOT NULL,
                produto_nome VARCHAR(100) NOT NULL,
                tipo VARCHAR(20) NOT NULL,
                quantidade INT NOT NULL,
                descricao VARCHAR(500),
                preco_unitario DECIMAL(10,2)
            )
        """;

        String sqlCategorias = """
            CREATE TABLE categorias (
                id INT IDENTITY(1,1) PRIMARY KEY,
                nome VARCHAR(100) NOT NULL UNIQUE
            )
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            if (!existeTabela(conn, "produtos")) {
                stmt.execute(sqlProdutos);
                System.out.println("Tabela 'produtos' criada.");
            } else {
                System.out.println("Tabela 'produtos' já existe.");
            }

            if (!existeTabela(conn, "movimentacoes")) {
                stmt.execute(sqlMovimentacoes);
                System.out.println("Tabela 'movimentacoes' criada.");
            } else {
                System.out.println("Tabela 'movimentacoes' já existe.");
            }

            if (!existeTabela(conn, "categorias")) {
                stmt.execute(sqlCategorias);
                System.out.println("Tabela 'categorias' criada.");
                inserirCategoriasPadrao(conn);
            } else {
                System.out.println("Tabela 'categorias' já existe.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // ========== VERIFICAÇÃO DE TABELA ==========

    private static boolean existeTabela(Connection conn, String nomeTabela) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, nomeTabela, null)) {
            return rs.next();
        }
    }

    // ========== VERIFICAÇÃO DE CONEXÃO ==========

    public static boolean isConnectionAvailable() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ========== VERIFICAR / ADICIONAR COLUNAS ==========

    public static void verificarColunas() {
        // Coluna de preço unitário nas movimentações
        garantirColuna("movimentacoes", "preco_unitario", "DECIMAL(10,2)");
        // Colunas iniciais (caso já exista tabela criada por versão anterior)
        garantirColuna("produtos", "quantidade_fardos_inicial", "INT");
        garantirColuna("produtos", "quantidade_unidades_inicial", "INT");
        garantirColuna("produtos", "categoria_id", "INT NULL");
        garantirColuna("produtos", "preco_unitario", "DECIMAL(10,2)");
    }

    private static void garantirColuna(String tabela, String coluna, String tipo) {
        if (!colunaExiste(tabela, coluna)) {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + tabela + " ADD " + coluna + " " + tipo);
                System.out.println("Coluna '" + coluna + "' adicionada em '" + tabela + "'.");
            } catch (SQLException e) {
                String msg = "Erro crítico ao verificar/adicionar a coluna '" + coluna + "' na tabela '" + tabela + "'. Verifique as permissões do banco de dados.";
                System.err.println(msg);
                e.printStackTrace();
                throw new RuntimeException(msg, e);
            }
        } else {
            System.out.println("Coluna '" + coluna + "' já existe em '" + tabela + "'.");
        }
    }

    private static boolean colunaExiste(String tabela, String coluna) {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tabela);
            stmt.setString(2, coluna);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private static void inserirCategoriasPadrao(Connection conn) throws SQLException {
        String[] padroes = {"Bebidas Alcoólicas", "Bebidas Não Alcoólicas", "Carnes",
                            "Laticínios", "Legumes", "Verduras", "Enlatados",
                            "Grãos", "Limpeza", "Higiene Pessoal"};
        String sql = "INSERT INTO categorias (nome) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String nome : padroes) {
                stmt.setString(1, nome);
                stmt.executeUpdate();
            }
            System.out.println("Categorias padrão inseridas.");
        }
    }
}