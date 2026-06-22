package estoque.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.geom.Ellipse2D;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class GraficoPrecoView extends JFrame {

    // Construtor original (um produto) – eixo Y padrão
    public GraficoPrecoView(String nomeProduto, Map<LocalDate, BigDecimal> historico) {
        this(nomeProduto, historico, "Preço Unitário (R$)");
    }

    // Construtor para um produto com eixo Y customizável
    public GraficoPrecoView(String nomeProduto, Map<LocalDate, BigDecimal> historico, String eixoY) {
        super("Histórico - " + nomeProduto);
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(criarSerie(nomeProduto, historico));
        configurarJanela(dataset, "Histórico - " + nomeProduto, eixoY);
    }

    // Construtor para múltiplos produtos (preço unitário ou valor gasto) com eixoY
    public GraficoPrecoView(String titulo, Map<String, Map<LocalDate, BigDecimal>> dadosPorProduto,
                            boolean isMulti, String eixoY) {
        super(titulo);
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (Map.Entry<String, Map<LocalDate, BigDecimal>> entry : dadosPorProduto.entrySet()) {
            dataset.addSeries(criarSerie(entry.getKey(), entry.getValue()));
        }
        configurarJanela(dataset, titulo, eixoY);
    }

    // Construtor antigo de múltiplos sem eixoY (mantido para compatibilidade)
    public GraficoPrecoView(String titulo, Map<String, Map<LocalDate, BigDecimal>> dadosPorProduto, boolean isMulti) {
        this(titulo, dadosPorProduto, isMulti, "Preço Unitário (R$)");
    }

    private XYSeries criarSerie(String nome, Map<LocalDate, BigDecimal> historico) {
        XYSeries series = new XYSeries(nome);
        if (historico.isEmpty()) return series;

        if (historico.size() == 1) {
            Map.Entry<LocalDate, BigDecimal> unico = historico.entrySet().iterator().next();
            long inicio = unico.getKey().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            series.add(inicio, unico.getValue().doubleValue());
            long fim = unico.getKey().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            series.add(fim, unico.getValue().doubleValue());
            return series;
        }

        for (Map.Entry<LocalDate, BigDecimal> entry : historico.entrySet()) {
            long millis = entry.getKey()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            series.add(millis, entry.getValue().doubleValue());
        }
        return series;
    }

    private void configurarJanela(XYSeriesCollection dataset, String tituloGrafico, String eixoY) {
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JFreeChart chart = ChartFactory.createXYLineChart(
                tituloGrafico,
                "Data",
                eixoY,              // eixo Y customizado
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(new GradientPaint(0, 0, new Color(240, 248, 255),
                0, 600, new Color(255, 255, 255)));
        plot.setDomainGridlinePaint(new Color(180, 180, 180));
        plot.setRangeGridlinePaint(new Color(180, 180, 180));
        plot.setDomainGridlineStroke(new BasicStroke(0.5f));
        plot.setRangeGridlineStroke(new BasicStroke(0.5f));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        int seriesCount = dataset.getSeriesCount();

        for (int i = 0; i < seriesCount; i++) {
            Color cor = gerarCor(i, seriesCount);
            renderer.setSeriesPaint(i, cor);
            renderer.setSeriesStroke(i, new BasicStroke(2.5f));
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesShape(i, new Ellipse2D.Double(-3, -3, 6, 6));
            renderer.setSeriesFillPaint(i, cor);
            renderer.setUseFillPaint(true);
        }
        plot.setRenderer(renderer);

        DateAxis dateAxis = new DateAxis("Data");
        dateAxis.setDateFormatOverride(new java.text.SimpleDateFormat("dd/MM/yyyy"));
        dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        dateAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 13));
        plot.setDomainAxis(dateAxis);

        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 13));

        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        chart.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(780, 560));
        setContentPane(chartPanel);
    }

    private Color gerarCor(int indice, int total) {
        float hue = (float) indice / total;
        return Color.getHSBColor(hue, 0.7f, 0.9f);
    }
}