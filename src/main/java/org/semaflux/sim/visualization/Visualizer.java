package org.semaflux.sim.visualization;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Separator;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import org.semaflux.sim.core.Aresta;
import org.semaflux.sim.core.FaseDoSemaforo;
import org.semaflux.sim.core.Grafo;
import org.semaflux.sim.core.ListaLigada;
import org.semaflux.sim.core.No;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.core.Veiculo;
import org.semaflux.sim.simulação.Estatisticas;
import org.semaflux.sim.simulação.Simulador;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.text.DecimalFormat;

/**
 * Classe responsável por visualizar o grafo e a simulação usando JavaFX.
 */
public class Visualizer {

    private static final double LARGURA_TELA = 1200;
    private static final double ALTURA_TELA = 800;
    private static final double MARGEM_TELA = 80;
    private static final double ANGULO_ROTACAO_GRAUS = 0;
    // Constante para controlar a suavidade da animação
    private static final double ANIMATION_SMOOTHNESS = 0.12;
    // Taxa de atualização para movimento mais fluido
    private static final int UPDATE_INTERVAL_MS = 25;

    private double minLat, maxLat, minLon, maxLon;
    private double centroLat, centroLon;
    private double escalaX, escalaY;
    private boolean transformacaoCalculada = false;
    private Label statsLabel;
    private Label timeLabel;
    private Label vehiclesLabel;
    private Label congestionLabel;
    private Label waitTimeLabel;
    private Label speedIndicatorLabel;
    private Grafo graph;
    private Simulador simulator;
    private XYChart.Series<Number, Number> congestionSeries;
    private LineChart<Number, Number> congestionChart;
    private ConcurrentLinkedQueue<Double> congestionDataPoints = new ConcurrentLinkedQueue<>();

    private Group pane;
    private Group mapGroup;
    private Pane mapPane;
    private Map<String, Group> trafficLightNodeVisuals;
    private Map<String, Circle> regularNodeVisuals;
    private Map<String, Circle> vehicleVisuals;
    // Mapa para armazenar as posições anteriores dos veículos para interpolação
    private Map<String, Point2D> previousVehiclePositions;
    // Mapa para armazenar as posições alvo dos veículos para interpolação
    private Map<String, Point2D> targetVehiclePositions;
    private double zoomFactor = 1.0;
    private double dragStartX, dragStartY;
    private double translateX = 0, translateY = 0;

    private volatile boolean running = true;
    private DecimalFormat df = new DecimalFormat("#,##0.0");

    // Tema de cores
    private final String PRIMARY_COLOR = "#2b6cb0";
    private final String SECONDARY_COLOR = "#3182ce";
    private final String ACCENT_COLOR = "#4299e1";
    private final String BACKGROUND_COLOR = "#f0f4f8";
    private final String PANEL_COLOR = "#ffffff";
    private final String TEXT_COLOR = "#2d3748";

    // Classe interna para representar o visual de um semáforo
    private static class TrafficLightDisplay {
        // Traço vertical (Norte-Sul)
        Line nsLine;
        
        // Traço horizontal (Leste-Oeste)
        Line ewLine;

        TrafficLightDisplay(Line nsLine, Line ewLine) {
            this.nsLine = nsLine;
            this.ewLine = ewLine;
        }
    }
    private Map<String, TrafficLightDisplay> lightVisualsMap;

    public Visualizer(Grafo graph, Simulador simulator) {
        this.graph = graph;
        this.simulator = simulator;
        this.trafficLightNodeVisuals = new HashMap<>();
        this.regularNodeVisuals = new HashMap<>();
        this.lightVisualsMap = new HashMap<>();
        this.vehicleVisuals = new HashMap<>();
        this.previousVehiclePositions = new HashMap<>();
        this.targetVehiclePositions = new HashMap<>();
    }

    public Visualizer() {
        this.trafficLightNodeVisuals = new HashMap<>();
        this.regularNodeVisuals = new HashMap<>();
        this.lightVisualsMap = new HashMap<>();
        this.vehicleVisuals = new HashMap<>();
        this.previousVehiclePositions = new HashMap<>();
        this.targetVehiclePositions = new HashMap<>();
    }

