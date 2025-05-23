package org.semaflux.sim.visualization;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.semaflux.sim.simulação.Estatisticas;

/**
 * Classe responsável por mostrar uma janela de resumo com gráficos 
 * detalhados ao final da simulação.
 */
public class ResumoSimulacao {
    
    private Estatisticas estatisticas;
    private Stage stage;
    
    // Tema de cores
    private final String PRIMARY_COLOR = "#2b6cb0";
    private final String SECONDARY_COLOR = "#3182ce";
    private final String ACCENT_COLOR = "#4299e1";
    private final String BACKGROUND_COLOR = "#f0f4f8";
    private final String PANEL_COLOR = "#ffffff";
    private final String TEXT_COLOR = "#2d3748";
    
    public ResumoSimulacao(Estatisticas estatisticas) {
        this.estatisticas = estatisticas;
    }
    
    /**
     * Mostra a janela de resumo da simulação com gráficos detalhados.
     */
    public void mostrarResumo() {
        Platform.runLater(() -> {
            stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Resumo da Simulação - Semaflux");
            stage.setWidth(1000);
            stage.setHeight(700);
            
         
            
            // Criar layout principal
            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");
            
            // Cabeçalho
            VBox header = criarCabecalho();
            root.setTop(header);
            
            // Conteúdo principal com abas
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
            
            // Aba de Resumo
            Tab resumoTab = new Tab("Resumo Geral");
            resumoTab.setContent(criarPainelResumo());
            
            // Aba de Gráficos
            Tab graficosTab = new Tab("Gráficos Detalhados");
            graficosTab.setContent(criarPainelGraficos());
            
            tabPane.getTabs().addAll(resumoTab, graficosTab);
            root.setCenter(tabPane);
            
            // Rodapé com botões
            HBox footer = criarRodape();
            root.setBottom(footer);
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        });
    }
    
    private VBox criarCabecalho() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");
        
        Text titulo = new Text("Resumo Completo da Simulação");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        titulo.setFill(Color.WHITE);
        
        Text subtitulo = new Text("Análise de desempenho da rede de tráfego simulada");
        subtitulo.setFont(Font.font("System", 16));
        subtitulo.setFill(Color.WHITE);
        
        header.getChildren().addAll(titulo, subtitulo);
        
