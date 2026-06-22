# 📦 Sistema de Controle de Estoque

Sistema desktop desenvolvido em Java Swing para gerenciamento de estoque, com suporte a produtos unitários e fardos/caixas, leitor de código de barras integrado, gráficos interativos e persistência em banco de dados **SQL Server**.

---

## ✨ Funcionalidades

- **Cadastro de produtos:** unitários ou fardos/caixas, com código de barras único, **categoria** (bebidas, carnes, laticínios, etc.) e **preço unitário**.
- **Categorias personalizáveis:** gerencie categorias em uma aba dedicada; o combo de categorias é atualizado automaticamente em todas as telas.
- **Listagem de produtos:** exibe código, nome, tipo, unid./fardo e **categoria**; ordenação por nome ou ordem de cadastro.
- **Estado do estoque:** visão dedicada com quantidades atuais, **destaque visual (vermelho) para produtos zerados**, colunas de preço unitário e valor total, e atualização em tempo real.
- **Movimentações de estoque:** entrada (unidades avulsas ou fardos) e saída, com abertura automática de fardos quando necessário. **Cada movimentação registra o preço unitário do produto naquele momento.**
- **Leitor de código de barras:** escaneie um código para abrir automaticamente as opções de entrada/saída; o botão "Entrada (Fardos)" é desabilitado para produtos unitários.
- **Histórico de movimentações:** filtro único e flexível por dia, mês ou ano (formato livre: `d‑M‑yyyy`, `M‑yyyy` ou `yyyy`).
- **Relatórios:** gere relatório de movimentações por período ou estado do estoque em uma data específica, com **filtros por nome do produto e categoria**, e opção de **exibir apenas produtos zerados**. **O preço exibido é o histórico (último preço praticado até a data do relatório)**, garantindo valores financeiros corretos em qualquer período. Destaque automático em vermelho para produtos com saldo zero. Impressão e exportação CSV disponíveis.
- **Gráficos interativos (JFreeChart):**
  - **Gráfico Preços (produto único):** selecione um produto na tabela de relatórios e veja a evolução do preço unitário no período.
  - **Gráfico por Categoria – Preço Unitário:** exibe uma linha colorida para cada produto da categoria, mostrando a variação do preço unitário ao longo do tempo.
  - **Gráfico por Categoria – Valor Gasto:** exibe uma única linha com o total gasto (entradas) por mês na categoria selecionada. Meses sem gasto são preenchidos com zero, facilitando a comparação mensal.
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
| **Configuração do banco** | Variáveis de ambiente ou código fonte | Arquivo `config.properties` externo (ao lado do JAR) com prioridade máxima |
| **Gráficos** | Não existiam | Três modos de visualização com JFreeChart (preço unitário por produto, por categoria, e gasto mensal da categoria) |
| **Consulta SQL de gasto mensal** | Colunas ambíguas geravam erro "Ambiguous column name" | Colunas qualificadas com alias da tabela (`m.quantidade`, `m.preco_unitario`) |
| **Preenchimento de meses vazios** | Gráfico de valor gasto só mostrava meses com dados | Todos os meses do intervalo são exibidos (com zero quando não há gasto) |

---

## 🛠️ Tecnologias Utilizadas

- **Java 11+** (recomendado 17 ou 21)
- **Swing** para interface gráfica
- **JFreeChart** para gráficos
- **Microsoft SQL Server** como banco de dados (driver jTDS)
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
│ │ ├── ProdutoMovimento.java
│ │ └── Categoria.java
│ ├── controller/
│ │ └── EstoqueController.java
│ ├── view/
│ │ ├── EstoqueGUI.java
│ │ ├── GraficoPrecoView.java
│ │ └── Main.java
│ └── database/
│ └── DatabaseConnection.java
lib/
├── jtds-1.3.1.jar
└── jfreechart-1.5.4.jar (e dependências)

```


---

## 🚀 Como Executar

### Pré‑requisitos

- Java 11 ou superior.
- SQL Server instalado e acessível (local ou rede).
- Arquivo `config.properties` na mesma pasta do JAR (veja abaixo).

### Configuração do banco de dados

Crie um arquivo `config.properties` ao lado do JAR executável com o seguinte conteúdo:

```properties
DB_HOST=192.168.100.251
DB_PORT=1434
DB_NAME=estoque
DB_USER=estoque_app
DB_PASS=ExemploDeSenha11