    /**
     * Inicia a visualização da simulação
     * @param primaryStage O Stage onde a visualização será mostrada
     * @throws Exception se ocorrer algum erro na inicialização
     */
    public void start(Stage primaryStage) throws Exception {
        if (this.graph == null || this.simulator == null) {
            Pane errorPane = new Pane(new Text("Erro Crítico: Dados da simulação não carregados para o Visualizer."));
            Scene errorScene = new Scene(errorPane, 450, 100);
            primaryStage.setTitle("Erro de Inicialização do Visualizer");
            primaryStage.setScene(errorScene);
            primaryStage.show();
            return;
        }

        // Configuração principal do layout usando BorderPane
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");
        
        // Cabeçalho com título e informações
        VBox headerBox = createHeader();
        root.setTop(headerBox);
        
        // Painel do mapa com controles de zoom
        mapGroup = new Group(); // Grupo para aplicar zoom
        mapPane = new Pane();
        mapPane.getChildren().add(mapGroup);
        mapPane.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #AAAAAA; -fx-border-width: 1px;");
        
        // Adicionar sombra ao mapa
        DropShadow mapShadow = new DropShadow();
        mapShadow.setRadius(5.0);
        mapShadow.setOffsetX(3.0);
        mapShadow.setOffsetY(3.0);
        mapShadow.setColor(Color.rgb(150, 150, 150, 0.5));
        mapPane.setEffect(mapShadow);
        
        // Configurar área do mapa
        mapPane.setPrefSize(LARGURA_TELA - 300, ALTURA_TELA - 200);
        mapPane.setMinSize(600, 400);
        
        // Configurar painel lateral de estatísticas e controles
        VBox sidePanel = createSidePanel();
        
        // Configurar painel principal central com mapa e estatísticas lado a lado
        HBox centerPanel = new HBox(15);
        centerPanel.setPadding(new Insets(10, 0, 10, 0));
        centerPanel.getChildren().addAll(mapPane, sidePanel);
        HBox.setHgrow(mapPane, Priority.ALWAYS);
        
        root.setCenter(centerPanel);
        
        // Rodapé com controles de zoom e navegação
        HBox footerBox = createFooter();
        root.setBottom(footerBox);
        
        // Configurar zoom e pan do mapa
        setupMapInteraction();
        
        this.pane = mapGroup;

        calcularParametrosDeTransformacao();
        desenharElementosEstaticos();

        // Configurar a velocidade inicial da simulação
        if (simulator != null) {
            simulator.setSpeedFactor(1.0); // Velocidade normal como padrão
        }
        
        Scene scene = new Scene(root, LARGURA_TELA, ALTURA_TELA);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulador de Mobilidade Urbana");
        primaryStage.show();
        
        // Atualizar o título com a velocidade inicial
        updateSpeedLabel();

        primaryStage.setOnCloseRequest(event -> {
            running = false;
            if (simulator != null) {
                simulator.stopSimulation();
            }
        });

        Thread updateThread = new Thread(() -> {
            while (running) {
                try {
                    // Intervalo de atualização menor para animações mais suaves
                    Thread.sleep(UPDATE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    if (!running) break;
                    System.err.println("Visualizer: update thread interrompida: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
                if (running) {
                    Platform.runLater(this::atualizarElementosDinamicos);
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.setName("VisualizerUpdateThread");
        updateThread.start();
    }
    
    private VBox createHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 10, 0));
        
        // Título
        Text title = new Text("Simulador de Mobilidade Urbana");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setFill(Color.web(PRIMARY_COLOR));
        
        // Adicionar sombra ao título
        DropShadow shadow = new DropShadow();
        shadow.setRadius(2.0);
        shadow.setOffsetX(1.0);
        shadow.setOffsetY(1.0);
        shadow.setColor(Color.gray(0.4, 0.3));
        title.setEffect(shadow);
        
        // Barra de informações da simulação
        HBox infoBar = new HBox(20);
        infoBar.setAlignment(Pos.CENTER);
        infoBar.setPadding(new Insets(5, 10, 5, 10));
        infoBar.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");
        
        // Tempo da simulação
        timeLabel = new Label("Tempo: 0.0s");
        timeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        timeLabel.setTextFill(Color.web(PRIMARY_COLOR));
        
        // Veículos ativos
        vehiclesLabel = new Label("Veículos: 0");
        vehiclesLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        vehiclesLabel.setTextFill(Color.web(SECONDARY_COLOR));
        
        // Congestionamento
        congestionLabel = new Label("Congestionamento: 0%");
        congestionLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        congestionLabel.setTextFill(Color.web(ACCENT_COLOR));
        
        // Tempo médio de espera
        waitTimeLabel = new Label("Tempo Médio de Espera: 0.0s");
        waitTimeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        waitTimeLabel.setTextFill(Color.web(TEXT_COLOR));
        
        // Indicador de velocidade da simulação
        speedIndicatorLabel = new Label("Velocidade: 1.0x");
        speedIndicatorLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        speedIndicatorLabel.setTextFill(Color.web(ACCENT_COLOR));
        
        infoBar.getChildren().addAll(timeLabel, vehiclesLabel, congestionLabel, waitTimeLabel, speedIndicatorLabel);
        
        header.getChildren().addAll(title, infoBar);
        return header;
    }
    
    private VBox createSidePanel() {
        VBox sidePanel = new VBox(15);
        sidePanel.setPadding(new Insets(10));
        sidePanel.setMinWidth(280);
        sidePanel.setMaxWidth(280);
        sidePanel.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");
        
        // Título do painel
        Text panelTitle = new Text("Estatísticas da Simulação");
        panelTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        panelTitle.setFill(Color.web(PRIMARY_COLOR));
        
        // Gráfico de congestionamento
        createCongestionChart();
        
        // Área de estatísticas detalhadas
        VBox statsBox = new VBox(8);
        statsBox.setPadding(new Insets(10));
        statsBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");
        
        statsLabel = new Label("");
        statsLabel.setFont(Font.font("System", 12));
        statsLabel.setWrapText(true);
        
        statsBox.getChildren().add(statsLabel);
        
        // Legenda de cores
        VBox legendBox = createLegend();
        
        sidePanel.getChildren().addAll(panelTitle, congestionChart, statsBox, new Separator(), legendBox);
        
        return sidePanel;
    }
    
    private void createCongestionChart() {
        // Criar eixos
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis(0, 100, 10);
        xAxis.setLabel("Tempo (s)");
        yAxis.setLabel("Congestionamento (%)");
        
        // Configurações adicionais para os eixos
        xAxis.setAutoRanging(true);  // Ajusta automaticamente o range do eixo X conforme dados são adicionados
        xAxis.setForceZeroInRange(false);  // Não força começar em zero
        yAxis.setAutoRanging(false);  // Mantém o range fixo para o eixo Y (0-100%)
        yAxis.setTickUnit(10);  // Linhas de grade a cada 10%
        
        // Criar gráfico
        congestionChart = new LineChart<>(xAxis, yAxis);
        congestionChart.setTitle("Nível de Congestionamento");
        congestionChart.setCreateSymbols(false);
        congestionChart.setAnimated(false);
        congestionChart.setLegendVisible(false);
        congestionChart.setPrefHeight(200);
        
        // Personalização adicional do gráfico
        congestionChart.setStyle("-fx-background-color: " + PANEL_COLOR + ";");
        congestionChart.lookup(".chart-plot-background").setStyle("-fx-background-color: #f8f9fa;");
        
        // Criar série de dados
        congestionSeries = new XYChart.Series<>();
        congestionSeries.setName("Congestionamento");
        
        // Adicionar série ao gráfico
        congestionChart.getData().add(congestionSeries);
    }
    
    private VBox createLegend() {
        VBox legend = new VBox(8);
        legend.setPadding(new Insets(10, 5, 5, 5));
        
        Text legendTitle = new Text("Legenda");
        legendTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        HBox semaforos = new HBox(10);
        Circle greenLight = new Circle(6, Color.LIMEGREEN);
        Circle yellowLight = new Circle(6, Color.YELLOW);
        Circle redLight = new Circle(6, Color.RED);
        
        semaforos.getChildren().addAll(
            greenLight, new Label("Verde"),
            yellowLight, new Label("Amarelo"),
            redLight, new Label("Vermelho")
        );
        
        HBox elementos = new HBox(10);
        Circle vehicleCircle = new Circle(6, Color.DEEPSKYBLUE);
        Circle nodeCircle = new Circle(6, Color.ROYALBLUE);
        
        elementos.getChildren().addAll(
            vehicleCircle, new Label("Veículo"),
            nodeCircle, new Label("Cruzamento")
        );
        
        legend.getChildren().addAll(legendTitle, semaforos, elementos);
        return legend;
    }
    
    private HBox createFooter() {
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10, 0, 0, 0));
        
        // Controles de zoom
        Label zoomLabel = new Label("Zoom:");
        zoomLabel.setTextFill(Color.web(TEXT_COLOR));
        
        Button zoomOutButton = new Button("-");
        zoomOutButton.setOnAction(e -> {
            zoomFactor = Math.max(0.5, zoomFactor - 0.1);
            applyZoom();
        });
        
        Slider zoomSlider = new Slider(0.5, 2.0, 1.0);
        zoomSlider.setPrefWidth(150);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setMajorTickUnit(0.5);
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            zoomFactor = newVal.doubleValue();
            applyZoom();
        });
        
        Button zoomInButton = new Button("+");
        zoomInButton.setOnAction(e -> {
            zoomFactor = Math.min(2.0, zoomFactor + 0.1);
            applyZoom();
        });
        
        Button resetViewButton = new Button("Centralizar Mapa");
        resetViewButton.setStyle("-fx-background-color: " + SECONDARY_COLOR + "; -fx-text-fill: white;");
        resetViewButton.setOnAction(e -> {
            resetView();
        });
        
        // Controles de velocidade da simulação
        Separator separator = new Separator(javafx.geometry.Orientation.VERTICAL);
        separator.setPadding(new Insets(0, 10, 0, 10));
        
        Label speedLabel = new Label("Velocidade:");
        speedLabel.setTextFill(Color.web(TEXT_COLOR));
        
        Button slowDownButton = new Button("0.5x");
        slowDownButton.setOnAction(e -> {
            if (simulator != null) {
                simulator.setSpeedFactor(0.5);
                updateSpeedLabel();
            }
        });
        
        Button normalSpeedButton = new Button("1x");
        normalSpeedButton.setStyle("-fx-background-color: " + SECONDARY_COLOR + "; -fx-text-fill: white;");
        normalSpeedButton.setOnAction(e -> {
            if (simulator != null) {
                simulator.setSpeedFactor(1.0);
                updateSpeedLabel();
            }
        });
        
        Button speedUpButton = new Button("2x");
        speedUpButton.setOnAction(e -> {
            if (simulator != null) {
                simulator.setSpeedFactor(2.0);
                updateSpeedLabel();
            }
        });
        
        Button turboButton = new Button("5x");
        turboButton.setOnAction(e -> {
            if (simulator != null) {
                simulator.setSpeedFactor(5.0);
                updateSpeedLabel();
            }
        });
        
        footer.getChildren().addAll(
            zoomLabel, zoomOutButton, zoomSlider, zoomInButton, resetViewButton,
            separator,
            speedLabel, slowDownButton, normalSpeedButton, speedUpButton, turboButton
        );
        
        return footer;
    }
    
