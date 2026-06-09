package estoque.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import estoque.controller.EstoqueController;
import estoque.model.Movimentacao;
import estoque.model.Produto;

public class EstoqueGUI extends JFrame {
	
	private DefaultTableModel tableModelEstado;
    private EstoqueController controller;
    private JTabbedPane tabbedPane;
    private JTextField tfCodigoBarrasAdicionar;
    
    private DefaultTableModel tableModelProdutos;
    private JTable tableProdutos;
    private DefaultTableModel tableModelMovimentacoes;
    private JTable tableMovimentacoes;
    private JComboBox<Produto> cbProdutoRemover;
    private JTextField tfQuantidadeRemover;
    private JTextField tfNomeProduto;
    private JRadioButton rbUnitario;
    private JRadioButton rbFardo;
    private JTextField tfUnidadesPorFardo;
    private JTextField tfQuantidadeFardos;
    private JTextField tfQuantidadeUnidades;
    private JLabel lblResultadoAdicionar;
    private JLabel lblResultadoRemover;
    
    
    public EstoqueGUI() {
        controller = new EstoqueController();
        initComponents();
        setTitle("Sistema de Estoque");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Listar Produtos", criarPainelListar());
        tabbedPane.addTab("Estado do Estoque", criarPainelEstado());
        tabbedPane.addTab("Adicionar Produto", criarPainelAdicionar());
        tabbedPane.addTab("Remover Produto", criarPainelRemover());
        tabbedPane.addTab("Movimentações", criarPainelMovimentacoes());
        tabbedPane.addTab("Leitor de Códigos", criarPainelLeitor());
        add(tabbedPane);
    }
    
