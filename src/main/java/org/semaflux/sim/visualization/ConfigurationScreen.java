package org.semaflux.sim.visualization;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import org.semaflux.sim.InicioSis;
import org.semaflux.sim.simulação.Config;

import java.util.Arrays;

public class ConfigurationScreen {
    private Config config;
    private Stage primaryStage;
    private InicioSis application;

    // Componente para seleção de mapa
    private ComboBox<String> mapaCombo;
    private TextField arquivoJsonField;
    private File arquivoJsonSelecionado;

    // Componentes para configuração geral
    private ComboBox<String> modoSemaforoCombo;
    private Slider taxaGeracaoVeiculosSlider;
    private CheckBox horarioPicoCheck;
    private Spinner<Double> duracaoSimulacaoSpinner;
    private Spinner<Double> pararGeracaoVeiculosSpinner;

    // Componentes para modo fixo
    private Spinner<Double> fixedGreenTimeSpinner;
    private Spinner<Double> fixedYellowTimeSpinner;
    private Spinner<Double> fixedRedTimeSpinner;

    // Componentes para modo adaptativo
    private Spinner<Double> adaptiveBaseGreenSpinner;
    private Spinner<Double> adaptiveYellowTimeSpinner;
    private Spinner<Double> adaptiveMinRedTimeSpinner;
    private Spinner<Double> adaptiveMaxRedTimeSpinner;
    private Spinner<Double> adaptiveMaxGreenSpinner;
    private Spinner<Double> adaptiveMinGreenTimeSpinner;
    private Spinner<Double> adaptiveIncrementSpinner;
    private Spinner<Integer> adaptiveQueueThresholdSpinner;

    // Componentes para modo economia de energia
    private Spinner<Double> energySavingBaseGreenSpinner;
    private Spinner<Double> energySavingYellowTimeSpinner;
    private Spinner<Double> energySavingMinRedTimeSpinner;
    private Spinner<Double> energySavingMaxRedTimeSpinner;
    private Spinner<Double> energySavingMinGreenSpinner;
    private Spinner<Double> energySavingMaxGreenTimeSpinner;
    private Spinner<Integer> energySavingThresholdSpinner;

    // Tema de cores
    private final String BACKGROUND_COLOR = "#f0f4f8";
    private final String PRIMARY_COLOR = "#2b6cb0";
    private final String SECONDARY_COLOR = "#3182ce";
    private final String ACCENT_COLOR = "#4299e1";
    private final String TEXT_COLOR = "#2d3748";
    private final String PANEL_COLOR = "#ffffff";

    public ConfigurationScreen(InicioSis application, Stage primaryStage) {
        this.application = application;
        this.primaryStage = primaryStage;
        this.config = new Config();
    }

    public Scene createConfigScene() {
        // Usar BorderPane como layout principal
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        // Cabeçalho
        VBox headerBox = createHeader();
        root.setTop(headerBox);

        // Container principal com ScrollPane
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(10, 0, 10, 0));

        // Seleção de mapa
        TitledPane mapSelectionPane = createMapSelectionPane();

        // Seções de configuração
        TitledPane configGeralPane = criarSecaoConfigGeral();
        TitledPane configFixoPane = criarSecaoModoFixo();
        TitledPane configAdaptativoPane = criarSecaoModoAdaptativo();
        TitledPane configEconomiaPane = criarSecaoModoEconomia();

        // Accordion para organizar as seções
        Accordion accordion = new Accordion();
        accordion.getPanes().addAll(mapSelectionPane, configGeralPane, configFixoPane, configAdaptativoPane,
                configEconomiaPane);
        accordion.setExpandedPane(mapSelectionPane);

        mainContent.getChildren().add(accordion);

        // Criar ScrollPane e adicionar o conteúdo principal
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(mainContent);
        scrollPane.setFitToWidth(true); // Ajusta a largura ao container
        scrollPane.setFitToHeight(false); // Permite rolagem vertical
        scrollPane.setPannable(true); // Permite arrastar para rolar
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Barra vertical quando necessário
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Nunca mostrar barra horizontal