    private void updateSpeedLabel() {
        if (simulator != null) {
            double speed = simulator.getSpeedFactor();
            String speedText = String.format("%.1fx", speed);
            
            // Atualizar o indicador de velocidade na interface
            if (speedIndicatorLabel != null) {
                Platform.runLater(() -> {
                    speedIndicatorLabel.setText("Velocidade: " + speedText);
                    
                    // Ajustar a cor baseado na velocidade
                    if (speed > 3.0) {
                        speedIndicatorLabel.setTextFill(Color.RED);
                    } else if (speed > 1.5) {
                        speedIndicatorLabel.setTextFill(Color.ORANGE);
                    } else if (speed < 1.0) {
                        speedIndicatorLabel.setTextFill(Color.GREEN);
                    } else {
                        speedIndicatorLabel.setTextFill(Color.web(ACCENT_COLOR));
                    }
                });
            }
            
            // Atualizar o título da janela para mostrar a velocidade atual
            if (pane != null && pane.getScene() != null && 
                pane.getScene().getWindow() instanceof Stage) {
                Stage stage = (Stage) pane.getScene().getWindow();
                Platform.runLater(() -> {
                    stage.setTitle("Simulador de Mobilidade Urbana - Velocidade: " + speedText);
                });
            }
        }
    }
    
