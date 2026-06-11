# 📦 Sistema de Controle de Estoque

Sistema desktop desenvolvido em Java Swing para gerenciamento de estoque, com suporte a produtos unitários e fardos/caixas, leitor de código de barras integrado e persistência em banco de dados **SQL Server**.

---

## ✨ Funcionalidades

- **Cadastro de produtos:** unitários ou fardos/caixas, com código de barras único e **categoria** (bebidas, carnes, laticínios, etc.).
- **Categorias personalizáveis:** gerencie categorias em uma aba dedicada; o combo de categorias é atualizado automaticamente em todas as telas.
- **Listagem de produtos:** exibe código, nome, tipo, unid./fardo e **categoria**; ordenação por nome ou ordem de cadastro.
- **Estado do estoque:** visão dedicada com quantidades atuais, destaque visual (vermelho) para produtos zerados e atualização em tempo real.
- **Movimentações de estoque:** entrada (unidades avulsas ou fardos) e saída, com abertura automática de fardos quando necessário.
- **Leitor de código de barras:** escaneie um código para abrir automaticamente as opções de entrada/saída; o botão "Entrada (Fardos)" é desabilitado para produtos unitários.
- **Histórico de movimentações:** filtros por dia, mês e ano.
- **Relatórios:** gere relatório de movimentações por período ou estado do estoque em uma data específica, com **filtros por nome do produto e categoria**, e opção de **exibir apenas produtos zerados**. Destaque automático em vermelho para produtos com saldo zero. Impressão e exportação CSV disponíveis.
- **Proteção contra exclusão:** impede remover produtos que ainda possuem unidades em estoque.
- **Interface gráfica amigável:** organizada em abas (Listar, Estado, Adicionar, Remover, Movimentações, Leitor, Relatórios, Categorias).

---

## 🆕 O que mudou? (Antes e depois)

| Funcionalidade | Antes | Agora |
|----------------|-------|-------|
| **Categorias** | Produtos não possuíam categorias | Campo "Categoria" na adição de produto, gerenciamento em aba própria, exibição na listagem e filtro nos relatórios |
| **Relatórios – Estado do Estoque** | Mostrava apenas produtos com movimentações | Mostra todos os produtos, com destaque vermelho para zerados e opção de filtrar apenas os zerados (checkbox "Apenas zerados") |
| **Relatórios – Filtros** | Apenas data | Filtros adicionais: nome do produto (case‑insensitive) e categoria |
| **Datas** | Aceitava apenas `dd-MM-yyyy`, `MM-yyyy`, `yyyy` | Aceita também `d-M-yyyy` e `M-yyyy` (1 ou 2 dígitos para dia/mês) |
| **Tratamento de erros SQL** | Apenas `printStackTrace`, silencioso para o usuário | Mensagens descritivas no console (`System.err`) indicando qual método falhou |
| **Checkbox "Apenas zerados"** | Inexistente | Aparece apenas no relatório de Estado do Estoque, oculto no de Movimentações |
| **Botão Imprimir** | Podia lançar `ArrayIndexOutOfBoundsException` se a tabela estivesse vazia | Verificação prévia evita o erro |
| **Layout da Adição de produto** | Label de resultado sobreposto ao botão | Ajustado para não sobrepor |
| **Relatório de Movimentações (com filtros)** | `JOIN` com `produtos` excluía movimentações de produtos deletados | `LEFT JOIN` preserva histórico |
| **Pesquisa por nome** | Sensível a maiúsculas/minúsculas | Case‑insensitive (`LOWER`) |
| **Persistência** | Microsoft Access (UCanAccess) | **SQL Server** (Microsoft SQL Server) |

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

- **Categorias de produtos:** nova classe `Categoria`, tabela `categorias`, aba de gerenciamento e integração com adição, listagem e relatórios.
- **Relatório de Estado do Estoque redesenhado:** agora mostra todos os produtos (com ou sem movimentações), com renderizador que pinta de vermelho os zerados e checkbox "Apenas zerados" para filtrar.
- **Filtros inteligentes nos relatórios:** busca por nome case‑insensitive e por categoria (combo).
- **Datas flexíveis aprimoradas:** parse aceita dia e mês com um ou dois dígitos (`1-6-2026` ou `01-06-2026`).
- **Tratamento de erros SQL:** todos os métodos do controller agora registram no console (`System.err`) o nome do método e a mensagem de erro, facilitando a depuração.
- **Melhorias na GUI:** layout corrigido na adição de produto, checkbox "Apenas zerados" oculto ao selecionar "Movimentações", proteção no botão Imprimir contra tabela vazia.
- **Migração do banco de dados:** de Microsoft Access (UCanAccess) para **Microsoft SQL Server**, oferecendo maior robustez e suporte a múltiplos acessos (dependendo da edição).

---

## 🤝 Contribuição

Sugestões e melhorias são bem-vindas! Sinta-se à vontade para abrir uma issue ou enviar um pull request.