        // Estilo do ScrollPane
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        root.setCenter(scrollPane);

        // Rodapé com botões
        HBox footerBox = createFooter();
        root.setBottom(footerBox);

        Scene scene = new Scene(root, 700, 650);
        return scene;
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        // Título
        Text title = new Text("SemaFlux");
        title.getStyleClass().add("header-title");

        // Adicionar sombra ao título
        DropShadow shadow = new DropShadow();
        shadow.setRadius(3.0);
        shadow.setOffsetX(1.0);
        shadow.setOffsetY(1.0);
        shadow.setColor(Color.gray(0.4, 0.3));
        title.setEffect(shadow);

        Text subtitle = new Text("Simulador de Semáforos e Fluxo de Tráfego");
        subtitle.getStyleClass().add("header-subtitle");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private HBox createFooter() {
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(20, 0, 0, 0));

        // Botão de ajuda
        Button helpButton = new Button("Ajuda");
        helpButton.getStyleClass().add("secondary-button");
        helpButton.setOnAction(e -> showHelpDialog());

        // Botão de iniciar
        Button iniciarButton = new Button("Iniciar Simulação");
        iniciarButton.getStyleClass().add("primary-button");
        iniciarButton.setPrefWidth(150);
        iniciarButton.setPrefHeight(40);
        iniciarButton.setOnAction(e -> iniciarSimulacao());

