# 📦 Sistema de Controle de Estoque

Sistema desktop desenvolvido em Java Swing para gerenciamento de estoque, com suporte a produtos unitários e fardos/caixas, leitor de código de barras integrado e persistência em banco de dados Microsoft Access (via UCanAccess).

## ✨ Funcionalidades

- **Cadastro de produtos**: unitários ou fardos/caixas, com código de barras único.
- **Listagem de produtos**: exibe apenas dados cadastrais (código, nome, tipo, unid./fardo) para consulta rápida, sem poluir com quantidades de estoque.
- **Estado do estoque**: visão dedicada com quantidades atuais, destaque visual (vermelho) para produtos zerados e atualização em tempo real.
- **Movimentações de estoque**: entrada (unidades avulsas ou fardos) e saída, com abertura automática de fardos quando necessário.
- **Leitor de código de barras**: escaneie um código para abrir automaticamente as opções de entrada/saída; o botão "Entrada (Fardos)" é desabilitado para produtos unitários, evitando operações inválidas.
- **Histórico de movimentações**: filtros por dia, mês e ano.
- **Relatórios**: gere relatório de movimentações por período ou estado do estoque em uma data específica, com opções de impressão e exportação CSV.
- **Proteção contra exclusão**: impede remover produtos que ainda possuem unidades em estoque.
- **Interface gráfica amigável**: organizada em abas (Listar, Estado, Adicionar, Remover, Movimentações, Leitor, Relatórios).

## 🛠️ Tecnologias Utilizadas

- Java 11+ (recomendado 17 ou 21)
- Swing para a interface gráfica
- UCanAccess 5.1.5 (driver JDBC puro para Access)
- Microsoft Access (.accdb) como banco de dados
- Eclipse como IDE (projeto compatível com qualquer IDE Java)

## 📁 Estrutura do Projeto
'''
rc/
├── estoque/
│ ├── model/
│ │ ├── Produto.java
│ │ ├── Movimentacao.java
│ │ └── ProdutoSaldo.java
│ ├── controller/
│ │ └── EstoqueController.java
│ ├── view/
│ │ ├── EstoqueGUI.java
│ │ └── Main.java
│ └── database/
│ └── DatabaseConnection.java
lib/
└── ucanaccess-5.1.5-uber.jar

'''

## 🚀 Como Executar

### Pré‑requisitos

1. **Java 11 ou superior** instalado.
2. **Driver UCanAccess** (uber JAR) adicionado ao classpath:
   - Baixe o arquivo `ucanaccess-x.x.x-uber.jar` da [página de releases](https://sourceforge.net/projects/ucanaccess/files/).
   - Coloque-o na pasta `lib` do projeto e adicione ao *Build Path*.
3. **Banco de dados Access (.accdb)**:
   - O caminho é definido pela variável de ambiente `CAMINHO_BANCO_ESTOQUE`.
   - Se não definida, o banco será criado automaticamente como `estoque.accdb` na pasta de execução do JAR.

### Executando o JAR

1. Gere o arquivo JAR executável pelo Eclipse:
   - *Botão direito no projeto → Export → Runnable JAR file → escolha a classe `Main` e a opção "Package required libraries into generated JAR"*.
2. Execute pelo terminal:
   ```bash
   java -jar ControleDeEstoque.jar

   ⚙️ Configurações

    Caminho do banco: variável de ambiente CAMINHO_BANCO_ESTOQUE (ex.: C:\MeuEstoque\estoque.accdb). Caso não exista, o sistema cria o arquivo localmente.

    Permissões de rede: se o arquivo estiver em uma pasta de rede, certifique-se de que o usuário tenha permissão de leitura/escrita.

    Uso simultâneo: o Access não suporta múltiplos acessos concorrentes. Para uso empresarial com vários terminais, recomenda-se migrar para MySQL/PostgreSQL.

📝 Últimas Atualizações
Melhorias recentes

    Aba "Listar Produtos" agora exibe apenas dados cadastrais (sem colunas de estoque).

    Leitor de códigos impede seleção de "Entrada (Fardos)" para produtos unitários.

    Validação reforçada nos campos numéricos da tela de adição de produto, com mensagens de erro específicas.

    Impressão de relatórios restaura automaticamente a formatação da tabela mesmo em caso de cancelamento ou erro.

    Inicialização do banco de dados mais confiável, utilizando DatabaseMetaData para verificar existência das tabelas.

    Nova classe ProdutoSaldo para representar o estado do estoque em relatórios.

Correções

    Corrigido bug no relatório "Estado do Estoque" que causava exceção por falta de parâmetro SQL.

    Validação independente do campo "Unidades Avulsas" na adição de produto.

    Nota: A funcionalidade de "backup automático" foi removida da lista de recursos porque o sistema não a implementa. A proteção existente limita-se a uma verificação de conexão com o banco antes de cada operação, garantindo que nenhum dado seja corrompido por falha de comunicação, mas não constitui um backup.

🤝 Contribuição

Sugestões e melhorias são bem-vindas! Sinta-se à vontade para abrir uma issue ou enviar um pull request.
