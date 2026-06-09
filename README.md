# 📦 Sistema de Controle de Estoque

Sistema desktop desenvolvido em **Java Swing** para gerenciamento de estoque, com suporte a produtos unitários e fardos/caixas, leitor de código de barras integrado e persistência em banco de dados **Microsoft Access** (via UCanAccess).

---

## ✨ Funcionalidades

- **Cadastro de produtos**: unitários ou fardos, com código de barras único.
- **Movimentações de estoque**: entrada (unidades ou fardos) e saída (com abertura automática de fardos).
- **Leitor de código de barras**: escaneie um código para dar entrada ou saída rapidamente.
- **Histórico de movimentações** com filtros por dia, mês e ano.
- **Visualização do estado do estoque**: destaque visual (vermelho) para produtos sem estoque.
- **Impede exclusão** de produtos que ainda possuem unidades.
- **Backup automático** e proteção contra perda de conexão com o banco de dados.
- **Interface gráfica amigável** organizada em abas.

---

## 🛠️ Tecnologias Utilizadas

- **Java 11+** (recomendado 17 ou 21)
- **Swing** para a interface gráfica
- **UCanAccess 5.1.5** (driver JDBC puro para Access)
- **Microsoft Access** (.accdb) como banco de dados
- **Eclipse** como IDE (projeto compatível com qualquer IDE Java)

---

## 📁 Estrutura do Projeto

src/
├── estoque/
│ ├── model/
│ │ ├── Produto.java
│ │ └── Movimentacao.java
│ ├── controller/
│ │ └── EstoqueController.java
│ ├── view/
│ │ ├── EstoqueGUI.java
│ │ └── Main.java
│ └── database/
│ └── DatabaseConnection.java
lib/
└── ucanaccess-5.1.5-uber.jar



---

## 🚀 Como Executar

### Pré‑requisitos

- Ter o **Java 11 ou superior** instalado.
- Baixar o driver UCanAccess (uber JAR) e adicioná‑lo ao classpath.
   - Acesse a [página de releases](https://github.com/spannm/ucanaccess/releases) e baixe o arquivo `ucanaccess-x.x.x-uber.jar`.
   - Coloque‑o na pasta `lib` do projeto e adicione ao Build Path.
- Definir a variável de ambiente `CAMINHO_BANCO_ESTOQUE` com o caminho completo do arquivo `.accdb`.
   - Exemplo: `C:\MeuEstoque\estoque.accdb` ou `\\servidor\pasta\estoque.accdb`.
   - Se não definida, o banco será criado localmente na pasta do JAR.

### Executando o JAR

1. Gere o arquivo JAR executável pelo Eclipse:
   - Botão direito no projeto → Export → Runnable JAR file → escolha a classe `Main` e a opção de empacotar bibliotecas.
2. Execute pelo terminal:
   ```bash
   java -jar ControleDeEstoque.jar
´´´

   ⚙️ Configurações

  Caminho do banco: o programa lê a variável de ambiente CAMINHO_BANCO_ESTOQUE; caso não exista, cria estoque.accdb na pasta do JAR.
    
  Permissões de rede: a pasta onde o .accdb está deve ter permissão de leitura/escrita para o usuário.

  Uso simultâneo: o Access não suporta múltiplos acessos concorrentes; para uso empresarial com vários usuários, recomenda‑se migrar para MySQL/PostgreSQL.

    
  🤝 Contribuição

  Sugestões e melhorias são bem‑vindas! Sinta‑se à vontade para abrir uma issue ou enviar um pull request.