        footer.getChildren().addAll(helpButton, iniciarButton);
        return footer;
    }

    private TitledPane createMapSelectionPane() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        grid.getStyleClass().add("panel");
        grid.getStyleClass().add("config-grid");

        // Título informativo
        Text infoText = new Text("Selecione o mapa que será usado na simulação:");
        infoText.getStyleClass().add("section-label");
        GridPane.setColumnSpan(infoText, 2);
        grid.add(infoText, 0, 0);

        // Seleção de mapa
        Label mapaLabel = new Label("Mapa:");
        grid.add(mapaLabel, 0, 1);

        mapaCombo = new ComboBox<>();
        mapaCombo.getItems().addAll("Jóquei - Teresina, Piauí", "Personalizado");
        mapaCombo.setValue("Jóquei - Teresina, Piauí");
        mapaCombo.setMaxWidth(Double.MAX_VALUE);
        mapaCombo.setTooltip(new Tooltip("Selecione o mapa da cidade para a simulação"));
        GridPane.setFillWidth(mapaCombo, true);
        grid.add(mapaCombo, 1, 1);

        // Campo para arquivo JSON personalizado
        Label arquivoLabel = new Label("Arquivo JSON:");
        grid.add(arquivoLabel, 0, 2);

        // Container para o campo de texto e botão procurar
        HBox fileSelectionBox = new HBox(10);
        
        arquivoJsonField = new TextField();
        arquivoJsonField.setEditable(false);
        arquivoJsonField.setPromptText("Selecione um arquivo JSON...");
        arquivoJsonField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(arquivoJsonField, Priority.ALWAYS);
        
        Button procurarButton = new Button("Procurar");
        procurarButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecionar arquivo JSON");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos JSON", "*.json")
            );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                arquivoJsonField.setText(selectedFile.getAbsolutePath());
                arquivoJsonSelecionado = selectedFile;
                mapaCombo.setValue("Personalizado");
            }
        });
        
        fileSelectionBox.getChildren().addAll(arquivoJsonField, procurarButton);
        GridPane.setColumnSpan(fileSelectionBox, 2);
        grid.add(fileSelectionBox, 0, 3);

        // Descrição do mapa
        TextArea descricaoMapa = new TextArea(
                "Mapa do bairro Jóquei em Teresina, Piauí, Brasil. Este mapa contém múltiplas avenidas e semáforos, adequado para simular o tráfego urbano em diferentes condições.");
        descricaoMapa.setWrapText(true);
        descricaoMapa.setEditable(false);
        descricaoMapa.setPrefRowCount(3);
        descricaoMapa.getStyleClass().add("info-box");
        GridPane.setColumnSpan(descricaoMapa, 2);
        grid.add(descricaoMapa, 0, 4);

        // Atualizar descrição ao mudar o mapa
        mapaCombo.setOnAction(e -> {
            String selectedMap = mapaCombo.getValue();
            if (selectedMap.contains("Jóquei")) {
                descricaoMapa.setText(
                        "Mapa do bairro Jóquei em Teresina, Piauí, Brasil. Este mapa contém múltiplas avenidas e semáforos, adequado para simular o tráfego urbano em diferentes condições.");
            } else if (selectedMap.equals("Personalizado")) {
                descricaoMapa.setText(
                        "Mapa personalizado carregado do arquivo JSON selecionado. Certifique-se de que o arquivo segue o formato esperado com nós, arestas e sinais de trânsito corretamente definidos.");
            }
        });

        TitledPane titledPane = new TitledPane("Seleção de Mapa", grid);
        titledPane.setExpanded(true);
        titledPane.setCollapsible(true);
        return titledPane;
    }

    private TitledPane criarSecaoConfigGeral() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        grid.getStyleClass().add("panel");
        grid.getStyleClass().add("config-grid");

        // Configurar colunas
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(40);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(45);
        ColumnConstraints column3 = new ColumnConstraints();
        column3.setPercentWidth(15);
        grid.getColumnConstraints().addAll(column1, column2, column3);

        int row = 0;

        // Modo de semáforo
        Label modoLabel = new Label("Modo de Semáforo:");
        grid.add(modoLabel, 0, row);

        modoSemaforoCombo = new ComboBox<>();
        modoSemaforoCombo.getItems().addAll("Fixo", "Adaptativo", "Economia de Energia");
        modoSemaforoCombo.setValue("Fixo");
        modoSemaforoCombo.setMaxWidth(Double.MAX_VALUE);
        modoSemaforoCombo.setTooltip(new Tooltip(
                "Fixo: Tempo constante | Adaptativo: Ajusta com tráfego | Economia: Otimiza em baixo tráfego"));
        grid.add(modoSemaforoCombo, 1, row);
        row++;

        // Taxa de geração de veículos
        Label taxaLabel = new Label("Taxa de Geração de Veículos:");
        grid.add(taxaLabel, 0, row);

        HBox sliderBox = new HBox(10);
        sliderBox.setAlignment(Pos.CENTER_LEFT);
        
        taxaGeracaoVeiculosSlider = new Slider(0.1, 1.0, config.getTaxaGeracaoVeiculos());
        taxaGeracaoVeiculosSlider.setShowTickLabels(true);
        taxaGeracaoVeiculosSlider.setShowTickMarks(true);
        taxaGeracaoVeiculosSlider.setMajorTickUnit(0.1);
        taxaGeracaoVeiculosSlider.setMinorTickCount(1);
        taxaGeracaoVeiculosSlider.setBlockIncrement(0.1);
        taxaGeracaoVeiculosSlider.setPrefWidth(200); // Definir largura fixa para o slider
        taxaGeracaoVeiculosSlider
                .setTooltip(new Tooltip("Controla quantos veículos serão gerados por segundo (0.1 a 1.0)"));

        Label valorTaxaLabel = new Label(String.format("%.1f", config.getTaxaGeracaoVeiculos()));
        valorTaxaLabel.setStyle("-fx-font-weight: bold;");
        valorTaxaLabel.setMinWidth(30); // Garantir largura mínima para o label

        taxaGeracaoVeiculosSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valorTaxaLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        sliderBox.getChildren().addAll(taxaGeracaoVeiculosSlider, valorTaxaLabel);
        grid.add(sliderBox, 1, row);
        row++;

        // Horário de pico
        Label picoLabel = new Label("Horário de Pico:");
        picoLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(picoLabel, 0, row);

        horarioPicoCheck = new CheckBox();
        horarioPicoCheck.setSelected(config.isHorarioPico());
        horarioPicoCheck.setTooltip(new Tooltip("Simula condições de horário de pico, com maior congestionamento"));
        grid.add(horarioPicoCheck, 1, row);
        row++;

        // Duração da simulação
        Label duracaoLabel = new Label("Duração da Simulação (s):");
        duracaoLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(duracaoLabel, 0, row);

        duracaoSimulacaoSpinner = new Spinner<>(60.0, 3600.0, config.getDuracaoSimulacao(), 30.0);
        duracaoSimulacaoSpinner.setEditable(true);
        duracaoSimulacaoSpinner.setPrefWidth(150);
        duracaoSimulacaoSpinner.setTooltip(new Tooltip("Tempo total de simulação em segundos (60 a 3600)"));
        grid.add(duracaoSimulacaoSpinner, 1, row);
        row++;

        // Tempo para parar de gerar veículos
        Label pararGeracaoLabel = new Label("Parar Geração Veículos (s):");
        pararGeracaoLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(pararGeracaoLabel, 0, row);

        pararGeracaoVeiculosSpinner = new Spinner<>(30.0, 3600.0, config.getParadaGeracao(), 30.0);
        pararGeracaoVeiculosSpinner.setEditable(true);
        pararGeracaoVeiculosSpinner.setPrefWidth(150);
        pararGeracaoVeiculosSpinner
                .setTooltip(new Tooltip("Momento em que a geração de novos veículos será interrompida"));
        grid.add(pararGeracaoVeiculosSpinner, 1, row);
        row++;

        TitledPane titledPane = new TitledPane("Configuração Geral", grid);
        titledPane.setExpanded(false);
        return titledPane;
    }

    private TitledPane criarSecaoModoFixo() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        grid.getStyleClass().add("panel");
        grid.getStyleClass().add("config-grid");

        // Configurar colunas
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(40);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(60);
        grid.getColumnConstraints().addAll(column1, column2);

        int row = 0;

        // Tempo verde fixo
        Label greenLabel = new Label("Tempo Verde (s):");
        greenLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(greenLabel, 0, row);

        fixedGreenTimeSpinner = new Spinner<>(5.0, 60.0, config.getFixedGreenTime(), 1.0);
        fixedGreenTimeSpinner.setEditable(true);
        fixedGreenTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        fixedGreenTimeSpinner.setTooltip(new Tooltip("Duração da fase verde no modo de tempo fixo"));
        grid.add(fixedGreenTimeSpinner, 1, row);
        row++;

        // Tempo amarelo fixo
        Label yellowLabel = new Label("Tempo Amarelo (s):");
        yellowLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(yellowLabel, 0, row);

        fixedYellowTimeSpinner = new Spinner<>(1.0, 10.0, config.getFixedYellowTime(), 0.5);
        fixedYellowTimeSpinner.setEditable(true);
        fixedYellowTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        fixedYellowTimeSpinner.setTooltip(new Tooltip("Duração da fase amarela no modo de tempo fixo"));
        grid.add(fixedYellowTimeSpinner, 1, row);
        row++;

        // Tempo vermelho fixo
        Label redLabel = new Label("Tempo Vermelho (s):");
        redLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(redLabel, 0, row);

        fixedRedTimeSpinner = new Spinner<>(5.0, 60.0, config.getFixedRedTime(), 1.0);
        fixedRedTimeSpinner.setEditable(true);
        fixedRedTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        fixedRedTimeSpinner.setTooltip(new Tooltip("Duração da fase vermelha no modo de tempo fixo"));
        grid.add(fixedRedTimeSpinner, 1, row);
        row++;

        // Texto informativo
        TextArea infoText = new TextArea(
                "No modo de tempo fixo, os semáforos alternam entre as fases com tempos constantes, independentemente do fluxo de veículos.");
        infoText.setWrapText(true);
        infoText.setEditable(false);
        infoText.setPrefRowCount(3);
        infoText.getStyleClass().add("info-box");
        GridPane.setColumnSpan(infoText, 2);
        grid.add(infoText, 0, row);

        TitledPane titledPane = new TitledPane("Modo Fixo", grid);
        titledPane.setExpanded(false);
        return titledPane;
    }

    private TitledPane criarSecaoModoAdaptativo() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        grid.getStyleClass().add("panel");
        grid.getStyleClass().add("config-grid");

        // Configurar colunas
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(45);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(55);
        grid.getColumnConstraints().addAll(column1, column2);

        int row = 0;

        // Tempo verde base
        Label baseGreenLabel = new Label("Tempo Verde Base (s):");
        baseGreenLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(baseGreenLabel, 0, row);

        adaptiveBaseGreenSpinner = new Spinner<>(5.0, 30.0, config.getAdaptiveVerdeBase(), 1.0);
        adaptiveBaseGreenSpinner.setEditable(true);
        adaptiveBaseGreenSpinner.setMaxWidth(Double.MAX_VALUE);
        adaptiveBaseGreenSpinner.setTooltip(new Tooltip("Tempo verde mínimo antes de considerar ajustes"));
        grid.add(adaptiveBaseGreenSpinner, 1, row);
        row++;

        // Tempo amarelo
        Label yellowLabel = new Label("Tempo Amarelo (s):");
        yellowLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(yellowLabel, 0, row);

        adaptiveYellowTimeSpinner = new Spinner<>(1.0, 10.0, config.getAdaptiveAmareloBase(), 0.5);
        adaptiveYellowTimeSpinner.setEditable(true);
        adaptiveYellowTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        adaptiveYellowTimeSpinner.setTooltip(new Tooltip("Duração da fase amarela (transição)"));
        grid.add(adaptiveYellowTimeSpinner, 1, row);
        row++;

        // Tempo vermelho mínimo
        Label minRedLabel = new Label("Tempo Vermelho Mínimo (s):");
        minRedLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(minRedLabel, 0, row);

        adaptiveMinRedTimeSpinner = new Spinner<>(5.0, 30.0, config.getAdaptiveMinTempoVermelho(), 1.0);
        adaptiveMinRedTimeSpinner.setEditable(true);
        adaptiveMinRedTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        adaptiveMinRedTimeSpinner.setTooltip(new Tooltip("Tempo mínimo que um semáforo permanecerá vermelho"));
        grid.add(adaptiveMinRedTimeSpinner, 1, row);
        row++;

        // Tempo vermelho máximo
        Label maxRedLabel = new Label("Tempo Vermelho Máximo (s):");
        maxRedLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(maxRedLabel, 0, row);

        adaptiveMaxRedTimeSpinner = new Spinner<>(10.0, 60.0, config.getAdaptiveTempoMaxVermelho(), 1.0);
        adaptiveMaxRedTimeSpinner.setEditable(true);
        adaptiveMaxRedTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        adaptiveMaxRedTimeSpinner.setTooltip(new Tooltip("Tempo máximo que um semáforo permanecerá vermelho"));
        grid.add(adaptiveMaxRedTimeSpinner, 1, row);
        row++;

        // Tempo verde máximo
        Label maxGreenLabel = new Label("Tempo Verde Máximo (s):");
        maxGreenLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(maxGreenLabel, 0, row);

        adaptiveMaxGreenSpinner = new Spinner<>(10.0, 60.0, config.getAdaptiveMaxVerde(), 1.0);
        adaptiveMaxGreenSpinner.setEditable(true);
        adaptiveMaxGreenSpinner.setMaxWidth(Double.MAX_VALUE);
        adaptiveMaxGreenSpinner.setTooltip(new Tooltip("Tempo máximo que um semáforo permanecerá verde"));
        grid.add(adaptiveMaxGreenSpinner, 1, row);
        row++;

        // Tempo verde mínimo
        Label minGreenLabel = new Label("Tempo Verde Mínimo (s):");
        minGreenLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(minGreenLabel, 0, row);

        adaptiveMinGreenTimeSpinner = new Spinner<>(3.0, 20.0, config.getAdaptiveMinTempoVerde(), 1.0);
        adaptiveMinGreenTimeSpinner.setEditable(true);
        adaptiveMinGreenTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        adaptiveMinGreenTimeSpinner.setTooltip(new Tooltip("Tempo mínimo que um semáforo permanecerá verde"));
        grid.add(adaptiveMinGreenTimeSpinner, 1, row);
        row++;

        // Incremento por veículo
        Label incrementLabel = new Label("Incremento por Veículo (s):");
        incrementLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(incrementLabel, 0, row);

        adaptiveIncrementSpinner = new Spinner<>(0.1, 5.0, config.getAdaptiveAumento(), 0.1);
        adaptiveIncrementSpinner.setEditable(true);
        adaptiveIncrementSpinner.setMaxWidth(Double.MAX_VALUE);
        adaptiveIncrementSpinner.setTooltip(new Tooltip("Tempo adicional de verde para cada veículo na fila"));
        grid.add(adaptiveIncrementSpinner, 1, row);
        row++;

        // Limiar de fila
        Label thresholdLabel = new Label("Limiar de Fila (veículos):");
        thresholdLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(thresholdLabel, 0, row);

        adaptiveQueueThresholdSpinner = new Spinner<>(1, 20, config.getAdaptiveQueueThreshold(), 1);
        adaptiveQueueThresholdSpinner.setEditable(true);
        adaptiveQueueThresholdSpinner.setMaxWidth(Double.MAX_VALUE);
        adaptiveQueueThresholdSpinner
                .setTooltip(new Tooltip("Número de veículos a partir do qual o tempo é estendido"));
        grid.add(adaptiveQueueThresholdSpinner, 1, row);
        row++;

        // Texto informativo
        TextArea infoText = new TextArea(
                "No modo adaptativo, os semáforos ajustam seus tempos baseados no volume de tráfego, estendendo o tempo verde quando há mais veículos na fila.");
        infoText.setWrapText(true);
        infoText.setEditable(false);
        infoText.setPrefRowCount(3);
        infoText.getStyleClass().add("info-box");
        GridPane.setColumnSpan(infoText, 2);
        grid.add(infoText, 0, row);

        TitledPane titledPane = new TitledPane("Modo Adaptativo", grid);
        titledPane.setExpanded(false);
        return titledPane;
    }

    private TitledPane criarSecaoModoEconomia() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        grid.getStyleClass().add("panel");
        grid.getStyleClass().add("config-grid");

        // Configurar colunas
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(45);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(55);
        grid.getColumnConstraints().addAll(column1, column2);

        int row = 0;

        // Tempo verde base
        Label baseGreenLabel = new Label("Tempo Verde Base (s):");
        baseGreenLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(baseGreenLabel, 0, row);

        energySavingBaseGreenSpinner = new Spinner<>(5.0, 40.0, config.getVerdeBaseEconomia(), 1.0);
        energySavingBaseGreenSpinner.setEditable(true);
        energySavingBaseGreenSpinner.setMaxWidth(Double.MAX_VALUE);
        energySavingBaseGreenSpinner.setTooltip(new Tooltip("Tempo verde padrão no modo de economia de energia"));
        grid.add(energySavingBaseGreenSpinner, 1, row);
        row++;

        // Tempo amarelo
        Label yellowLabel = new Label("Tempo Amarelo (s):");
        yellowLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(yellowLabel, 0, row);

        energySavingYellowTimeSpinner = new Spinner<>(1.0, 10.0, config.getAmareloEconomia(), 0.5);
        energySavingYellowTimeSpinner.setEditable(true);
        energySavingYellowTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        energySavingYellowTimeSpinner.setTooltip(new Tooltip("Duração da fase amarela (transição)"));
        grid.add(energySavingYellowTimeSpinner, 1, row);
        row++;

        // Tempo vermelho mínimo
        Label minRedLabel = new Label("Tempo Vermelho Mínimo (s):");
        minRedLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(minRedLabel, 0, row);

        energySavingMinRedTimeSpinner = new Spinner<>(5.0, 30.0, config.getMinimoVermelhoEconomia(), 1.0);
        energySavingMinRedTimeSpinner.setEditable(true);
        energySavingMinRedTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        energySavingMinRedTimeSpinner.setTooltip(new Tooltip("Tempo mínimo que um semáforo permanecerá vermelho"));
        grid.add(energySavingMinRedTimeSpinner, 1, row);
        row++;

        // Tempo vermelho máximo
        Label maxRedLabel = new Label("Tempo Vermelho Máximo (s):");
        maxRedLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(maxRedLabel, 0, row);

        energySavingMaxRedTimeSpinner = new Spinner<>(10.0, 60.0, config.getMaximoVermelhoEconomia(), 1.0);
        energySavingMaxRedTimeSpinner.setEditable(true);
        energySavingMaxRedTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        energySavingMaxRedTimeSpinner.setTooltip(new Tooltip("Tempo máximo que um semáforo permanecerá vermelho"));
        grid.add(energySavingMaxRedTimeSpinner, 1, row);
        row++;

        // Tempo verde mínimo
        Label minGreenLabel = new Label("Tempo Verde Mínimo (s):");
        minGreenLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(minGreenLabel, 0, row);

        energySavingMinGreenSpinner = new Spinner<>(3.0, 20.0, config.getMinimoVerdeEconomia(), 1.0);
        energySavingMinGreenSpinner.setEditable(true);
        energySavingMinGreenSpinner.setMaxWidth(Double.MAX_VALUE);
        energySavingMinGreenSpinner.setTooltip(new Tooltip("Tempo mínimo que um semáforo permanecerá verde"));
        grid.add(energySavingMinGreenSpinner, 1, row);
        row++;

        // Tempo verde máximo
        Label maxGreenLabel = new Label("Tempo Verde Máximo (s):");
        maxGreenLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(maxGreenLabel, 0, row);

        energySavingMaxGreenTimeSpinner = new Spinner<>(10.0, 60.0, config.getTempoMaximoVerdeEconomia(), 1.0);
        energySavingMaxGreenTimeSpinner.setEditable(true);
        energySavingMaxGreenTimeSpinner.setMaxWidth(Double.MAX_VALUE);
        energySavingMaxGreenTimeSpinner.setTooltip(new Tooltip("Tempo máximo que um semáforo permanecerá verde"));
        grid.add(energySavingMaxGreenTimeSpinner, 1, row);
        row++;

        // Limiar de tráfego baixo
        Label thresholdLabel = new Label("Limiar Tráfego Baixo (veículos):");
        thresholdLabel.setTextFill(Color.web(TEXT_COLOR));
        grid.add(thresholdLabel, 0, row);

        energySavingThresholdSpinner = new Spinner<>(0, 10, config.getLimiarEconomia(), 1);
        energySavingThresholdSpinner.setEditable(true);
        energySavingThresholdSpinner.setMaxWidth(Double.MAX_VALUE);
        energySavingThresholdSpinner
                .setTooltip(new Tooltip("Quantidade de veículos abaixo da qual o modo de economia é ativado"));
        grid.add(energySavingThresholdSpinner, 1, row);
        row++;

        // Texto informativo
        TextArea infoText = new TextArea(
                "No modo de economia de energia, os semáforos reduzem seu consumo em períodos de baixo tráfego, aumentando o tempo vermelho e diminuindo o tempo verde quando poucos veículos estão presentes.");
        infoText.setWrapText(true);
        infoText.setEditable(false);
        infoText.setPrefRowCount(3);
        infoText.getStyleClass().add("info-box");
        GridPane.setColumnSpan(infoText, 2);
        grid.add(infoText, 0, row);

        TitledPane titledPane = new TitledPane("Modo Economia de Energia", grid);
        titledPane.setExpanded(false);
        return titledPane;
    }

    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ajuda do SemaFlux");
        alert.setHeaderText("Guia de Uso do SemaFlux");

        TextArea textArea = new TextArea(
                "COMO USAR:\n\n" +
                        "1. Selecione o mapa para simulação\n" +
                        "   - Use o mapa pré-definido do Jóquei ou selecione \"Personalizado\" para importar seu próprio arquivo JSON\n" +
                        "   - Para mapas personalizados, clique em \"Procurar\" e selecione um arquivo JSON no formato correto\n" +
                        "2. Configure as opções gerais de simulação\n" +
                        "3. Ajuste os parâmetros específicos do modo de semáforo escolhido\n" +
                        "4. Clique em 'Iniciar Simulação' para começar\n\n" +
                        "MODOS DE SEMÁFORO:\n\n" +
                        "- Fixo: Semáforos alternam com tempos constantes\n" +
                        "- Adaptativo: Semáforos ajustam tempos com base no tráfego\n" +
                        "- Economia: Otimiza energia em horários de baixo tráfego\n\n" +
                        "ARQUIVOS JSON PERSONALIZADOS:\n\n" +
                        "Os arquivos JSON devem seguir o mesmo formato dos mapas pré-definidos, contendo:\n" +
                        "- Nós (intersecções) com coordenadas geográficas\n" +
                        "- Arestas (vias) conectando os nós\n" +
                        "- Definições de semáforos nas interseções apropriadas");
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void iniciarSimulacao() {
        // Atualizar a configuração com os valores da interface
        atualizarConfiguracao();

        // Obter o mapa selecionado
        String selectedMap = mapaCombo.getValue();

        // Iniciar a simulação com a configuração atualizada e o mapa selecionado
        application.iniciarSimulacao(config, primaryStage, selectedMap, arquivoJsonSelecionado);
    }

    private void atualizarConfiguracao() {
        // Configuração geral
        int modoSemaforo = modoSemaforoCombo.getSelectionModel().getSelectedIndex() + 1; // 1=Fixo, 2=Adaptativo,
                                                                                         // 3=Economia
        config.setModoSemaforo(modoSemaforo);
        config.setTaxaGeracaoVeiculos(taxaGeracaoVeiculosSlider.getValue());
        config.setHorarioPico(horarioPicoCheck.isSelected());
        config.setDuracaoSimulacao(duracaoSimulacaoSpinner.getValue());
        config.setParadaGeracao(pararGeracaoVeiculosSpinner.getValue());

        // Modo fixo
        config.setFixedGreenTime(fixedGreenTimeSpinner.getValue());
        config.setFixedYellowTime(fixedYellowTimeSpinner.getValue());
        config.setFixedRedTime(fixedRedTimeSpinner.getValue());

        // Modo adaptativo
        config.setAdaptiveVerdeBase(adaptiveBaseGreenSpinner.getValue());
        config.setAdaptiveAmareloBase(adaptiveYellowTimeSpinner.getValue());
        config.setAdaptiveMinTempoVermelho(adaptiveMinRedTimeSpinner.getValue());
        config.setAdaptiveTempoMaxVermelho(adaptiveMaxRedTimeSpinner.getValue());
        config.setAdaptiveMaxVerde(adaptiveMaxGreenSpinner.getValue());
        config.setAdaptiveMinTempoVerde(adaptiveMinGreenTimeSpinner.getValue());
        config.setAdaptiveAumento(adaptiveIncrementSpinner.getValue());
        config.setAdaptiveQueueThreshold(adaptiveQueueThresholdSpinner.getValue());

        // Modo economia
        config.setVerdeBaseEconomia(energySavingBaseGreenSpinner.getValue());
        config.setAmareloEconomia(energySavingYellowTimeSpinner.getValue());
        config.setMinimoVermelhoEconomia(energySavingMinRedTimeSpinner.getValue());
        config.setMaximoVermelhoEconomia(energySavingMaxRedTimeSpinner.getValue());
        config.setMinimoVerdeEconomia(energySavingMinGreenSpinner.getValue());
        config.setTempoMaximoVerdeEconomia(energySavingMaxGreenTimeSpinner.getValue());
        config.setLimiarEconomia(energySavingThresholdSpinner.getValue());
    }
}