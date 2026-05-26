import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardForm extends JFrame {

    private JLabel lblMeusRespondidos, lblTotalCasos, lblPorcentagemFalta, lblTaxaAdesao;
    private JPanel panelGraficoPizza, panelGraficoBarras, panelGraficoLinhas;
    private final int ID_ALUNO_LOGADO = 1; 

    public DashboardForm() {
        setTitle("Portal do Aluno - Painel de Atividades Clínicas");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        inicializarComponentes();
        carregarDadosPainel();
    }

    private void inicializarComponentes() {
        JPanel panelKPIs = new JPanel(new GridLayout(1, 4, 15, 15));
        panelKPIs.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelKPIs.setBackground(new Color(242, 246, 248));

        lblMeusRespondidos = criarCardKPI("Seus Casos Respondidos");
        lblTotalCasos = criarCardKPI("Total de Casos no Sistema");
        lblPorcentagemFalta = criarCardKPI("Casos Pendentes");
        lblTaxaAdesao = criarCardKPI("Taxa de Adesão Geral");

        panelKPIs.add(lblMeusRespondidos);
        panelKPIs.add(lblTotalCasos);
        panelKPIs.add(lblPorcentagemFalta);
        panelKPIs.add(lblTaxaAdesao);
        add(panelKPIs, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new GridLayout(1, 3, 15, 15));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        
        panelGraficoPizza = new JPanel(new BorderLayout());
        panelGraficoBarras = new JPanel(new BorderLayout());
        panelGraficoLinhas = new JPanel(new BorderLayout());
        
        panelCentro.add(panelGraficoPizza);
        panelCentro.add(panelGraficoBarras);
        panelCentro.add(panelGraficoLinhas);
        add(panelCentro, BorderLayout.CENTER);
    }

    private JLabel criarCardKPI(String titulo) {
        JLabel label = new JLabel("<html><center><b>" + titulo + "</b><br><font size='5' color='#2C3E50'>0</font></center></html>", SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setBorder(BorderFactory.createLineBorder(new Color(215, 220, 225), 1, true));
        return label;
    }

    private void carregarDadosPainel() {
        try (Connection conn = ConexaoDB.getConexao()) {
            
            String sqlKPI = "SELECT (SELECT COUNT(*) FROM respostas_casos WHERE id_aluno = ?) AS respondidos, " +
                            "(SELECT COUNT(*) FROM casos_clinicos) AS total, " +
                            "(SELECT COUNT(DISTINCT id_aluno) FROM respostas_casos) AS alunos_ativos, " +
                            "(SELECT COUNT(*) FROM alunos) AS total_alunos";
            PreparedStatement stmt = conn.prepareStatement(sqlKPI);
            stmt.setInt(1, ID_ALUNO_LOGADO);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int respondidos = rs.getInt("respondidos");
                int total = rs.getInt("total");
                int pendentes = total - respondidos;
                
                double ativos = rs.getDouble("alunos_ativos");
                double totalAlunos = rs.getDouble("total_alunos");
                double taxa = (totalAlunos > 0) ? (ativos / totalAlunos) * 100 : 0;

                lblMeusRespondidos.setText("<html><center><b>Seus Casos Respondidos</b><br><font size='6' color='#27AE60'>" + respondidos + "</font></center></html>");
                lblTotalCasos.setText("<html><center><b>Total de Casos no Sistema</b><br><font size='6' color='#2980B9'>" + total + "</font></center></html>");
                lblPorcentagemFalta.setText("<html><center><b>Casos Pendentes</b><br><font size='6' color='#E67E22'>" + pendentes + "</font></center></html>");
                lblTaxaAdesao.setText("<html><center><b>Taxa de Adesão Geral</b><br><font size='6' color='#8E44AD'>" + String.format("%.1f", taxa) + "%</font></center></html>");
            }

            PieChart chartPizza = new PieChartBuilder().width(400).height(400).title("Engajamento (Alunos)").theme(Styler.ChartTheme.GGPlot2).build();
            String sqlPizza = "SELECT 'Responderam' AS status, COUNT(DISTINCT id_aluno) AS total FROM respostas_casos UNION SELECT 'Não Responderam' AS status, (SELECT COUNT(*) FROM alunos) - COUNT(DISTINCT id_aluno) AS total FROM respostas_casos";
            ResultSet rsPizza = conn.prepareStatement(sqlPizza).executeQuery();
            while (rsPizza.next()) {
                chartPizza.addSeries(rsPizza.getString(1), rsPizza.getInt(2));
            }
            panelGraficoPizza.add(new XChartPanel<>(chartPizza), BorderLayout.CENTER);

            CategoryChart chartBarras = new CategoryChartBuilder().width(400).height(400).title("Volume por Curso").xAxisTitle("Curso").yAxisTitle("Respostas").build();
            chartBarras.getStyler().setLegendVisible(false);
            chartBarras.getStyler().setXAxisLabelRotation(45); 
            
            List<String> cursos = new ArrayList<>();
            List<Integer> quantidades = new ArrayList<>();
            String sqlBarras = "SELECT c.nome_curso, COUNT(rc.id_resposta) FROM cursos c LEFT JOIN alunos a ON c.id_curso = a.id_curso LEFT JOIN respostas_casos rc ON a.id_aluno = rc.id_aluno GROUP BY c.nome_curso ORDER BY c.nome_curso ASC";
            ResultSet rsBars = conn.prepareStatement(sqlBarras).executeQuery();
            while (rsBars.next()) {
                cursos.add(rsBars.getString(1));
                quantidades.add(rsBars.getInt(2));
            }
            chartBarras.addSeries("Respostas", cursos, quantidades);
            panelGraficoBarras.add(new XChartPanel<>(chartBarras), BorderLayout.CENTER);

            XYChart chartLinha = new XYChartBuilder().width(400).height(400).title("Histórico Temporal de Envio").xAxisTitle("Data").yAxisTitle("Total Respostas").build();
            chartLinha.getStyler().setLegendVisible(false);
            
            List<Date> datas = new ArrayList<>();
            List<Integer> respostasPorDia = new ArrayList<>();
            String sqlLinhas = "SELECT data_resposta, COUNT(id_resposta) FROM respostas_casos GROUP BY data_resposta ORDER BY data_resposta ASC";
            ResultSet rsLines = conn.prepareStatement(sqlLinhas).executeQuery();
            while (rsLines.next()) {
                datas.add(rsLines.getDate(1));
                respostasPorDia.add(rsLines.getInt(2));
            }
            if (!datas.isEmpty()) {
                chartLinha.addSeries("Envios", datas, respostasPorDia);
            }
            panelGraficoLinhas.add(new XChartPanel<>(chartLinha), BorderLayout.CENTER);

            validate();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar dados: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new DashboardForm().setVisible(true));
    }
}