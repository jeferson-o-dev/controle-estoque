package estoque.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
	private static final String CAMINHO_BANCO = System.getenv().getOrDefault("CAMINHO_BANCO_ESTOQUE", "estoque.accdb");

	public static Connection getConnection() throws SQLException {
	    File arquivo = new File(CAMINHO_BANCO);
	    System.out.println("Conectando ao banco em: " + arquivo.getAbsolutePath());
	    String url = "jdbc:ucanaccess://" + arquivo.getAbsolutePath()
	                 + ";newDatabaseVersion=V2010"
	                 + ";singleConnection=true"
	                 + ";keepMirror=memory"
	                 + ";memory=true";
	    return DriverManager.getConnection(url);
	}
//CRIAR TABELA
	public static void criarTabelas() {
	    String sqlProdutos = """
	        CREATE TABLE produtos (
	            id COUNTER PRIMARY KEY,
	            codigo_barras TEXT NOT NULL UNIQUE,
	            nome TEXT NOT NULL,
	            tipo TEXT NOT NULL,
	            unidades_por_fardo INTEGER,
	            quantidade_fardos INTEGER,
	            quantidade_unidades INTEGER,
	            quantidade_fardos_inicial INTEGER,
	            quantidade_unidades_inicial INTEGER
	        )
	    """;

	    String sqlMovimentacoes = """
	        CREATE TABLE movimentacoes (
	            id COUNTER PRIMARY KEY,
	            data_hora DATETIME NOT NULL,
	            produto_id INTEGER NOT NULL,
	            produto_nome TEXT NOT NULL,
	            tipo TEXT NOT NULL,
	            quantidade INTEGER NOT NULL,
	            descricao TEXT
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

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
    //VERIFICAR TABELA
    private static boolean existeTabela(Connection conn, String nomeTabela) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, nomeTabela, null)) {
            return rs.next(); // se houver resultado, a tabela existe
        }
    }
    
    //VERIFICAR CONEXAO
    public static boolean isConnectionAvailable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM produtos")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public static void verificarColunas() {
        String[] colunas = {"quantidade_fardos_inicial", "quantidade_unidades_inicial"};
        for (String coluna : colunas) {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE produtos ADD COLUMN " + coluna + " INTEGER");
                System.out.println("Coluna '" + coluna + "' adicionada.");
            } catch (SQLException e) {
                if (e.getMessage().toLowerCase().contains("duplicate column") ||
                    e.getMessage().toLowerCase().contains("already exists")) {
                    System.out.println("Coluna '" + coluna + "' já existe.");
                } else {
                    e.printStackTrace();
                }
            }
        }
    }
    
}