        return header;
    }
    
    private ScrollPane criarPainelResumo() {
        VBox conteudo = new VBox(20);
        conteudo.setPadding(new Insets(20));
        
        // Seção de estatísticas principais
        VBox estatisticasPrincipais = criarSecaoEstatisticasPrincipais();
        
        // Gráficos básicos
        HBox graficosBasicos = new HBox(15);
        graficosBasicos.setPadding(new Insets(10));
        
        VBox graficoVeiculos = criarGraficoVeiculos();
        VBox graficoTempos = criarGraficoTempos();
        
        graficosBasicos.getChildren().addAll(graficoVeiculos, graficoTempos);
        HBox.setHgrow(graficoVeiculos, Priority.ALWAYS);
        HBox.setHgrow(graficoTempos, Priority.ALWAYS);
        
        // Adicionar tudo ao conteúdo
        conteudo.getChildren().addAll(estatisticasPrincipais, new Separator(), graficosBasicos);
        
        // Criar ScrollPane para conteúdo
        ScrollPane scrollPane = new ScrollPane(conteudo);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }
    
    private VBox criarSecaoEstatisticasPrincipais() {
        VBox secao = new VBox(15);
        secao.setPadding(new Insets(15));
        secao.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");
        
        Text tituloSecao = new Text("Estatísticas Principais");
        tituloSecao.setFont(Font.font("System", FontWeight.BOLD, 18));
        tituloSecao.setFill(Color.web(PRIMARY_COLOR));
        
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(10);
        
        // Primeira coluna - Estatísticas de veículos
        criarLabel(grid, "Tempo Total de Simulação:", 0, 0);
        criarLabel(grid, "Total de Veículos Gerados:", 0, 1);
        criarLabel(grid, "Veículos Chegados ao Destino:", 0, 2);
        criarLabel(grid, "Taxa de Chegada:", 0, 3);
        
        criarValor(grid, String.format("%.2f segundos", estatisticas.getCurrentTime()), 1, 0);
        criarValor(grid, String.format("%d", estatisticas.getTotalVehiclesGenerated()), 1, 1);
        criarValor(grid, String.format("%d", estatisticas.getVehiclesArrived()), 1, 2);
        criarValor(grid, String.format("%.2f%%", estatisticas.getArrivalRate()), 1, 3);
        
        // Segunda coluna - Tempos
        criarLabel(grid, "Tempo Médio de Viagem:", 2, 0);
        criarLabel(grid, "Tempo Máximo de Viagem:", 2, 1);
        criarLabel(grid, "Tempo Médio de Espera:", 2, 2);
        criarLabel(grid, "Tempo Máximo de Espera:", 2, 3);
        
        criarValor(grid, String.format("%.2f segundos", estatisticas.getAverageTravelTime()), 3, 0);
        criarValor(grid, String.format("%.2f segundos", estatisticas.getMaxTravelTime()), 3, 1);
        criarValor(grid, String.format("%.2f segundos", estatisticas.getAverageWaitTime()), 3, 2);
        criarValor(grid, String.format("%.2f segundos", estatisticas.getMaxWaitTime()), 3, 3);
        
        // Terceira coluna - Combustível e congestionamento
        criarLabel(grid, "Consumo Total de Combustível:", 4, 0);
        criarLabel(grid, "Consumo Médio por Veículo:", 4, 1);
        criarLabel(grid, "Pico de Congestionamento:", 4, 2);
        criarLabel(grid, "Média de Congestionamento:", 4, 3);
        
        criarValor(grid, String.format("%.3f litros", estatisticas.getTotalFuelConsumed()), 5, 0);
        criarValor(grid, String.format("%.3f litros", estatisticas.getAverageFuelConsumptionPerVehicle()), 5, 1);
        criarValor(grid, String.format("%.2f%%", estatisticas.getMaxRecordedCongestionRatio()), 5, 2);
        criarValor(grid, String.format("%.2f%%", estatisticas.getAverageCongestionRatio()), 5, 3);
        
        secao.getChildren().addAll(tituloSecao, grid);
        return secao;
    }
    
    private void criarLabel(GridPane grid, String texto, int coluna, int linha) {
        Label label = new Label(texto);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        grid.add(label, coluna, linha);
    }
    
    private void criarValor(GridPane grid, String texto, int coluna, int linha) {
        Label valor = new Label(texto);
        valor.setFont(Font.font("System", 12));
        grid.add(valor, coluna, linha);
    }
    
    private VBox criarGraficoVeiculos() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");
        
        Text titulo = new Text("Distribuição de Veículos");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Criar gráfico de pizza
        PieChart grafico = new PieChart();
        
        // Adicionar dados
        int chegados = estatisticas.getVehiclesArrived();
        int naoChegados = estatisticas.getTotalVehiclesGenerated() - chegados;
        
        PieChart.Data chegadosData = new PieChart.Data("Chegaram ao Destino", chegados);
        PieChart.Data naoChegadosData = new PieChart.Data("Não Chegaram", naoChegados);
        
        grafico.getData().addAll(chegadosData, naoChegadosData);
        grafico.setLabelsVisible(true);
        grafico.setLegendVisible(true);
        
        container.getChildren().addAll(titulo, grafico);
        VBox.setVgrow(grafico, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox criarGraficoTempos() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");
        
        Text titulo = new Text("Tempos de Viagem vs. Espera");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Criar gráfico de barras
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Métrica");
        yAxis.setLabel("Tempo (segundos)");
        
        BarChart<String, Number> grafico = new BarChart<>(xAxis, yAxis);
        grafico.setTitle("Comparação de Tempos");
        
        // Adicionar dados
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tempos");
        series.getData().add(new XYChart.Data<>("Tempo Médio Viagem", estatisticas.getAverageTravelTime()));
        series.getData().add(new XYChart.Data<>("Tempo Máximo Viagem", estatisticas.getMaxTravelTime()));
        series.getData().add(new XYChart.Data<>("Tempo Médio Espera", estatisticas.getAverageWaitTime()));
        series.getData().add(new XYChart.Data<>("Tempo Máximo Espera", estatisticas.getMaxWaitTime()));
        
        grafico.getData().add(series);
        
        container.getChildren().addAll(titulo, grafico);
        VBox.setVgrow(grafico, Priority.ALWAYS);
        
        return container;
    }
    
    private ScrollPane criarPainelGraficos() {
        VBox conteudo = new VBox(20);
        conteudo.setPadding(new Insets(20));
        
        // Gráfico de congestionamento ao longo do tempo
        VBox congestionamentoBox = criarGraficoCongestionamento();
        
        // Gráfico de veículos ativos ao longo do tempo
        VBox veiculosAtivosBox = criarGraficoVeiculosAtivos();
        
        // Gráfico de consumo de combustível
        VBox combustivelBox = criarGraficoCombustivel();
        
        // Gráfico de tempo de espera
        VBox esperaBox = criarGraficoEspera();
        
        conteudo.getChildren().addAll(congestionamentoBox, veiculosAtivosBox, combustivelBox, esperaBox);
        
        // Criar ScrollPane para conteúdo
        ScrollPane scrollPane = new ScrollPane(conteudo);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }
    
    private VBox criarGraficoCongestionamento() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Text titulo = new Text("Nível de Congestionamento ao Longo do Tempo");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Obter dados
        List<Double> tempos = estatisticas.getTimeHistory();
        List<Double> congestionamento = estatisticas.getCongestionHistory();
        
        // Verificar se há dados suficientes
        if (tempos.isEmpty() || congestionamento.isEmpty()) {
            Text semDados = new Text("Não há dados suficientes para exibir o gráfico.");
            semDados.setFill(Color.RED);
            container.getChildren().addAll(titulo, semDados);
            return container;
        }
        
        
        // Criar gráfico
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis(0, 100, 10);
        xAxis.setLabel("Tempo (segundos)");
        yAxis.setLabel("Congestionamento (%)");
        
        AreaChart<Number, Number> grafico = new AreaChart<>(xAxis, yAxis);
        grafico.setTitle("Evolução do Congestionamento");
        grafico.setCreateSymbols(false);
        
        // Adicionar dados
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Nível de Congestionamento");
        
        for (int i = 0; i < tempos.size() && i < congestionamento.size(); i++) {
            series.getData().add(new XYChart.Data<>(tempos.get(i), congestionamento.get(i)));
        }
        
        grafico.getData().add(series);
        
        container.getChildren().addAll(titulo, grafico);
        VBox.setVgrow(grafico, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox criarGraficoVeiculosAtivos() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Text titulo = new Text("Veículos Ativos ao Longo do Tempo");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Obter dados
        List<Double> tempos = estatisticas.getTimeHistory();
        List<Integer> veiculos = estatisticas.getActiveVehiclesHistory();
        
        // Verificar se há dados suficientes
        if (tempos.isEmpty() || veiculos.isEmpty()) {
            Text semDados = new Text("Não há dados suficientes para exibir o gráfico.");
            semDados.setFill(Color.RED);
            container.getChildren().addAll(titulo, semDados);
            return container;
        }
        
        // Log para debug
        
        // Criar gráfico
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tempo (segundos)");
        yAxis.setLabel("Número de Veículos");
        
        LineChart<Number, Number> grafico = new LineChart<>(xAxis, yAxis);
        grafico.setTitle("Veículos Ativos na Simulação");
        grafico.setCreateSymbols(false);
        
        // Adicionar dados
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Veículos Ativos");
        
        for (int i = 0; i < tempos.size() && i < veiculos.size(); i++) {
            series.getData().add(new XYChart.Data<>(tempos.get(i), veiculos.get(i)));
        }
        
        grafico.getData().add(series);
        
        container.getChildren().addAll(titulo, grafico);
        VBox.setVgrow(grafico, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox criarGraficoCombustivel() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Text titulo = new Text("Consumo de Combustível ao Longo do Tempo");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Obter dados
        List<Double> tempos = estatisticas.getTimeHistory();
        List<Double> combustivel = estatisticas.getFuelConsumptionHistory();
        
        // Verificar se há dados suficientes
        if (tempos.isEmpty() || combustivel.isEmpty()) {
            Text semDados = new Text("Não há dados suficientes para exibir o gráfico.");
            semDados.setFill(Color.RED);
            container.getChildren().addAll(titulo, semDados);
            return container;
        }
        
        // Log para debug
        
        // Criar gráfico
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tempo (segundos)");
        yAxis.setLabel("Combustível (litros)");
        
        AreaChart<Number, Number> grafico = new AreaChart<>(xAxis, yAxis);
        grafico.setTitle("Consumo Acumulado de Combustível");
        grafico.setCreateSymbols(false);
        
        // Adicionar dados
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Combustível Consumido");
        
        for (int i = 0; i < tempos.size() && i < combustivel.size(); i++) {
            series.getData().add(new XYChart.Data<>(tempos.get(i), combustivel.get(i)));
        }
        
        grafico.getData().add(series);
        
        container.getChildren().addAll(titulo, grafico);
        VBox.setVgrow(grafico, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox criarGraficoEspera() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Text titulo = new Text("Tempo Médio de Espera ao Longo do Tempo");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Obter dados
        List<Double> tempos = estatisticas.getTimeHistory();
        List<Double> esperas = estatisticas.getWaitTimeHistory();
        
        // Verificar se há dados suficientes
        if (tempos.isEmpty() || esperas.isEmpty()) {
            Text semDados = new Text("Não há dados suficientes para exibir o gráfico.");
            semDados.setFill(Color.RED);
            container.getChildren().addAll(titulo, semDados);
            return container;
        }
        
        // Log para debug
        
        // Criar gráfico
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tempo (segundos)");
        yAxis.setLabel("Tempo de Espera (segundos)");
        
        LineChart<Number, Number> grafico = new LineChart<>(xAxis, yAxis);
        grafico.setTitle("Evolução do Tempo Médio de Espera");
        grafico.setCreateSymbols(false);
        
        // Adicionar dados
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Tempo Médio de Espera");
        
        for (int i = 0; i < tempos.size() && i < esperas.size(); i++) {
            series.getData().add(new XYChart.Data<>(tempos.get(i), esperas.get(i)));
        }
        
        grafico.getData().add(series);
        
        container.getChildren().addAll(titulo, grafico);
        VBox.setVgrow(grafico, Priority.ALWAYS);
        
        return container;
    }
    
    private HBox criarRodape() {
        HBox footer = new HBox(15);
        footer.setPadding(new Insets(15));
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        Button exportarButton = new Button("Exportar Relatório");
        exportarButton.setStyle("-fx-background-color: " + SECONDARY_COLOR + "; -fx-text-fill: white;");
        exportarButton.setOnAction(e -> exportarRelatorio());
        
        Button fecharButton = new Button("Fechar");
        fecharButton.setOnAction(e -> stage.close());
        
        footer.getChildren().addAll(exportarButton, fecharButton);
        
        return footer;
    }
    
    private void exportarRelatorio() {
        try {
            // Criar FileChooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar Relatório da Simulação");
            
            // Configurar filtro de extensão
            fileChooser.getExtensionFilters().add(
                new ExtensionFilter("Arquivos de Texto", "*.txt")
            );
            
            // Definir nome inicial do arquivo
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            fileChooser.setInitialFileName("simulacao_relatorio_" + timestamp + ".txt");
            
            // Mostrar diálogo de salvamento
            File arquivoSelecionado = fileChooser.showSaveDialog(stage);
            
            // Se o usuário cancelou a seleção, retornar
            if (arquivoSelecionado == null) {
                return;
            }
            
            // Garantir que o arquivo tenha a extensão .txt
            final File arquivo = arquivoSelecionado.getName().toLowerCase().endsWith(".txt") 
                ? arquivoSelecionado 
                : new File(arquivoSelecionado.getAbsolutePath() + ".txt");
            
            FileWriter writer = new FileWriter(arquivo);
            
            // Escrever cabeçalho
            writer.write("=================================================================\n");
            writer.write("                RELATÓRIO DE SIMULAÇÃO - SEMAFLUX                \n");
            writer.write("=================================================================\n\n");
            writer.write("Data/Hora: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + "\n\n");
            
            // Escrever estatísticas gerais
            writer.write("ESTATÍSTICAS GERAIS\n");
            writer.write("-----------------------------------------------------------------\n");
            writer.write(String.format("Tempo Total de Simulação: %.2f segundos\n", estatisticas.getCurrentTime()));
            writer.write(String.format("Total de Veículos Gerados: %d\n", estatisticas.getTotalVehiclesGenerated()));
            writer.write(String.format("Veículos Chegados ao Destino: %d\n", estatisticas.getVehiclesArrived()));
            writer.write(String.format("Taxa de Chegada: %.2f%%\n\n", estatisticas.getArrivalRate()));
            
            // Escrever tempos
            writer.write("MÉTRICAS DE TEMPO\n");
            writer.write("-----------------------------------------------------------------\n");
            writer.write(String.format("Tempo Médio de Viagem: %.2f segundos\n", estatisticas.getAverageTravelTime()));
            writer.write(String.format("Tempo Máximo de Viagem: %.2f segundos\n", estatisticas.getMaxTravelTime()));
            writer.write(String.format("Tempo Médio de Espera: %.2f segundos\n", estatisticas.getAverageWaitTime()));
            writer.write(String.format("Tempo Máximo de Espera: %.2f segundos\n\n", estatisticas.getMaxWaitTime()));
            
            // Escrever combustível e congestionamento
            writer.write("COMBUSTÍVEL E CONGESTIONAMENTO\n");
            writer.write("-----------------------------------------------------------------\n");
            writer.write(String.format("Consumo Total de Combustível: %.3f litros\n", estatisticas.getTotalFuelConsumed()));
            writer.write(String.format("Consumo Médio por Veículo: %.3f litros\n", estatisticas.getAverageFuelConsumptionPerVehicle()));
            writer.write(String.format("Pico de Congestionamento: %.2f%%\n", estatisticas.getMaxRecordedCongestionRatio()));
            writer.write(String.format("Média de Congestionamento: %.2f%%\n\n", estatisticas.getAverageCongestionRatio()));
            
            // Incluir dados de série temporal se houver muitos pontos
            List<Double> tempos = estatisticas.getTimeHistory();
            List<Double> congestionamento = estatisticas.getCongestionHistory();
            
            if (tempos.size() > 5) {
                writer.write("DADOS DE CONGESTIONAMENTO AO LONGO DO TEMPO (AMOSTRA)\n");
                writer.write("-----------------------------------------------------------------\n");
                writer.write("Tempo (s) | Congestionamento (%)\n");
                
                // Escrever apenas alguns pontos representativos
                int step = Math.max(1, tempos.size() / 20); // No máximo 20 pontos
                for (int i = 0; i < tempos.size() && i < congestionamento.size(); i += step) {
                    writer.write(String.format("%.2f | %.2f\n", tempos.get(i), congestionamento.get(i)));
                }
            }
            
            writer.write("\n=================================================================\n");
            writer.write("                          FIM DO RELATÓRIO                      \n");
            writer.write("=================================================================\n");
            
            writer.close();
            
            Platform.runLater(() -> {
                Label confirmacao = new Label("Relatório exportado com sucesso: " + arquivo.getName());
                confirmacao.setStyle("-fx-text-fill: green;");
                if (stage.getScene().getRoot() instanceof BorderPane) {
                    BorderPane root = (BorderPane) stage.getScene().getRoot();
                    if (root.getBottom() instanceof HBox) {
                        HBox footer = (HBox) root.getBottom();
                        if (!footer.getChildren().contains(confirmacao)) {
                            footer.getChildren().add(0, confirmacao);
                        }
                    }
                }
            });
            
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                Label erro = new Label("Erro ao exportar relatório: " + e.getMessage());
                erro.setStyle("-fx-text-fill: red;");
                if (stage.getScene().getRoot() instanceof BorderPane) {
                    BorderPane root = (BorderPane) stage.getScene().getRoot();
                    if (root.getBottom() instanceof HBox) {
                        HBox footer = (HBox) root.getBottom();
                        if (footer.getChildren().get(0) instanceof Label) {
                            footer.getChildren().remove(0);
                        }
                        footer.getChildren().add(0, erro);
                    }
                }
            });
        }
    }
} 