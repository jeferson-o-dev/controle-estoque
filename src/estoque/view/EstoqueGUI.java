package estoque.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
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
import estoque.model.Categoria;
import estoque.model.Movimentacao;
import estoque.model.Produto;
import estoque.model.ProdutoMovimento;
import estoque.model.ProdutoSaldo;

public class EstoqueGUI extends JFrame {
	
	private JComboBox<Categoria> cbFiltroCategoria;
	private JComboBox<Categoria> cbCategoriaAdicionar;
	private JComboBox<String> cbOrdenacaoListar;
	private JLabel lblQuantidadeFardos;
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
	private JTextField tfPrecoUnitario;

	private static final java.util.Locale LOCALE_BR = new java.util.Locale("pt", "BR");
	
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
		tabbedPane.addTab("Relatórios", criarPainelRelatorios());
		tabbedPane.addTab("Categorias", criarPainelCategorias());
		add(tabbedPane);
	}

	// ==================== 1. LISTAR PRODUTOS ====================
	private JPanel criarPainelListar() {
		JPanel panel = new JPanel(new BorderLayout());

		// Painel de ordenação (topo)
		JPanel panelOrdenacao = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelOrdenacao.add(new JLabel("Ordenar por:"));
		cbOrdenacaoListar = new JComboBox<>(new String[]{"Ordem de Cadastro", "Ordem Alfabética (Nome)"});
		panelOrdenacao.add(cbOrdenacaoListar);

		// Tabela
		String[] colunas = {"Código de Barras", "Nome", "Tipo", "Unid./Fardo", "Categoria"};
		tableModelProdutos = new DefaultTableModel(colunas, 0);
		tableProdutos = new JTable(tableModelProdutos);
		tableProdutos.setDefaultEditor(Object.class, null);

		// Painel de botões (rodapé)
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton btnAtualizar = new JButton("Atualizar");
		JButton btnExcluir = new JButton("Excluir Produto Selecionado");
		btnExcluir.setBackground(new Color(255, 100, 100));
		buttonPanel.add(btnAtualizar);
		buttonPanel.add(btnExcluir);

		panel.add(panelOrdenacao, BorderLayout.NORTH);
		panel.add(new JScrollPane(tableProdutos), BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);

		btnAtualizar.addActionListener(e -> atualizarTabelaProdutos());
		btnExcluir.addActionListener(e -> excluirProdutoSelecionado());
		cbOrdenacaoListar.addActionListener(e -> atualizarTabelaProdutos());

		atualizarTabelaProdutos();
		return panel;
	}

	private void atualizarTabelaProdutos() {
		List<Produto> lista = controller.listarProdutos();

		if (cbOrdenacaoListar != null && cbOrdenacaoListar.getSelectedItem() != null) {
			String selecionado = (String) cbOrdenacaoListar.getSelectedItem();
			if (selecionado.equals("Ordem Alfabética (Nome)")) {
				lista.sort(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER));
			}
		}

		tableModelProdutos.setRowCount(0);
		for (Produto p : lista) {
			tableModelProdutos.addRow(new Object[]{
					p.getCodigoBarras(),
					p.getNome(),
					p.getTipo(),
					p.getTipo() == Produto.TipoProduto.FARDO ? p.getUnidadesPorFardo() : "-",
					p.getCategoriaNome() != null ? p.getCategoriaNome() : ""
			});
		}
	}

	// EXCLUIR PRODUTO SELECIONADO
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

		Produto produto = controller.buscarProdutoPorCodigoBarras(codigoBarras);
		if (produto == null) {
			JOptionPane.showMessageDialog(this,
					"Produto não encontrado.",
					"Erro",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

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
	private JPanel criarPainelEstado() {
		JPanel panel = new JPanel(new BorderLayout());
		String[] colunas = {"Produto", "Tipo", "Fardos", "Unid. Avulsas", "Total Unid.", "Preço Unit.", "Valor Total"};
		tableModelEstado = new DefaultTableModel(colunas, 0);
		JTable table = new JTable(tableModelEstado);
		table.setDefaultEditor(Object.class, null);

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

	// ATUALIZAR TABELA ESTADO
	private void atualizarTabelaEstado() {
		List<Produto> lista = controller.listarProdutos();
		lista.sort(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER));

		tableModelEstado.setRowCount(0);
		NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_BR);
		for (Produto p : lista) {
			BigDecimal preco = p.getPrecoUnitario();
			String precoStr = "-";
			String valorStr = "-";
			if (preco != null) {
				BigDecimal valorTotal = preco.multiply(BigDecimal.valueOf(p.getTotalUnidades()));
				precoStr = nf.format(preco);
				valorStr = nf.format(valorTotal);
			}
			tableModelEstado.addRow(new Object[]{
					p.getNome(),
					p.getTipo(),
					p.getQuantidadeFardos(),
					p.getQuantidadeUnidades(),
					p.getTotalUnidades(),
					precoStr,
					valorStr
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

		// Categoria (linha 2)
		gbc.gridx = 0; gbc.gridy = 2;
		panel.add(new JLabel("Categoria:"), gbc);
		gbc.gridx = 1;
		cbCategoriaAdicionar = new JComboBox<>();
		cbCategoriaAdicionar.addItem(new Categoria(0, "Nenhuma"));
		List<Categoria> categorias = controller.listarCategorias();
		for (Categoria c : categorias) {
			cbCategoriaAdicionar.addItem(c);
		}
		panel.add(cbCategoriaAdicionar, gbc);

		// Tipo (linha 3)
		gbc.gridx = 0; gbc.gridy = 3;
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

		// Unidades por Fardo (linha 4)
		gbc.gridx = 0; gbc.gridy = 4;
		panel.add(new JLabel("Unidades por Fardo:"), gbc);
		gbc.gridx = 1;
		tfUnidadesPorFardo = new JTextField(10);
		tfUnidadesPorFardo.setEnabled(false);
		panel.add(tfUnidadesPorFardo, gbc);

		// Quantidade de Fardos (linha 5)
		gbc.gridx = 0; gbc.gridy = 5;
		lblQuantidadeFardos = new JLabel("Quantidade de Fardos:");
		panel.add(lblQuantidadeFardos, gbc);
		gbc.gridx = 1;
		tfQuantidadeFardos = new JTextField(10);
		panel.add(tfQuantidadeFardos, gbc);

		// Unidades Avulsas (linha 6)
		gbc.gridx = 0; gbc.gridy = 6;
		panel.add(new JLabel("Unidades Avulsas:"), gbc);
		gbc.gridx = 1;
		tfQuantidadeUnidades = new JTextField(10);
		panel.add(tfQuantidadeUnidades, gbc);

		// Preço Unitário (linha 7)
		gbc.gridx = 0; gbc.gridy = 7;
		panel.add(new JLabel("Preço Unitário:"), gbc);
		gbc.gridx = 1;
		tfPrecoUnitario = new JTextField(10);   // 🆕 agora usa o campo da classe
		panel.add(tfPrecoUnitario, gbc);

		// Botão Adicionar (linha 8)
		JButton btnAdicionar = new JButton("Adicionar Produto");
		gbc.gridx = 0; gbc.gridy = 8;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		panel.add(btnAdicionar, gbc);

		lblResultadoAdicionar = new JLabel(" ");
		gbc.gridx = 0; gbc.gridy = 9;
		gbc.gridwidth = 2;
		panel.add(lblResultadoAdicionar, gbc);

		rbFardo.addActionListener(e -> {
			tfUnidadesPorFardo.setEnabled(true);
			lblQuantidadeFardos.setVisible(true);
			tfQuantidadeFardos.setVisible(true);
		});

		rbUnitario.addActionListener(e -> {
			tfUnidadesPorFardo.setEnabled(false);
			lblQuantidadeFardos.setVisible(false);
			tfQuantidadeFardos.setVisible(false);
		});

		btnAdicionar.addActionListener(e -> adicionarProduto());
		lblQuantidadeFardos.setVisible(false);
		tfQuantidadeFardos.setVisible(false);
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
			int qtdFardos = 0;

			if (tipo == Produto.TipoProduto.FARDO) {
				try {
					unidadesPorFardo = Integer.parseInt(tfUnidadesPorFardo.getText().trim());
				} catch (NumberFormatException e) {
					throw new Exception("Unidades por fardo deve ser um número inteiro.");
				}
				if (unidadesPorFardo <= 0) throw new Exception("Unidades por fardo deve ser positivo.");

				try {
					qtdFardos = Integer.parseInt(tfQuantidadeFardos.getText().trim());
				} catch (NumberFormatException e) {
					throw new Exception("Quantidade de fardos deve ser um número inteiro.");
				}
				if (qtdFardos < 0) throw new Exception("Quantidade de fardos não pode ser negativa.");
			}
			int qtdUnidades;
			try {
				qtdUnidades = Integer.parseInt(tfQuantidadeUnidades.getText().trim());
			} catch (NumberFormatException e) {
				throw new Exception("Quantidade de unidades avulsas deve ser um número inteiro.");
			}
			if (qtdFardos < 0 || qtdUnidades < 0) throw new Exception("Quantidades não podem ser negativas.");

			Produto p = new Produto();
			p.setCodigoBarras(codigo);
			p.setNome(nome);
			p.setTipo(tipo);
			p.setUnidadesPorFardo(unidadesPorFardo);
			p.setQuantidadeFardos(qtdFardos);
			p.setQuantidadeUnidades(qtdUnidades);

			Categoria selCat = (Categoria) cbCategoriaAdicionar.getSelectedItem();
			if (selCat != null && selCat.getId() != 0) {
				p.setCategoriaId(selCat.getId());
			} else {
				p.setCategoriaId(null);
			}

			// 🆕 Lê o preço unitário
			BigDecimal preco = null;
			if (!tfPrecoUnitario.getText().trim().isEmpty()) {
				try {
					preco = new BigDecimal(tfPrecoUnitario.getText().trim().replace(",", "."));
				} catch (NumberFormatException e) {
					throw new Exception("Preço unitário inválido. Use ponto ou vírgula como separador decimal.");
				}
			}
			p.setPrecoUnitario(preco);

			controller.adicionarProduto(p);
			lblResultadoAdicionar.setForeground(new Color(0, 128, 0));
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
		lblQuantidadeFardos.setVisible(false);   // ← ADICIONAR
	    tfQuantidadeFardos.setVisible(false);    // ← ADICIONAR
		tfQuantidadeUnidades.setText("");
		tfPrecoUnitario.setText("");   // 🆕 limpa também o campo de preço
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

		Produto p = controller.buscarProdutoPorCodigoBarras(selecionado.getCodigoBarras());
		if (p == null) {
			lblResultadoRemover.setText("Produto não encontrado no estoque.");
			return;
		}

		try {
			int qtde = Integer.parseInt(tfQuantidadeRemover.getText().trim());
			if (qtde <= 0) throw new Exception("Quantidade deve ser positiva.");

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
		produtos.sort(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER));
		for (Produto p : produtos) {
			cbProdutoRemover.addItem(p);
		}
	}

	// ==================== 5. MOVIMENTAÇÕES ====================
	private JPanel criarPainelMovimentacoes() {
		JPanel panel = new JPanel(new BorderLayout());
		JPanel filtroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JRadioButton rbDia = new JRadioButton("Dia");
		JRadioButton rbMes = new JRadioButton("Mês");
		JRadioButton rbAno = new JRadioButton("Ano");
		ButtonGroup group = new ButtonGroup();
		group.add(rbDia); group.add(rbMes); group.add(rbAno);
		rbDia.setSelected(true);

		JTextField tfData = new JTextField(12);
		tfData.setToolTipText("dd-MM-yyyy (dia), MM-yyyy (mês) ou yyyy (ano)");
		JButton btnFiltrar = new JButton("Filtrar");

		filtroPanel.add(new JLabel("Filtrar por:"));
		filtroPanel.add(rbDia);
		filtroPanel.add(rbMes);
		filtroPanel.add(rbAno);
		filtroPanel.add(tfData);
		filtroPanel.add(btnFiltrar);

		String[] colunas = {"Data/Hora", "Produto", "Tipo", "Quantidade", "Descrição"};
		tableModelMovimentacoes = new DefaultTableModel(colunas, 0);
		tableMovimentacoes = new JTable(tableModelMovimentacoes);
		tableMovimentacoes.setDefaultEditor(Object.class, null);

		btnFiltrar.addActionListener(e -> {
			String dataStr = tfData.getText().trim();
			if (dataStr.isEmpty()) {
				carregarTodasMovimentacoes();
				return;
			}
			try {
				if (rbDia.isSelected()) {
					LocalDate dia = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
					filtrarMovimentacoesPorDia(dia);
				} else if (rbMes.isSelected()) {
					YearMonth mes = YearMonth.parse(dataStr, DateTimeFormatter.ofPattern("MM-yyyy"));
					filtrarMovimentacoesPorMes(mes);
				} else {
					if (!dataStr.matches("\\d{4}")) {
						throw new IllegalArgumentException("Ano inválido. Use 4 dígitos.");
					}
					int ano = Integer.parseInt(dataStr);
					filtrarMovimentacoesPorAno(ano);
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(panel,
						"Formato inválido!\n\n" +
						"Para dia: dd-MM-yyyy (ex: 11-06-2026)\n" +
						"Para mês: MM-yyyy (ex: 06-2026)\n" +
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

		JPanel operacaoPanel = new JPanel();
		JRadioButton rbEntradaFardo = new JRadioButton("Entrada (Fardos)");
		JRadioButton rbEntradaUnidade = new JRadioButton("Entrada (Unidades)");
		JRadioButton rbSaida = new JRadioButton("Saída (- estoque)");
		ButtonGroup bgOperacao = new ButtonGroup();
		bgOperacao.add(rbEntradaFardo);
		bgOperacao.add(rbEntradaUnidade);
		bgOperacao.add(rbSaida);
		rbEntradaUnidade.setSelected(true);
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
					tabbedPane.setSelectedIndex(2);
					tfCodigoBarrasAdicionar.setText(codigo);
					tfNomeProduto.setText("");
					tfNomeProduto.requestFocus();
				}
				tfCodigo.setText("");
				return;
			}

			rbEntradaFardo.setEnabled(p.getTipo() == Produto.TipoProduto.FARDO);
			if (!rbEntradaFardo.isEnabled()) {
				rbEntradaUnidade.setSelected(true);
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
				    controller.adicionarFardos(produtoAtual[0], qtd);
				} else if (rbEntradaUnidade.isSelected()) {
				    controller.adicionarQuantidade(produtoAtual[0], qtd);
				} else {
				    boolean ok = controller.removerQuantidade(produtoAtual[0], qtd);
				    if (!ok) {
				        lblMensagem.setText("Estoque insuficiente.");
				        // Não atualiza tabelas e não busca novo total, pois não houve alteração
				        return;
				    }
				}

				// 🆕 Busca o produto atualizado do banco para exibir o estoque real
				Produto atualizado = controller.buscarProdutoPorCodigoBarras(produtoAtual[0].getCodigoBarras());
				if (atualizado != null) {
				    lblMensagem.setText("Operação registrada! Estoque atual: " + atualizado.getTotalUnidades() + " unidade(s)");
				} else {
				    lblMensagem.setText("Operação registrada!");
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
				JOptionPane.showMessageDialog(panel, "Quantidade inválida.", "Erro", JOptionPane.WARNING_MESSAGE);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(panel, "Erro na operação:\n" + ex.getMessage(), "Falha", JOptionPane.ERROR_MESSAGE);
			}
		});

		return panel;
	}

	// ==================== 7. RELATÓRIOS ====================
	private JPanel criarPainelRelatorios() {
	    JPanel panel = new JPanel(new BorderLayout(10, 10));

	    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
	    JRadioButton rbMovimentacoes = new JRadioButton("Movimentações");
	    JRadioButton rbEstado = new JRadioButton("Estado do Estoque");
	    ButtonGroup bgTipo = new ButtonGroup();
	    bgTipo.add(rbMovimentacoes);
	    bgTipo.add(rbEstado);
	    rbMovimentacoes.setSelected(true);
	    topPanel.add(rbMovimentacoes);
	    topPanel.add(rbEstado);
	    

	    JPanel filtroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
	    filtroPanel.setPreferredSize(new java.awt.Dimension(filtroPanel.getPreferredSize().width, 70));
	    JLabel lblInicio = new JLabel("Data Início:");
	    JTextField tfInicio = new JTextField(12);
	    tfInicio.setToolTipText("d-M-yyyy (dia), M-yyyy (mês) ou yyyy (ano)");
	    JLabel lblFim = new JLabel("Data Fim:");
	    JTextField tfFim = new JTextField(12);
	    tfFim.setToolTipText("d-M-yyyy (dia), M-yyyy (mês) ou yyyy (ano)");
	   

	    JLabel lblNomeFiltro = new JLabel("Nome:");
	    JTextField tfFiltroNome = new JTextField(12);
	    tfFiltroNome.setToolTipText("Deixe em branco para listar todos os produtos");
	    JLabel lblCatFiltro = new JLabel("Categoria:");
	    cbFiltroCategoria = new JComboBox<>();
	    cbFiltroCategoria.addItem(new Categoria(0, "Todas"));
	    for (Categoria c : controller.listarCategorias()) {
	        cbFiltroCategoria.addItem(c);
	    }

	    JCheckBox chkZerados = new JCheckBox("Apenas zerados");
	    chkZerados.setVisible(false);
	    JButton btnGerar = new JButton("Gerar Relatório");
	    JButton btnImprimir = new JButton("Imprimir");
	    JButton btnExportar = new JButton("Exportar CSV");

	    filtroPanel.add(lblInicio);
	    filtroPanel.add(tfInicio);
	    filtroPanel.add(lblFim);
	    filtroPanel.add(tfFim);
	    filtroPanel.add(lblNomeFiltro);
	    filtroPanel.add(tfFiltroNome);
	    filtroPanel.add(lblCatFiltro);
	    filtroPanel.add(cbFiltroCategoria);
	    filtroPanel.add(chkZerados);
	    filtroPanel.add(btnGerar);
	    filtroPanel.add(btnImprimir);
	    filtroPanel.add(btnExportar);

	    DefaultTableModel tableModel = new DefaultTableModel();
	    JTable table = new JTable(tableModel);
	    table.setDefaultEditor(Object.class, null);
	    JScrollPane scrollPane = new JScrollPane(table);
	    
	 // 🆕 Painel de totais (inicialmente oculto)
	    JPanel painelTotais = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
	    JLabel lblTotalEntradas = new JLabel("Entradas: R$ 0,00");
	    JLabel lblTotalSaidas = new JLabel("Saídas: R$ 0,00");
	    JLabel lblTotalLiquido = new JLabel("Líquido: R$ 0,00");
	    painelTotais.add(lblTotalEntradas);
	    painelTotais.add(lblTotalSaidas);
	    painelTotais.add(lblTotalLiquido);
	    painelTotais.setVisible(false);

	    rbMovimentacoes.addActionListener(e -> {
	        lblFim.setVisible(true);
	        tfFim.setVisible(true);
	        chkZerados.setVisible(false);
	    });
	    rbEstado.addActionListener(e -> {
	        lblFim.setVisible(true);   // agora fica visível (opcional)
	        tfFim.setVisible(true);    // idem
	        chkZerados.setVisible(true);
	    });

	    btnGerar.addActionListener(e -> {
	        try {
	        	painelTotais.setVisible(false);
	            String nomeFiltro = tfFiltroNome.getText().trim();
	            Categoria catSel = (Categoria) cbFiltroCategoria.getSelectedItem();
	            Integer catId = (catSel != null && catSel.getId() != 0) ? catSel.getId() : null;
	            boolean apenasZerados = chkZerados.isSelected();

	            if (rbMovimentacoes.isSelected()) {
	                // Relatório de Movimentações (período) – inalterado
	                String inicioStr = tfInicio.getText().trim();
	                String fimStr = tfFim.getText().trim();

	                if (inicioStr.isEmpty() || fimStr.isEmpty()) {
	                    JOptionPane.showMessageDialog(panel,
	                            "Informe os dois campos de data (formato d-M-yyyy, M-yyyy ou yyyy).",
	                            "Campos obrigatórios", JOptionPane.WARNING_MESSAGE);
	                    return;
	                }

	                LocalDate[] intervaloInicio = parseDataFlexivel(inicioStr);
	                LocalDate[] intervaloFim = parseDataFlexivel(fimStr);

	                if (intervaloInicio == null || intervaloFim == null) {
	                    JOptionPane.showMessageDialog(panel,
	                            "Formato de data inválido. Use d-M-yyyy, M-yyyy ou yyyy.",
	                            "Erro", JOptionPane.ERROR_MESSAGE);
	                    return;
	                }

	                LocalDate dataInicial = intervaloInicio[0];
	                LocalDate dataFinal = intervaloFim[1];

	                if (dataInicial.isAfter(dataFinal)) {
	                    JOptionPane.showMessageDialog(panel,
	                            "A data inicial não pode ser maior que a final.",
	                            "Período inválido", JOptionPane.WARNING_MESSAGE);
	                    return;
	                }
	                if (dataFinal.isAfter(LocalDate.now())) {
	                    JOptionPane.showMessageDialog(panel,
	                            "A data final não pode ser futura.",
	                            "Data inválida", JOptionPane.WARNING_MESSAGE);
	                    return;
	                }

	                List<Movimentacao> movs = controller.getMovimentacoesPorPeriodo(
	                        dataInicial, dataFinal,
	                        nomeFiltro.isEmpty() ? null : nomeFiltro,
	                        catId);
	                String[] colunas = {"Data/Hora", "Produto", "Tipo", "Quantidade", "Descrição"};
	                tableModel.setColumnIdentifiers(colunas);
	                tableModel.setRowCount(0);
	                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	                for (Movimentacao m : movs) {
	                    tableModel.addRow(new Object[]{
	                            m.getDataHora().format(fmt),
	                            m.getProdutoNome(),
	                            m.getTipo(),
	                            m.getQuantidadeUnidades(),
	                            m.getDescricao()
	                    });
	                }
	                if (movs.isEmpty()) {
	                    JOptionPane.showMessageDialog(panel,
	                            "Nenhuma movimentação encontrada no período.",
	                            "Informação", JOptionPane.INFORMATION_MESSAGE);
	                }
	            } else {
	                // ===== RELATÓRIO DE ESTADO DO ESTOQUE (DATA OU PERÍODO) =====
	                String dataStr = tfInicio.getText().trim();
	                String fimStr = tfFim.getText().trim();

	                if (dataStr.isEmpty()) {
	                    JOptionPane.showMessageDialog(panel,
	                            "Informe uma data (formato d-M-yyyy, M-yyyy ou yyyy).",
	                            "Data obrigatória", JOptionPane.WARNING_MESSAGE);
	                    return;
	                }

	                LocalDate[] intervaloInicio = parseDataFlexivel(dataStr);
	                if (intervaloInicio == null) {
	                    JOptionPane.showMessageDialog(panel,
	                            "Formato de data inválido. Use d-M-yyyy, M-yyyy ou yyyy.",
	                            "Erro", JOptionPane.ERROR_MESSAGE);
	                    return;
	                }
	                LocalDate dataReferencia = intervaloInicio[1];

	                // Verifica se a data fim foi preenchida
	                if (!fimStr.isEmpty()) {
	                    // MODO PERÍODO (movimento)
	                    LocalDate[] intervaloFim = parseDataFlexivel(fimStr);
	                    if (intervaloFim == null) {
	                        JOptionPane.showMessageDialog(panel,
	                                "Formato de data fim inválido. Use d-M-yyyy, M-yyyy ou yyyy.",
	                                "Erro", JOptionPane.ERROR_MESSAGE);
	                        return;
	                    }
	                    LocalDate dataFim = intervaloFim[1];

	                    if (dataReferencia.isAfter(dataFim)) {
	                        JOptionPane.showMessageDialog(panel,
	                                "A data inicial não pode ser maior que a final.",
	                                "Período inválido", JOptionPane.WARNING_MESSAGE);
	                        return;
	                    }
	                    if (dataFim.isAfter(LocalDate.now())) {
	                        JOptionPane.showMessageDialog(panel,
	                                "A data final não pode ser futura.",
	                                "Data inválida", JOptionPane.WARNING_MESSAGE);
	                        return;
	                    }

	                    // Consulta o movimento no período
	                    List<ProdutoMovimento> movs = controller.getMovimentoNoPeriodo(
	                            dataReferencia, dataFim,
	                            nomeFiltro.isEmpty() ? null : nomeFiltro,
	                            catId);

	                    String[] colunas = {"Código", "Nome", "Entradas", "Saídas", "Saldo Líq.", "V. Entradas", "V. Saídas", "V. Líquido"};
	                    tableModel.setColumnIdentifiers(colunas);
	                    tableModel.setRowCount(0);

	                    NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_BR);
	                    for (ProdutoMovimento pm : movs) {
	                        String valEntStr = pm.getValorEntradas() != null ? nf.format(pm.getValorEntradas()) : "-";
	                        String valSaiStr = pm.getValorSaidas() != null ? nf.format(pm.getValorSaidas()) : "-";
	                        String valLiqStr = pm.getValorLiquido() != null ? nf.format(pm.getValorLiquido()) : "-";
	                        tableModel.addRow(new Object[]{
	                                pm.getCodigoBarras(),
	                                pm.getNome(),
	                                pm.getTotalEntradas(),
	                                pm.getTotalSaidas(),
	                                pm.getSaldoLiquido(),
	                                valEntStr,
	                                valSaiStr,
	                                valLiqStr
	                        });
	                    }
	                    
	                 // 🆕 Cálculo e exibição dos totais
	                    BigDecimal somaEntradas = BigDecimal.ZERO;
	                    BigDecimal somaSaidas = BigDecimal.ZERO;
	                    BigDecimal somaLiquido = BigDecimal.ZERO;

	                    for (ProdutoMovimento pm : movs) {
	                        if (pm.getValorEntradas() != null) somaEntradas = somaEntradas.add(pm.getValorEntradas());
	                        if (pm.getValorSaidas() != null)   somaSaidas   = somaSaidas.add(pm.getValorSaidas());
	                        if (pm.getValorLiquido() != null)  somaLiquido  = somaLiquido.add(pm.getValorLiquido());
	                    }

	                    
	                    lblTotalEntradas.setText("Entradas: " + nf.format(somaEntradas));
	                    lblTotalSaidas.setText("Saídas: " + nf.format(somaSaidas));
	                    lblTotalLiquido.setText("Líquido: " + nf.format(somaLiquido));
	                    painelTotais.setVisible(true);

	                    if (movs.isEmpty()) {
	                        JOptionPane.showMessageDialog(panel,
	                                "Nenhum produto encontrado no período.",
	                                "Informação", JOptionPane.INFORMATION_MESSAGE);
	                    }
	                } else {
	                    // MODO DATA ÚNICA (estoque na data) – comportamento original
	                    if (dataReferencia.isAfter(LocalDate.now())) {
	                        JOptionPane.showMessageDialog(panel,
	                                "A data não pode ser futura.",
	                                "Data inválida", JOptionPane.WARNING_MESSAGE);
	                        return;
	                    }

	                    List<ProdutoSaldo> saldos = controller.getEstadoEstoqueNaData(
	                            dataReferencia,
	                            nomeFiltro.isEmpty() ? null : nomeFiltro,
	                            catId,
	                            apenasZerados);

	                    String[] colunas = {"Código", "Nome", "Fardos", "Unid. Avulsas", "Total Unid.", "Preço Unit.", "Valor Total"};
	                    tableModel.setColumnIdentifiers(colunas);
	                    tableModel.setRowCount(0);

	                    NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_BR);
	                    for (ProdutoSaldo ps : saldos) {
	                        String precoStr = ps.getPrecoUnitario() != null ? nf.format(ps.getPrecoUnitario()) : "-";
	                        String valorStr = ps.getValorTotal() != null ? nf.format(ps.getValorTotal()) : "-";
	                        tableModel.addRow(new Object[]{
	                                ps.getCodigoBarras(),
	                                ps.getNome(),
	                                ps.getFardos(),
	                                ps.getUnidadesAvulsas(),
	                                ps.getTotalUnidades(),
	                                precoStr,
	                                valorStr
	                        });
	                    }

	                    // Renderizador de zerados (apenas para modo data única)
	                    table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
	                        @Override
	                        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
	                                boolean isSelected, boolean hasFocus, int row, int column) {
	                            java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	                            if (table.getColumnCount() > 4 && "Total Unid.".equals(table.getColumnName(4))) {
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
	                            }
	                            return c;
	                        }
	                    });

	                    if (saldos.isEmpty()) {
	                        JOptionPane.showMessageDialog(panel,
	                                "Nenhum produto encontrado.",
	                                "Informação", JOptionPane.INFORMATION_MESSAGE);
	                    }
	                }
	            }
	        } catch (DateTimeParseException ex) {
	            JOptionPane.showMessageDialog(panel,
	                    "Formato de data inválido. Use os formatos indicados.",
	                    "Erro", JOptionPane.ERROR_MESSAGE);
	        } catch (Exception ex) {
	            JOptionPane.showMessageDialog(panel,
	                    "Erro ao gerar relatório: " + ex.getMessage(),
	                    "Erro", JOptionPane.ERROR_MESSAGE);
	        }
	    });

	    btnImprimir.addActionListener(e -> {
	        if (table.getColumnCount() < 2) {
	            JOptionPane.showMessageDialog(panel,
	                    "Gere um relatório antes de imprimir.",
	                    "Aviso", JOptionPane.WARNING_MESSAGE);
	            return;
	        }
	        java.awt.Font originalFont = table.getFont();
	        int originalWidthCodigo = table.getColumnModel().getColumn(0).getPreferredWidth();
	        int originalWidthNome = table.getColumnModel().getColumn(1).getPreferredWidth();
	        try {
	            table.setFont(originalFont.deriveFont(18f));
	            table.getColumnModel().getColumn(0).setPreferredWidth(150);
	            table.getColumnModel().getColumn(1).setPreferredWidth(250);

	            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
	            job.setJobName("Relatório de Estoque");
	            java.awt.print.PageFormat format = job.defaultPage();
	            format.setOrientation(java.awt.print.PageFormat.LANDSCAPE);
	            java.awt.print.Paper paper = format.getPaper();
	            paper.setImageableArea(30, 30, paper.getWidth() - 60, paper.getHeight() - 60);
	            format.setPaper(paper);
	            job.setPrintable(table.getPrintable(javax.swing.JTable.PrintMode.FIT_WIDTH, null, null), format);

	            if (job.printDialog()) {
	                job.print();
	            }
	        } catch (java.awt.print.PrinterException ex) {
	            JOptionPane.showMessageDialog(panel, "Erro ao imprimir: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
	        } finally {
	            table.getColumnModel().getColumn(0).setPreferredWidth(originalWidthCodigo);
	            table.getColumnModel().getColumn(1).setPreferredWidth(originalWidthNome);
	            table.setFont(originalFont);
	        }
	    });

	    btnExportar.addActionListener(e -> {
	        JFileChooser fileChooser = new JFileChooser();
	        fileChooser.setSelectedFile(new java.io.File("relatorio.csv"));
	        int result = fileChooser.showSaveDialog(panel);
	        if (result == JFileChooser.APPROVE_OPTION) {
	            java.io.File arquivo = fileChooser.getSelectedFile();
	            try (java.io.PrintWriter pw = new java.io.PrintWriter(
	                    new java.io.OutputStreamWriter(
	                            new java.io.FileOutputStream(arquivo), java.nio.charset.StandardCharsets.UTF_8))) {
	                pw.print('\uFEFF');

	                for (int i = 0; i < tableModel.getColumnCount(); i++) {
	                    pw.print(tableModel.getColumnName(i));
	                    if (i < tableModel.getColumnCount() - 1) pw.print(";");
	                }
	                pw.println();

	                for (int row = 0; row < tableModel.getRowCount(); row++) {
	                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
	                        Object valor = tableModel.getValueAt(row, col);
	                        String texto = valor != null ? valor.toString() : "";
	                        if (col == 0) {
	                            pw.print("\t" + texto);
	                        } else {
	                            pw.print(texto);
	                        }
	                        if (col < tableModel.getColumnCount() - 1) pw.print(";");
	                    }
	                    pw.println();
	                }
	                JOptionPane.showMessageDialog(panel, "Arquivo salvo com sucesso!");
	            } catch (Exception ex) {
	                JOptionPane.showMessageDialog(panel, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
	            }
	        }
	    });

	 // Painel que agrupa radio buttons e filtros verticalmente
	    JPanel topContainer = new JPanel();
	    topContainer.setLayout(new javax.swing.BoxLayout(topContainer, javax.swing.BoxLayout.Y_AXIS));
	    topContainer.add(topPanel);
	    topContainer.add(filtroPanel);

	    // Painel central com tabela e totais
	    JPanel centerPanel = new JPanel(new BorderLayout());
	    centerPanel.add(scrollPane, BorderLayout.CENTER);
	    centerPanel.add(painelTotais, BorderLayout.SOUTH);

	    panel.add(topContainer, BorderLayout.NORTH);
	    panel.add(centerPanel, BorderLayout.CENTER);
	    return panel;
	}

	private LocalDate[] parseDataFlexivel(String texto) throws DateTimeParseException {
		if (texto == null || texto.trim().isEmpty()) {
			return null;
		}
		texto = texto.trim();

		// Tenta como dia com padrões flexíveis
		try {
			DateTimeFormatter fmt = new DateTimeFormatterBuilder()
					.appendPattern("[d-M-yyyy][dd-MM-yyyy][d-MM-yyyy][dd-M-yyyy]")
					.toFormatter();
			LocalDate data = LocalDate.parse(texto, fmt);
			return new LocalDate[]{data, data};
		} catch (DateTimeParseException ignored) {}

		// Tenta como mês
		try {
			DateTimeFormatter fmt = new DateTimeFormatterBuilder()
					.appendPattern("[M-yyyy][MM-yyyy]")
					.toFormatter();
			YearMonth ym = YearMonth.parse(texto, fmt);
			return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
		} catch (DateTimeParseException ignored) {}

		// Tenta como ano
		if (texto.matches("\\d{4}")) {
			int ano = Integer.parseInt(texto);
			return new LocalDate[]{LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31)};
		}

		throw new DateTimeParseException("Formato inválido", texto, 0);
	}

	private JPanel criarPainelCategorias() {
		JPanel panel = new JPanel(new BorderLayout());
		DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nome"}, 0);
		JTable table = new JTable(model);
		table.setDefaultEditor(Object.class, null);
		atualizarTabelaCategorias(model);

		JPanel botoes = new JPanel();
		JTextField tfNomeCat = new JTextField(15);
		JButton btnAdicionar = new JButton("Adicionar");
		JButton btnRemover = new JButton("Remover Selecionada");
		botoes.add(new JLabel("Nome:"));
		botoes.add(tfNomeCat);
		botoes.add(btnAdicionar);
		botoes.add(btnRemover);

		btnAdicionar.addActionListener(e -> {
			String nome = tfNomeCat.getText().trim();
			if (nome.isEmpty()) {
				JOptionPane.showMessageDialog(panel, "Digite um nome.");
				return;
			}
			try {
				controller.adicionarCategoria(nome);
				tfNomeCat.setText("");
				atualizarTabelaCategorias(model);
				atualizarComboCategorias();
			} catch (IllegalArgumentException ex) {
				JOptionPane.showMessageDialog(panel, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
			}
		});

		btnRemover.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row == -1) {
				JOptionPane.showMessageDialog(panel, "Selecione uma categoria.");
				return;
			}
			int id = (int) model.getValueAt(row, 0);
			String nome = (String) model.getValueAt(row, 1);
			int resp = JOptionPane.showConfirmDialog(panel, "Excluir categoria " + nome + "?");
			if (resp == JOptionPane.YES_OPTION) {
				try {
					controller.removerCategoria(id);
					atualizarTabelaCategorias(model);
					atualizarComboCategorias();
				} catch (IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(panel, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		panel.add(botoes, BorderLayout.SOUTH);
		return panel;
	}

	private void atualizarTabelaCategorias(DefaultTableModel model) {
		model.setRowCount(0);
		for (Categoria c : controller.listarCategorias()) {
			model.addRow(new Object[]{c.getId(), c.getNome()});
		}
	}

	private void atualizarComboCategorias() {
		if (cbCategoriaAdicionar != null) {
			cbCategoriaAdicionar.removeAllItems();
			cbCategoriaAdicionar.addItem(new Categoria(0, "Nenhuma"));
			for (Categoria c : controller.listarCategorias()) {
				cbCategoriaAdicionar.addItem(c);
			}
		}
		if (cbFiltroCategoria != null) {
			cbFiltroCategoria.removeAllItems();
			cbFiltroCategoria.addItem(new Categoria(0, "Todas"));
			for (Categoria c : controller.listarCategorias()) {
				cbFiltroCategoria.addItem(c);
			}
		}
	}
}