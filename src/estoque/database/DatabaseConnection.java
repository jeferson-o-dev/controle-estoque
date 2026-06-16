package estoque.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseConnection {
	private static final Properties props = new Properties();

	static {
	    // 1. Tenta carregar de arquivo externo (mesma pasta do JAR) – TEM PRIORIDADE
	    File externalFile = new File("config.properties");
	    if (externalFile.exists()) {
	        try (InputStream input = new FileInputStream(externalFile)) {
	            props.load(input);
	            System.out.println("Arquivo config.properties carregado da pasta do JAR.");
	        } catch (IOException e) {
	            System.err.println("Erro ao ler config.properties externo: " + e.getMessage());
	        }
	    } else {
	        // 2. Se não encontrou externo, tenta classpath (dentro do JAR ou src)
	        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
	            if (input != null) {
	                props.load(input);
	                System.out.println("Arquivo config.properties carregado do classpath.");
	            } else {
	                System.out.println("Arquivo config.properties não encontrado. Usando variáveis de ambiente ou padrões.");
	            }
	        } catch (IOException e) {
	            System.err.println("Erro ao ler config.properties do classpath: " + e.getMessage());
	        }
	    }
	}

    // Método auxiliar: prioriza propriedade do arquivo > variável de ambiente > valor padrão
    private static String get(String key, String defaultValue) {
        String propValue = props.getProperty(key);
        if (propValue != null) return propValue;

        String envValue = System.getenv(key);
        if (envValue != null) return envValue;

        return defaultValue;
    }

    // Configurações do SQL Server (prioridade: arquivo .properties > variáveis de ambiente > padrão)
    private static final String HOST = get("DB_HOST", "localhost");
    private static final String PORT = get("DB_PORT", "1433");
    private static final String DB_NAME = get("DB_NAME", "estoque");
    private static final String USER = get("DB_USER", "estoque_app");
    private static final String PASS = get("DB_PASS", "123");

    private static final String URL = String.format(
    	    "jdbc:jtds:sqlserver://%s:%s/%s", HOST, PORT, DB_NAME
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