    private void setupMapInteraction() {
        // Configurar eventos de mouse para arrastar o mapa
        mapPane.setOnMousePressed(event -> {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        });
        
        mapPane.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - dragStartX;
            double deltaY = event.getSceneY() - dragStartY;
            
            translateX += deltaX;
            translateY += deltaY;
            
            mapGroup.setTranslateX(translateX);
            mapGroup.setTranslateY(translateY);
            
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        });
        
        // Configurar evento de scroll para zoom
        mapPane.setOnScroll(event -> {
            double zoomDelta = event.getDeltaY() > 0 ? 0.1 : -0.1;
            zoomFactor = Math.max(0.5, Math.min(2.0, zoomFactor + zoomDelta));
            applyZoom();
        });
    }
    
    private void applyZoom() {
        mapGroup.setScaleX(zoomFactor);
        mapGroup.setScaleY(zoomFactor);
    }
    
    private void resetView() {
        zoomFactor = 1.0;
        translateX = 0;
        translateY = 0;
        mapGroup.setScaleX(1.0);
        mapGroup.setScaleY(1.0);
        mapGroup.setTranslateX(0);
        mapGroup.setTranslateY(0);
    }

    private void calcularParametrosDeTransformacao() {
        if (graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("Visualizer: Nenhum nó no grafo para calcular transformação. Usando defaults.");
            minLat = -5.12; maxLat = -5.06; minLon = -42.84; maxLon = -42.78;
        } else {
            minLat = Double.MAX_VALUE; maxLat = -Double.MAX_VALUE;
            minLon = Double.MAX_VALUE; maxLon = -Double.MAX_VALUE;
            for (No node : graph.getNodes()) {
                if (node == null) continue;
                if (node.getLatitude() < minLat) minLat = node.getLatitude();
                if (node.getLatitude() > maxLat) maxLat = node.getLatitude();
                if (node.getLongitude() < minLon) minLon = node.getLongitude();
                if (node.getLongitude() > maxLon) maxLon = node.getLongitude();
            }
        }
        if (Math.abs(maxLat - minLat) < 0.00001) { maxLat = minLat + 0.001; minLat = minLat - 0.001;}
        if (Math.abs(maxLon - minLon) < 0.00001) { maxLon = minLon + 0.001; minLon = minLon - 0.001;}

        centroLat = (minLat + maxLat) / 2.0;
        centroLon = (minLon + maxLon) / 2.0;
        double deltaLonGeo = maxLon - minLon;
        double deltaLatGeo = maxLat - minLat;
        
        // Aumentar área visível para centralizar melhor
        double larguraDesenhoUtil = mapPane.getPrefWidth() - 2 * MARGEM_TELA;
        double alturaDesenhoUtil = mapPane.getPrefHeight() - 2 * MARGEM_TELA;
        
        double escalaPotencialX = (deltaLonGeo == 0) ? larguraDesenhoUtil : larguraDesenhoUtil / deltaLonGeo;
        double escalaPotencialY = (deltaLatGeo == 0) ? alturaDesenhoUtil : alturaDesenhoUtil / deltaLatGeo;
        escalaX = Math.min(escalaPotencialX, escalaPotencialY) * 1.2; // 20% maior para centralizar
        escalaY = escalaX;
        transformacaoCalculada = true;
    }

    private Point2D transformarCoordenadas(double latGeo, double lonGeo) {
        if (!transformacaoCalculada) {
            calcularParametrosDeTransformacao();
            if(!transformacaoCalculada) {
                return new Point2D(mapPane.getPrefWidth() / 2, mapPane.getPrefHeight() / 2);
            }
        }
        double lonRel = lonGeo - centroLon;
        double latRel = latGeo - centroLat;
        double xTela = lonRel * escalaX + mapPane.getPrefWidth() / 2;
        double yTela = latRel * (-escalaY) + mapPane.getPrefHeight() / 2;
        return new Point2D(xTela, yTela);
    }

    private void desenharElementosEstaticos() {
        pane.getChildren().clear();
        trafficLightNodeVisuals.clear();
        regularNodeVisuals.clear();
        lightVisualsMap.clear();

        // Desenhar linhas da grade (estradas)
        if (graph.getEdges() != null) {
            for (Aresta edge : graph.getEdges()) {
                if (edge == null) continue;
                No sourceNode = graph.getNode(edge.getSource());
                No targetNode = graph.getNode(edge.getDestination());
                if (sourceNode != null && targetNode != null) {
                    Point2D p1 = transformarCoordenadas(sourceNode.getLatitude(), sourceNode.getLongitude());
                    Point2D p2 = transformarCoordenadas(targetNode.getLatitude(), targetNode.getLongitude());
                    Line line = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    line.setStroke(Color.rgb(120, 120, 120, 0.8));
                    line.setStrokeWidth(2.0);
                    pane.getChildren().add(line);
                }
            }
        }

        // Desenhar nós e semáforos
        if (graph.getNodes() != null) {
            for (No node : graph.getNodes()) {
                if (node == null) continue;
                Point2D p = transformarCoordenadas(node.getLatitude(), node.getLongitude());
                SinalTransito tl = findTrafficLight(node.getId());

                if (tl != null) {
                    Group trafficLightGroup = new Group(); // Agrupa todos os elementos do semáforo

                    // Indicador do cruzamento
                    Circle baseCircle = new Circle(p.getX(), p.getY(), 5, Color.DARKSLATEGRAY);
                    baseCircle.setStroke(Color.BLACK);
                    baseCircle.setStrokeWidth(0.5);
                    
                    // Adicionar efeito de sombra ao cruzamento
                    DropShadow baseCircleShadow = new DropShadow();
                    baseCircleShadow.setRadius(2.0);
                    baseCircleShadow.setOffsetX(1.0);
                    baseCircleShadow.setOffsetY(1.0);
                    baseCircleShadow.setColor(Color.rgb(0, 0, 0, 0.5));
                    baseCircle.setEffect(baseCircleShadow);
                    
                    trafficLightGroup.getChildren().add(baseCircle);

                    // Dimensões e propriedades dos traços
                    double lineLength = 30.0;
                    double lineWidth = 4.0;
                    
                    // Traço Norte-Sul (vertical)
                    Line nsLine = new Line(
                            p.getX(), p.getY() - lineLength, // ponto inicial
                            p.getX(), p.getY() - 5 // ponto final (até o círculo)
                    );
                    nsLine.setStrokeWidth(lineWidth);
                    nsLine.setStroke(Color.DARKRED); // Cor inicial (vermelho)
                    
                    // Efeito de brilho para o traço
                    DropShadow nsLineShadow = new DropShadow();
                    nsLineShadow.setRadius(5.0);
                    nsLineShadow.setColor(Color.rgb(255, 0, 0, 0.5));
                    nsLine.setEffect(nsLineShadow);
                    
                    // Traço Leste-Oeste (horizontal)
                    Line ewLine = new Line(
                            p.getX() + 5, p.getY(), // ponto inicial (a partir do círculo)
                            p.getX() + lineLength, p.getY() // ponto final
                    );
                    ewLine.setStrokeWidth(lineWidth);
                    ewLine.setStroke(Color.DARKRED); // Cor inicial (vermelho)
                    
                    // Efeito de brilho para o traço
                    DropShadow ewLineShadow = new DropShadow();
                    ewLineShadow.setRadius(5.0);
                    ewLineShadow.setColor(Color.rgb(255, 0, 0, 0.5));
                    ewLine.setEffect(ewLineShadow);
                    
                    // Adiciona todos os elementos ao grupo
                    trafficLightGroup.getChildren().addAll(nsLine, ewLine);
                    
                    pane.getChildren().add(trafficLightGroup);

                    // Armazenar os componentes para atualização
                    lightVisualsMap.put(node.getId(), new TrafficLightDisplay(nsLine, ewLine));
                    trafficLightNodeVisuals.put(node.getId(), trafficLightGroup);
                } else {
                    Circle nodeCircle = new Circle(p.getX(), p.getY(), 3, Color.ROYALBLUE);
                    nodeCircle.setStroke(Color.NAVY);
                    nodeCircle.setStrokeWidth(0.5);
                    regularNodeVisuals.put(node.getId(), nodeCircle);
                    pane.getChildren().add(nodeCircle);
                }
            }
        }
    }

    private void atualizarElementosDinamicos() {
        if (pane == null || graph == null || simulator == null || !transformacaoCalculada) return;

        // 1. Atualizar Cores dos Semáforos
        if (graph.getTrafficLights() != null) {
            for (SinalTransito tl : graph.getTrafficLights()) {
                if (tl == null) continue;
                TrafficLightDisplay display = lightVisualsMap.get(tl.getNodeId());
                if (display != null) {
                    FaseDoSemaforo phase = tl.getCurrentPhase();
                    
                    // Resetar ambos os traços para vermelho inicialmente
                    display.nsLine.setStroke(Color.DARKRED);
                    display.ewLine.setStroke(Color.DARKRED);
                    
                    // Remover efeitos anteriores
                    DropShadow nsRedShadow = new DropShadow();
                    nsRedShadow.setRadius(5.0);
                    nsRedShadow.setColor(Color.rgb(255, 0, 0, 0.5));
                    display.nsLine.setEffect(nsRedShadow);
                    
                    DropShadow ewRedShadow = new DropShadow();
                    ewRedShadow.setRadius(5.0);
                    ewRedShadow.setColor(Color.rgb(255, 0, 0, 0.5));
                    display.ewLine.setEffect(ewRedShadow);

                    // Atualizar as cores e efeitos baseado na fase atual
                    if (phase != null) {
                        switch (phase) {
                            case NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO:
                              
                                display.nsLine.setStroke(Color.LIMEGREEN);
                                
                                // Efeito de brilho para verde
                                DropShadow nsGreenShadow = new DropShadow();
                                nsGreenShadow.setRadius(8.0);
                                nsGreenShadow.setColor(Color.rgb(0, 255, 0, 0.7));
                                display.nsLine.setEffect(nsGreenShadow);
                                
                                // Leste-Oeste permanece vermelho
                                display.ewLine.setStroke(Color.RED);
                                break;
                                
                            case NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO:
                                // Norte-Sul Amarelo
                                display.nsLine.setStroke(Color.YELLOW);
                                
                                // Efeito de brilho para amarelo
                                DropShadow nsYellowShadow = new DropShadow();
                                nsYellowShadow.setRadius(8.0);
                                nsYellowShadow.setColor(Color.rgb(255, 255, 0, 0.7));
                                display.nsLine.setEffect(nsYellowShadow);
                                
                                // Leste-Oeste permanece vermelho
                                display.ewLine.setStroke(Color.RED);
                                break;
                                
                            case NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE:
                                // Norte-Sul Vermelho
                                display.nsLine.setStroke(Color.RED);
                                
                                // Leste-Oeste Verde
                                display.ewLine.setStroke(Color.LIMEGREEN);
                                
                                // Efeito de brilho para verde
                                DropShadow ewGreenShadow = new DropShadow();
                                ewGreenShadow.setRadius(8.0);
                                ewGreenShadow.setColor(Color.rgb(0, 255, 0, 0.7));
                                display.ewLine.setEffect(ewGreenShadow);
                                break;
                                
                            case NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO:
                                // Norte-Sul Vermelho
                                display.nsLine.setStroke(Color.RED);
                                
                                // Leste-Oeste Amarelo
                                display.ewLine.setStroke(Color.YELLOW);
                                
                                // Efeito de brilho para amarelo
                                DropShadow ewYellowShadow = new DropShadow();
                                ewYellowShadow.setRadius(8.0);
                                ewYellowShadow.setColor(Color.rgb(255, 255, 0, 0.7));
                                display.ewLine.setEffect(ewYellowShadow);
                                break;
                        }
                    }
                }
            }
        }

        // 2. Atualizar Posições dos Veículos
        ListaLigada<Veiculo> currentVehicles = simulator.getVehicles();
        if (currentVehicles == null) return;

        Map<String, Circle> newVehicleVisualsMap = new HashMap<>();
        Map<String, Point2D> newTargetPositions = new HashMap<>();
        List<javafx.scene.Node> childrenToAdd = new ArrayList<>();
        List<javafx.scene.Node> childrenToRemove = new ArrayList<>();

        for (Veiculo vehicle : currentVehicles) {
            if (vehicle == null || vehicle.getCurrentNode() == null) continue;

            Point2D vehicleTargetPos;
            No currentNodeObject = graph.getNode(vehicle.getCurrentNode());
            if (currentNodeObject == null) continue;

            if (vehicle.getPosition() == 0.0 || vehicle.getRoute() == null || vehicle.getRoute().isEmpty()) {
                vehicleTargetPos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
            } else {
                String nextNodeId = getNextNodeIdInRoute(vehicle, currentNodeObject.getId());
                if (nextNodeId == null) {
                    vehicleTargetPos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                } else {
                    No nextNodeObject = graph.getNode(nextNodeId);
                    if (nextNodeObject == null) {
                        vehicleTargetPos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                    } else {
                        Point2D startScreenPos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                        Point2D endScreenPos = transformarCoordenadas(nextNodeObject.getLatitude(), nextNodeObject.getLongitude());
                        double interpolatedX = startScreenPos.getX() + vehicle.getPosition() * (endScreenPos.getX() - startScreenPos.getX());
                        double interpolatedY = startScreenPos.getY() + vehicle.getPosition() * (endScreenPos.getY() - startScreenPos.getY());
                        vehicleTargetPos = new Point2D(interpolatedX, interpolatedY);
                    }
                }
            }

            // Guardar a posição alvo para este veículo
            newTargetPositions.put(vehicle.getId(), vehicleTargetPos);

            // Obter ou criar o círculo para este veículo
            Circle vehicleCircle = vehicleVisuals.get(vehicle.getId());
            if (vehicleCircle == null) {
                // Usar o mesmo tamanho e cor para todos os veículos
                double tamanho = 4.0; // Tamanho padrão para todos os veículos
                Color corVeiculo = Color.DEEPSKYBLUE; // Cor padrão para todos os veículos
                
                vehicleCircle = new Circle(tamanho, corVeiculo);
                vehicleCircle.setStroke(Color.BLACK);
                vehicleCircle.setStrokeWidth(0.8);
                
                // Melhorar o efeito visual dos veículos
                DropShadow vehicleShadow = new DropShadow();
                vehicleShadow.setRadius(3.0); // Raio fixo para todos
                vehicleShadow.setOffsetX(1.0);
                vehicleShadow.setOffsetY(1.0);
                vehicleShadow.setColor(Color.rgb(0, 0, 0, 0.5));
                
                // Adicionar um efeito de brilho sutil
                Glow glow = new Glow();
                glow.setLevel(0.3);
                
                // Combinar os efeitos
                glow.setInput(vehicleShadow);
                vehicleCircle.setEffect(glow);
                
                // Adicionar tooltip para mostrar informações sobre o veículo
                Tooltip tooltip = new Tooltip(
                    "ID: " + vehicle.getId() + "\n" +
                    "Origem: " + vehicle.getOrigin() + "\n" +
                    "Destino: " + vehicle.getDestination()
                );
                Tooltip.install(vehicleCircle, tooltip);
                
                // Definir a posição inicial
                vehicleCircle.setCenterX(vehicleTargetPos.getX());
                vehicleCircle.setCenterY(vehicleTargetPos.getY());
                previousVehiclePositions.put(vehicle.getId(), vehicleTargetPos);
                
                childrenToAdd.add(vehicleCircle);
            } else {
                // Interpolar suavemente entre a posição atual e a posição alvo
                Point2D previousPos = previousVehiclePositions.getOrDefault(vehicle.getId(), vehicleTargetPos);
                
                // Calcular a nova posição com suavização
                double newX = previousPos.getX() + (vehicleTargetPos.getX() - previousPos.getX()) * ANIMATION_SMOOTHNESS;
                double newY = previousPos.getY() + (vehicleTargetPos.getY() - previousPos.getY()) * ANIMATION_SMOOTHNESS;
                
                // Verificar se o veículo está em movimento
                boolean isMoving = !previousPos.equals(vehicleTargetPos);
                
                // Aplicar pequena vibração aleatória para veículos em movimento para efeito mais realista
                if (isMoving) {
                    // Adicionar pequena vibração aleatória
                    double vibrationAmount = 0.1;
                    newX += (Math.random() - 0.5) * vibrationAmount;
                    newY += (Math.random() - 0.5) * vibrationAmount;
                    
                    // Adicionar um efeito de rastro leve para veículos em movimento
                    double velocidade = Math.sqrt(
                        Math.pow(vehicleTargetPos.getX() - previousPos.getX(), 2) +
                        Math.pow(vehicleTargetPos.getY() - previousPos.getY(), 2)
                    );
                    
                    // Ajustar o efeito de brilho com base na velocidade
                    Glow movingGlow = new Glow();
                    double glowLevel = Math.min(0.3 + velocidade / 50, 0.6);
                    movingGlow.setLevel(glowLevel);
                    
                    DropShadow shadow = new DropShadow();
                    shadow.setRadius(3.0); // Raio fixo para todos
                    shadow.setOffsetX(1.0);
                    shadow.setOffsetY(1.0);
                    shadow.setColor(Color.rgb(0, 0, 0, 0.5));
                    
                    movingGlow.setInput(shadow);
                    vehicleCircle.setEffect(movingGlow);
                }
                
                vehicleCircle.setCenterX(newX);
                vehicleCircle.setCenterY(newY);
                
                // Atualizar a posição anterior
                previousVehiclePositions.put(vehicle.getId(), new Point2D(newX, newY));
            }
            
            newVehicleVisualsMap.put(vehicle.getId(), vehicleCircle);
        }

        // Atualizar o mapa de posições alvo
        targetVehiclePositions = newTargetPositions;

        // Remover veículos que não estão mais ativos
        for (String existingId : vehicleVisuals.keySet()) {
            if (!newVehicleVisualsMap.containsKey(existingId)) {
                childrenToRemove.add(vehicleVisuals.get(existingId));
                previousVehiclePositions.remove(existingId);
                targetVehiclePositions.remove(existingId);
            }
        }

        pane.getChildren().removeAll(childrenToRemove);
        pane.getChildren().addAll(childrenToAdd);
        vehicleVisuals = newVehicleVisualsMap;

        // 3. Atualizar Texto de Estatísticas
        if (simulator != null && simulator.getStats() != null) {
            Estatisticas currentStats = simulator.getStats();
            
            // Atualizar labels de informações
            double tempo = currentStats.getCurrentTime();
            int numVeiculos = (simulator.getVehicles() != null ? simulator.getVehicles().size() : 0);
            double congestion = currentStats.getCurrentCongestionIndex();
            double avgWaitTime = currentStats.getAverageWaitTime();
            
            timeLabel.setText("Tempo: " + df.format(tempo) + "s");
            vehiclesLabel.setText("Veículos: " + numVeiculos);
            
            // Formatação especial para o congestionamento com cores
            String congestionText = df.format(congestion) + "%";
            congestionLabel.setText("Congestionamento: " + congestionText);
            
            // Ajusta a cor do texto de congestionamento baseado no valor
            if (congestion > 75) {
                congestionLabel.setTextFill(Color.RED);
            } else if (congestion > 50) {
                congestionLabel.setTextFill(Color.ORANGE);
            } else if (congestion > 25) {
                congestionLabel.setTextFill(Color.web(ACCENT_COLOR));
            } else {
                congestionLabel.setTextFill(Color.GREEN);
            }
            
            waitTimeLabel.setText("Tempo Médio de Espera: " + df.format(avgWaitTime) + "s");
            
            // Atualizar gráfico de congestionamento
            updateCongestionChart(tempo, congestion);
            
            // Atualizar estatísticas detalhadas
            updateDetailedStats(currentStats);
        }
    }
    
    private void updateCongestionChart(double time, double congestion) {
        // Limitar o número de pontos no gráfico para melhor desempenho
        if (congestionSeries.getData().size() > 100) {
            congestionSeries.getData().remove(0);
        }
        
        // Adicionar novo ponto ao gráfico (problema: a condição estava filtrando demais)
        // A verificação anterior com % 10 == 0 impedia que a maioria dos pontos fosse adicionada
        congestionSeries.getData().add(new XYChart.Data<>(time, congestion));
        
        // Certificar que o gráfico esteja visível e atualizado
        congestionChart.setVisible(true);
    }
    
    private void updateDetailedStats(Estatisticas stats) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Veículos Gerados: ").append(stats.getTotalVehiclesGenerated()).append("\n");
        sb.append("Veículos Chegaram ao Destino: ").append(stats.getVehiclesArrived()).append("\n");
        sb.append("Tempo Médio de Viagem: ").append(df.format(stats.getAverageTravelTime())).append("s\n");
        sb.append("Tempo Máximo de Viagem: ").append(df.format(stats.getAverageTravelTime() * 1.5)).append("s\n"); // Estimativa
        sb.append("Tempo Médio de Espera: ").append(df.format(stats.getAverageWaitTime())).append("s\n");
        sb.append("Tempo Máximo de Espera: ").append(df.format(stats.getAverageWaitTime() * 1.5)).append("s\n"); // Estimativa
        sb.append("Combustível Total Consumido: ").append(df.format(stats.getTotalFuelConsumed())).append(" unidades");
        
        statsLabel.setText(sb.toString());
    }

    private String getNextNodeIdInRoute(Veiculo vehicle, String currentVehicleNodeId) {
        ListaLigada<String> route = vehicle.getRoute();
        if (route == null || route.isEmpty()) return null;

        int currentIndex = route.indexOf(currentVehicleNodeId);
        if (currentIndex != -1 && currentIndex + 1 < route.size()) {
            return route.get(currentIndex + 1);
        }
        return null;
    }

    /**
     * Procura o semáforo associado a um nó.
     * @param nodeId ID do nó
     * @return O semáforo associado ao nó, ou null se não houver
     */
    private SinalTransito findTrafficLight(String nodeId) {
        if (graph == null || graph.getTrafficLights() == null || nodeId == null) return null;
        
        for (SinalTransito trafficLight : graph.getTrafficLights()) {
            if (trafficLight != null && nodeId.equals(trafficLight.getNodeId())) {
                return trafficLight;
            }
        }
        return null;
    }
}