# 📦 Sistema de Controle de Estoque

Sistema desktop desenvolvido em Java Swing para gerenciamento de estoque, com suporte a produtos unitários e fardos/caixas, leitor de código de barras integrado e persistência em banco de dados **SQL Server**.

---

## ✨ Funcionalidades

- **Cadastro de produtos:** unitários ou fardos/caixas, com código de barras único, **categoria** (bebidas, carnes, laticínios, etc.) e **preço unitário**.
- **Categorias personalizáveis:** gerencie categorias em uma aba dedicada; o combo de categorias é atualizado automaticamente em todas as telas.
- **Listagem de produtos:** exibe código, nome, tipo, unid./fardo e **categoria**; ordenação por nome ou ordem de cadastro.
- **Estado do estoque:** visão dedicada com quantidades atuais, **destaque visual (vermelho) para produtos zerados**, colunas de preço unitário e valor total, e atualização em tempo real.
- **Movimentações de estoque:** entrada (unidades avulsas ou fardos) e saída, com abertura automática de fardos quando necessário. **Cada movimentação registra o preço unitário do produto naquele momento.**
- **Leitor de código de barras:** escaneie um código para abrir automaticamente as opções de entrada/saída; o botão "Entrada (Fardos)" é desabilitado para produtos unitários.
- **Histórico de movimentações:** **filtro único e flexível** por dia, mês ou ano (formato livre: `d-M-yyyy`, `M-yyyy` ou `yyyy`).
- **Relatórios:** gere relatório de movimentações por período ou estado do estoque em uma data específica, com **filtros por nome do produto e categoria**, e opção de **exibir apenas produtos zerados**. **O preço exibido é o histórico (último preço praticado até a data do relatório)**, garantindo valores financeiros corretos em qualquer período. Destaque automático em vermelho para produtos com saldo zero. Impressão e exportação CSV disponíveis.
- **Proteção contra exclusão:** impede remover produtos que ainda possuem unidades em estoque.
- **Transações atômicas:** todas as operações de escrita (adição, remoção, entradas rápidas) são executadas em transações com `commit`/`rollback`, garantindo consistência dos dados.
- **Data/hora do servidor:** todas as movimentações utilizam `GETDATE()` do SQL Server, eliminando dependência do relógio da máquina cliente.
- **Interface gráfica amigável:** organizada em abas (Listar, Estado, Adicionar, Remover, Movimentações, Leitor, Relatórios, Categorias).

---

## 🆕 O que mudou? (Antes e depois)

| Funcionalidade | Antes | Agora |
|----------------|-------|-------|
| **Preço dos produtos** | Preço não era registrado nas movimentações | Cada movimentação armazena o preço unitário praticado no momento |
| **Valor do estoque histórico** | Relatórios usavam apenas o preço atual do cadastro | Relatórios utilizam o último preço registrado até a data do filtro, refletindo o valor real da época |
| **Transações** | Operações de escrita podiam deixar inconsistências em caso de falha | Transações atômicas garantem que estoque e movimentação sejam confirmados ou desfeitos juntos |
| **Data/hora das movimentações** | Usava `LocalDateTime.now()` da máquina cliente | Utiliza `GETDATE()` do SQL Server, centralizado e confiável |
| **Aba Movimentações** | Exigia seleção de Dia/Mês/Ano e formato específico | Campo único com parser flexível (`d-M-yyyy`, `M-yyyy`, `yyyy`) |
| **Tratamento de erros SQL** | Apenas `printStackTrace`, silencioso para o usuário | Erros de banco propagam `RuntimeException` e exibem mensagem na interface |
| **Layout da aba Relatórios** | Filtros competiam por espaço com a tabela | Filtros fixos no topo, tabela expansível no centro |
| **Busca por código de barras** | `ResultSet` não fechado explicitamente | Fechamento garantido com `try` interno |
| **Limpeza do formulário** | Campos de fardo não eram escondidos ao limpar | Campos de fardo são ocultados corretamente |
| **Histórico de movimentações com filtros** | `JOIN` com `produtos` excluía movimentações de produtos deletados | `LEFT JOIN` preserva o histórico |

*(As demais funcionalidades já listadas anteriormente permanecem.)*

---

## 🛠️ Tecnologias Utilizadas

- **Java 11+** (recomendado 17 ou 21)
- **Swing** para interface gráfica
- **Microsoft SQL Server** como banco de dados (driver JDBC incluído)
- **Eclipse** como IDE (projeto compatível com qualquer IDE Java)

---

## 📁 Estrutura do Projeto

```

src/
├── estoque/
│ ├── model/
│ │ ├── Produto.java
│ │ ├── Movimentacao.java
│ │ ├── ProdutoSaldo.java
│ │ └── Categoria.java
│ ├── controller/
│ │ └── EstoqueController.java
│ ├── view/
│ │ ├── EstoqueGUI.java
│ │ └── Main.java
│ └── database/
│ └── DatabaseConnection.java
lib/
└── mssql-jdbc-12.4.2.jre8.jar

```


---

## 🚀 Como Executar

### Pré‑requisitos

- Java 11 ou superior.
- SQL Server instalado e acessível (local ou rede).
- Variáveis de ambiente (ou configuração direta no código) para:
  - `DB_HOST` (padrão: `localhost`)
  - `DB_PORT` (padrão: `1433`)
  - `DB_NAME` (padrão: `estoque`)
  - `DB_USER` (padrão: `estoque_app`)
  - `DB_PASS` (padrão: `123`)

O banco e as tabelas são criados automaticamente na primeira execução.

### Compilando e executando

1. Importe o projeto no Eclipse.
2. Adicione o driver JDBC do SQL Server ao Build Path.
3. Execute a classe `Main` como aplicação Java.

---

## 📝 Últimas Atualizações (detalhadas)

- **Histórico de preços:** o preço unitário agora é armazenado em cada movimentação; os relatórios de estado do estoque exibem o último preço registrado até a data do filtro (ou o preço cadastral, se não houver movimentação anterior).
- **Transações atômicas:** os métodos `adicionarProduto`, `removerQuantidade`, `adicionarQuantidade` e `adicionarFardos` executam todas as operações dentro de uma transação com `commit`/`rollback`, garantindo que nenhum dado fique inconsistente em caso de falha.
- **Data/hora do servidor (`GETDATE()`):** todas as movimentações passaram a usar a função `GETDATE()` do SQL Server, eliminando divergências causadas por relógios incorretos nas máquinas clientes.
- **Aba Movimentações unificada:** removidos os radio buttons Dia/Mês/Ano; agora um único campo de data com parser flexível aceita `d-M-yyyy`, `M-yyyy` ou `yyyy` e filtra as movimentações correspondentes.
- **Melhorias na interface:** filtros da aba Relatórios reposicionados para o topo (fixos), tabela ocupa o centro; altura dos filtros não é mais fixa, adaptando-se a novos componentes futuros.
- **Correções de bugs:** ajustado o método `buscarProdutoPorCodigoBarras` para definir o parâmetro antes de executar a consulta; tratamento adequado de `ResultSet` não fechado.
- **Limpeza do formulário de adição:** campos de "Quantidade de Fardos" agora são ocultados corretamente ao limpar o formulário após adicionar um produto do tipo fardo.

---

## 🤝 Contribuição

Sugestões e melhorias são bem-vindas! Sinta-se à vontade para abrir uma issue ou enviar um pull request.