    // ==================== 1. LISTAR PRODUTOS ====================
    private JPanel criarPainelListar() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] colunas = {"Código de Barras", "Nome", "Tipo", "Unid./Fardo", "Fardos", "Unid. Avulsas", "Total Unidades"};
        tableModelProdutos = new DefaultTableModel(colunas, 0);
        tableProdutos = new JTable(tableModelProdutos);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton btnAtualizar = new JButton("Atualizar");
        JButton btnExcluir = new JButton("Excluir Produto Selecionado");
        btnExcluir.setBackground(new Color(255, 100, 100));
        
        buttonPanel.add(btnAtualizar);
        buttonPanel.add(btnExcluir);
        
        panel.add(new JScrollPane(tableProdutos), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        btnAtualizar.addActionListener(e -> atualizarTabelaProdutos());
        btnExcluir.addActionListener(e -> excluirProdutoSelecionado());
        
        atualizarTabelaProdutos();
        return panel;
    }
    
    private void atualizarTabelaProdutos() {
        tableModelProdutos.setRowCount(0);
        for (Produto p : controller.listarProdutos()) {
            tableModelProdutos.addRow(new Object[]{
                p.getCodigoBarras(),
                p.getNome(),
                p.getTipo(),
                p.getTipo() == Produto.TipoProduto.FARDO ? p.getUnidadesPorFardo() : "-",
                p.getQuantidadeFardos(),
                p.getQuantidadeUnidades(),
                p.getTotalUnidades()
            });
        }
    }
    //EXCLUIR PRODUTO SELECIONADO
    private void excluirProdutoSelecionado() {
        int linhaSelecionada = tableProdutos.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this,
                "Selecione um produto na tabela para excluir.",
                "Nenhum produto selecionado",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String codigoBarras = (String) tableModelProdutos.getValueAt(linhaSelecionada, 0);
        String nomeProduto = (String) tableModelProdutos.getValueAt(linhaSelecionada, 1);

        // Busca o produto para verificar o estoque
        Produto produto = controller.buscarProdutoPorCodigoBarras(codigoBarras);
        if (produto == null) {
            JOptionPane.showMessageDialog(this,
                "Produto não encontrado.",
                "Erro",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 🚫 Impede a exclusão se ainda houver estoque
        if (produto.getTotalUnidades() > 0) {
            JOptionPane.showMessageDialog(this,
                "Não é possível excluir o produto \"" + nomeProduto + "\".\n" +
                "Ainda há " + produto.getTotalUnidades() + " unidade(s) em estoque.\n" +
                "Zere o estoque antes de excluí‑lo.",
                "Exclusão não permitida",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Tem certeza que deseja excluir permanentemente o produto \"" + nomeProduto + "\"?\n" +
            "Essa ação não pode ser desfeita.",
            "Confirmar exclusão",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean sucesso = controller.excluirProdutoPorCodigo(codigoBarras);
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Produto excluído com sucesso!");
                atualizarTabelaProdutos();
                atualizarCombosProdutos();
                atualizarTabelaEstado(); 
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir produto.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
   
 // ==================== 2. ESTADO DO ESTOQUE ====================
 // ==================== 2. ESTADO DO ESTOQUE ====================
    private JPanel criarPainelEstado() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] colunas = {"Produto", "Tipo", "Fardos", "Unidades Avulsas", "Total Unidades"};
        tableModelEstado = new DefaultTableModel(colunas, 0);
        JTable table = new JTable(tableModelEstado);
        
        // Renderizador para destacar produtos com estoque zero
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Object totalObj = table.getModel().getValueAt(row, 4);
                if (totalObj instanceof Integer) {
                    int total = (Integer) totalObj;
                    if (total == 0) {
                        c.setForeground(java.awt.Color.RED);
                        if (!isSelected) {
                            c.setBackground(new java.awt.Color(255, 230, 230));
                        }
                    } else {
                        c.setForeground(java.awt.Color.BLACK);
                        c.setBackground(isSelected ? table.getSelectionBackground() : java.awt.Color.WHITE);
                    }
                }
                return c;
            }
        });
        
        JButton btnAtualizar = new JButton("Atualizar");
        btnAtualizar.addActionListener(e -> atualizarTabelaEstado());
        
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(btnAtualizar, BorderLayout.SOUTH);
        
        atualizarTabelaEstado();
        return panel;
    }
    
    

    private void atualizarTabelaEstado() {
        tableModelEstado.setRowCount(0);
        for (Produto p : controller.listarProdutos()) {
            tableModelEstado.addRow(new Object[]{
                p.getNome(),
                p.getTipo(),
                p.getQuantidadeFardos(),
                p.getQuantidadeUnidades(),
                p.getTotalUnidades()
            });
        }
    }
    // ==================== 3. ADICIONAR PRODUTO ====================
    private JPanel criarPainelAdicionar() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Código de Barras (linha 0)
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Código de Barras:"), gbc);
        gbc.gridx = 1;
        tfCodigoBarrasAdicionar = new JTextField(20);
        panel.add(tfCodigoBarrasAdicionar, gbc);
        
        // Nome (linha 1)
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Nome do Produto:"), gbc);
        gbc.gridx = 1;
        tfNomeProduto = new JTextField(20);
        panel.add(tfNomeProduto, gbc);
        
        // Tipo (linha 2)
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        rbUnitario = new JRadioButton("Unitário");
        rbFardo = new JRadioButton("Fardo/Caixa");
        ButtonGroup bgTipo = new ButtonGroup();
        bgTipo.add(rbUnitario);
        bgTipo.add(rbFardo);
        rbUnitario.setSelected(true);
        JPanel tipoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tipoPanel.add(rbUnitario);
        tipoPanel.add(rbFardo);
        panel.add(tipoPanel, gbc);
        
        // Unidades por Fardo (linha 3)
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Unidades por Fardo:"), gbc);
        gbc.gridx = 1;
        tfUnidadesPorFardo = new JTextField(10);
        tfUnidadesPorFardo.setEnabled(false);
        panel.add(tfUnidadesPorFardo, gbc);
        
        // Quantidade de Fardos (linha 4)
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Quantidade de Fardos:"), gbc);
        gbc.gridx = 1;
        tfQuantidadeFardos = new JTextField(10);
        panel.add(tfQuantidadeFardos, gbc);
        
        // Unidades Avulsas (linha 5)
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Unidades Avulsas:"), gbc);
        gbc.gridx = 1;
        tfQuantidadeUnidades = new JTextField(10);
        panel.add(tfQuantidadeUnidades, gbc);
        
        // Botão Adicionar (linha 6)
        JButton btnAdicionar = new JButton("Adicionar Produto");
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnAdicionar, gbc);
        
        lblResultadoAdicionar = new JLabel(" ");
        gbc.gridy = 7;
        panel.add(lblResultadoAdicionar, gbc);
        
        rbFardo.addActionListener(e -> tfUnidadesPorFardo.setEnabled(true));
        rbUnitario.addActionListener(e -> tfUnidadesPorFardo.setEnabled(false));
        
        btnAdicionar.addActionListener(e -> adicionarProduto());
        
        return panel;
    }
    
    private void adicionarProduto() {
        try {
            String codigo = tfCodigoBarrasAdicionar.getText().trim();
            if (codigo.isEmpty()) throw new Exception("Código de barras é obrigatório.");
            if (controller.existeProdutoComCodigo(codigo)) throw new Exception("Já existe um produto com este código.");
            
            String nome = tfNomeProduto.getText().trim();
            if (nome.isEmpty()) throw new Exception("Nome é obrigatório.");
            
            Produto.TipoProduto tipo = rbFardo.isSelected() ? Produto.TipoProduto.FARDO : Produto.TipoProduto.UNITARIO;
            int unidadesPorFardo = 0;
            if (tipo == Produto.TipoProduto.FARDO) {
                unidadesPorFardo = Integer.parseInt(tfUnidadesPorFardo.getText().trim());
                if (unidadesPorFardo <= 0) throw new Exception("Unidades por fardo deve ser positivo.");
            }
            int qtdFardos = Integer.parseInt(tfQuantidadeFardos.getText().trim());
            int qtdUnidades = Integer.parseInt(tfQuantidadeUnidades.getText().trim());
            if (qtdFardos < 0 || qtdUnidades < 0) throw new Exception("Quantidades não podem ser negativas.");
            
            Produto p = new Produto();
            p.setCodigoBarras(codigo);
            p.setNome(nome);
            p.setTipo(tipo);
            p.setUnidadesPorFardo(unidadesPorFardo);
            p.setQuantidadeFardos(qtdFardos);
            p.setQuantidadeUnidades(qtdUnidades);
            
            controller.adicionarProduto(p);
            lblResultadoAdicionar.setText("Produto adicionado com sucesso!");
            limparCamposAdicionar();
            atualizarTabelaProdutos();
            atualizarCombosProdutos();
            atualizarTabelaEstado();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao adicionar produto:\n" + ex.getMessage(),
                    "Falha na gravação",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void limparCamposAdicionar() {
        tfCodigoBarrasAdicionar.setText("");
        tfNomeProduto.setText("");
        rbUnitario.setSelected(true);
        tfUnidadesPorFardo.setText("");
        tfUnidadesPorFardo.setEnabled(false);
        tfQuantidadeFardos.setText("");
        tfQuantidadeUnidades.setText("");
    }
    
    // ==================== 4. REMOVER PRODUTO ====================
    private JPanel criarPainelRemover() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Selecione o Produto:"), gbc);
        gbc.gridx = 1;
        cbProdutoRemover = new JComboBox<>();
        panel.add(cbProdutoRemover, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Quantidade (em unidades):"), gbc);
        gbc.gridx = 1;
        tfQuantidadeRemover = new JTextField(10);
        panel.add(tfQuantidadeRemover, gbc);
        
        JButton btnRemover = new JButton("Remover");
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnRemover, gbc);
        
        lblResultadoRemover = new JLabel(" ");
        gbc.gridy = 3;
        panel.add(lblResultadoRemover, gbc);
        
        atualizarCombosProdutos();
        btnRemover.addActionListener(e -> removerProduto());
        
        return panel;
    }
    
    private void removerProduto() {
        Produto selecionado = (Produto) cbProdutoRemover.getSelectedItem();
        if (selecionado == null) {
            lblResultadoRemover.setText("Nenhum produto selecionado.");
            return;
        }
        
        // Busca o produto ATUALIZADO pelo código de barras
        Produto p = controller.buscarProdutoPorCodigoBarras(selecionado.getCodigoBarras());
        if (p == null) {
            lblResultadoRemover.setText("Produto não encontrado no estoque.");
            return;
        }
        
        try {
            int qtde = Integer.parseInt(tfQuantidadeRemover.getText().trim());
            if (qtde <= 0) throw new Exception("Quantidade deve ser positiva.");
            
            // Verifica estoque disponível ANTES de tentar remover
            if (p.getTotalUnidades() < qtde) {
                lblResultadoRemover.setText("Estoque insuficiente! Disponível: " + p.getTotalUnidades() + " unidade(s).");
                return;
            }
            
            boolean ok = controller.removerQuantidade(p, qtde);
            if (ok) {
                lblResultadoRemover.setText("Removido com sucesso!");
                tfQuantidadeRemover.setText("");
                atualizarTabelaProdutos();
                atualizarCombosProdutos();
                atualizarTabelaEstado(); 
            } else {
                lblResultadoRemover.setText("Erro ao remover. Verifique o estoque.");
            }
        } catch (NumberFormatException ex) {
            lblResultadoRemover.setText("Quantidade inválida.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao remover produto:\n" + ex.getMessage(),
                    "Falha na gravação",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void atualizarCombosProdutos() {
        cbProdutoRemover.removeAllItems();
        List<Produto> produtos = controller.listarProdutos();
        for (Produto p : produtos) {
            cbProdutoRemover.addItem(p);
        }
    }
    
    // ==================== 5. MOVIMENTAÇÕES ====================
    private JPanel criarPainelMovimentacoes() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Filtros
        JPanel filtroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JRadioButton rbDia = new JRadioButton("Dia");
        JRadioButton rbMes = new JRadioButton("Mês");
        JRadioButton rbAno = new JRadioButton("Ano");
        ButtonGroup group = new ButtonGroup();
        group.add(rbDia); group.add(rbMes); group.add(rbAno);
        rbDia.setSelected(true);
        
        JTextField tfData = new JTextField(12);
        tfData.setToolTipText("Formato: yyyy-MM-dd (dia), yyyy-MM (mês) ou yyyy (ano)");
        JButton btnFiltrar = new JButton("Filtrar");
        
        filtroPanel.add(new JLabel("Filtrar por:"));
        filtroPanel.add(rbDia);
        filtroPanel.add(rbMes);
        filtroPanel.add(rbAno);
        filtroPanel.add(tfData);
        filtroPanel.add(btnFiltrar);
        
        // Tabela de movimentações
        String[] colunas = {"Data/Hora", "Produto", "Tipo", "Quantidade", "Descrição"};
        tableModelMovimentacoes = new DefaultTableModel(colunas, 0);
        tableMovimentacoes = new JTable(tableModelMovimentacoes);
        
        btnFiltrar.addActionListener(e -> {
            String dataStr = tfData.getText().trim();
            if (dataStr.isEmpty()) {
                carregarTodasMovimentacoes();
                return;
            }
            try {
                if (rbDia.isSelected()) {
                    // Tenta parse como dia (yyyy-MM-dd)
                    LocalDate dia = LocalDate.parse(dataStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    filtrarMovimentacoesPorDia(dia);
                } else if (rbMes.isSelected()) {
                    // Tenta parse como mês (yyyy-MM)
                    YearMonth mes = YearMonth.parse(dataStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                    filtrarMovimentacoesPorMes(mes);
                } else {
                    // Tenta parse como ano (yyyy)
                    int ano = Integer.parseInt(dataStr);
                    filtrarMovimentacoesPorAno(ano);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel,
                    "Formato inválido!\n\n" +
                    "Para dia: yyyy-MM-dd (ex: 2026-06-09)\n" +
                    "Para mês: yyyy-MM (ex: 2026-06)\n" +
                    "Para ano: yyyy (ex: 2026)",
                    "Erro de Formato",
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        
        panel.add(filtroPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(tableMovimentacoes), BorderLayout.CENTER);
        
        carregarTodasMovimentacoes();
        return panel;
    }
    
    private void carregarTodasMovimentacoes() {
        tableModelMovimentacoes.setRowCount(0);
        for (Movimentacao m : controller.getMovimentacoes()) {
            adicionarMovimentacaoNaTabela(m);
        }
    }
    
    private void filtrarMovimentacoesPorDia(LocalDate dia) {
        tableModelMovimentacoes.setRowCount(0);
        for (Movimentacao m : controller.getMovimentacoes()) {
            if (m.getDataHora().toLocalDate().equals(dia)) {
                adicionarMovimentacaoNaTabela(m);
            }
        }
    }
    
    private void filtrarMovimentacoesPorMes(YearMonth mes) {
        tableModelMovimentacoes.setRowCount(0);
        for (Movimentacao m : controller.getMovimentacoes()) {
            YearMonth dataMes = YearMonth.from(m.getDataHora());
            if (dataMes.equals(mes)) {
                adicionarMovimentacaoNaTabela(m);
            }
        }
    }
    
    private void filtrarMovimentacoesPorAno(int ano) {
        tableModelMovimentacoes.setRowCount(0);
        for (Movimentacao m : controller.getMovimentacoes()) {
            if (m.getDataHora().getYear() == ano) {
                adicionarMovimentacaoNaTabela(m);
            }
        }
    }
    
    private void adicionarMovimentacaoNaTabela(Movimentacao m) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        tableModelMovimentacoes.addRow(new Object[]{
            m.getDataHora().format(fmt),
            m.getProdutoNome(),
            m.getTipo(),
            m.getQuantidadeUnidades(),
            m.getDescricao()
        });
    }
    
    // ==================== 6. LEITOR DE CÓDIGOS ====================
    private JPanel criarPainelLeitor() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Escaneie o código de barras:"), gbc);
        gbc.gridx = 1;
        JTextField tfCodigo = new JTextField(20);
        panel.add(tfCodigo, gbc);

        JLabel lblProdutoInfo = new JLabel("Produto: ---");
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(lblProdutoInfo, gbc);

        // Painel de operação com as três opções
        JPanel operacaoPanel = new JPanel();
        JRadioButton rbEntradaFardo = new JRadioButton("Entrada (Fardos)");
        JRadioButton rbEntradaUnidade = new JRadioButton("Entrada (Unidades)");
        JRadioButton rbSaida = new JRadioButton("Saída (- estoque)");
        ButtonGroup bgOperacao = new ButtonGroup();
        bgOperacao.add(rbEntradaFardo);
        bgOperacao.add(rbEntradaUnidade);
        bgOperacao.add(rbSaida);
        rbEntradaUnidade.setSelected(true); // padrão: entrada de unidades
        operacaoPanel.add(rbEntradaFardo);
        operacaoPanel.add(rbEntradaUnidade);
        operacaoPanel.add(rbSaida);
        operacaoPanel.setVisible(false);

        gbc.gridy = 2;
        panel.add(operacaoPanel, gbc);

        JPanel quantidadePanel = new JPanel();
        quantidadePanel.add(new JLabel("Quantidade:"));
        JTextField tfQuantidade = new JTextField(8);
        quantidadePanel.add(tfQuantidade);
        JButton btnConfirmar = new JButton("Confirmar");
        quantidadePanel.add(btnConfirmar);
        quantidadePanel.setVisible(false);

        gbc.gridy = 3;
        panel.add(quantidadePanel, gbc);

        JLabel lblMensagem = new JLabel(" ");
        gbc.gridy = 4;
        panel.add(lblMensagem, gbc);

        final Produto[] produtoAtual = {null};

        tfCodigo.addActionListener(e -> {
            String codigo = tfCodigo.getText().trim();
            if (codigo.isEmpty()) return;

            Produto p = controller.buscarProdutoPorCodigoBarras(codigo);
            if (p == null) {
                int resposta = JOptionPane.showConfirmDialog(panel,
                    "Código " + codigo + " não cadastrado.\nDeseja cadastrar um novo produto com este código?",
                    "Produto não encontrado",
                    JOptionPane.YES_NO_OPTION);
                if (resposta == JOptionPane.YES_OPTION) {
                    tabbedPane.setSelectedIndex(2); // índice da aba Adicionar Produto
                    tfCodigoBarrasAdicionar.setText(codigo);
                    tfNomeProduto.setText("");
                    tfNomeProduto.requestFocus();
                }
                tfCodigo.setText("");
                return;
            }

            lblProdutoInfo.setText("Produto: " + p.getNome() + " (Estoque: " + p.getTotalUnidades() + ")");
            lblMensagem.setText(" ");
            operacaoPanel.setVisible(true);
            quantidadePanel.setVisible(true);
            produtoAtual[0] = p;
            tfQuantidade.requestFocus();
            tfCodigo.setText("");
        });

        btnConfirmar.addActionListener(e -> {
            if (produtoAtual[0] == null) {
                lblMensagem.setText("Nenhum produto escaneado.");
                return;
            }
            try {
                int qtd = Integer.parseInt(tfQuantidade.getText().trim());
                if (qtd <= 0) throw new Exception("Quantidade deve ser positiva.");

                if (rbEntradaFardo.isSelected()) {
                    // Entrada de fardos
                    controller.adicionarFardos(produtoAtual[0], qtd);
                    lblMensagem.setText("Entrada de " + qtd + " fardo(s) registrada! Novo total: " + produtoAtual[0].getTotalUnidades() + " unidade(s)");
                } else if (rbEntradaUnidade.isSelected()) {
                    // Entrada de unidades avulsas
                    controller.adicionarQuantidade(produtoAtual[0], qtd);
                    lblMensagem.setText("Entrada de " + qtd + " unidade(s) registrada! Novo total: " + produtoAtual[0].getTotalUnidades() + " unidade(s)");
                } else {
                    // Saída
                    boolean ok = controller.removerQuantidade(produtoAtual[0], qtd);
                    if (ok) {
                        lblMensagem.setText("Saída registrada! Estoque atual: " + produtoAtual[0].getTotalUnidades() + " unidade(s)");
                    } else {
                        lblMensagem.setText("Estoque insuficiente.");
                    }
                }

                atualizarTabelaProdutos();
                atualizarTabelaEstado();
                atualizarCombosProdutos();
                operacaoPanel.setVisible(false);
                quantidadePanel.setVisible(false);
                lblProdutoInfo.setText("Produto: ---");
                produtoAtual[0] = null;
                tfQuantidade.setText("");
                tfCodigo.requestFocus();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel,
                        "Quantidade inválida.",
                        "Erro",
                        JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel,
                        "Erro na operação:\n" + ex.getMessage(),
                        "Falha",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }
}