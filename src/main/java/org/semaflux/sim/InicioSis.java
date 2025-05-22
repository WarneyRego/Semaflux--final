package org.semaflux.sim;

import org.semaflux.sim.control.JsonParser;
import org.semaflux.sim.core.Grafo;
import org.semaflux.sim.simulação.Config;
import org.semaflux.sim.simulação.Simulador;
import org.semaflux.sim.visualization.Visualizer;
import org.semaflux.sim.visualization.ConfigurationScreen;
import org.semaflux.sim.visualization.ResumoSimulacao;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;

public class InicioSis extends Application {

    private Simulador simulator;
    private Thread simulationThread;
    


    

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Iniciando");
        
        // Mostrar a tela de configuração em vez de iniciar diretamente a simulação
        ConfigurationScreen configScreen = new ConfigurationScreen(this, primaryStage);
        Scene configScene = configScreen.createConfigScene();
        
        // Carregar o arquivo CSS
        String cssPath = getClass().getResource("/css/modern-style.css").toExternalForm();
        configScene.getStylesheets().add(cssPath);
        
        primaryStage.setTitle("Configuração da Simulação de Mobilidade Urbana");
        primaryStage.setScene(configScene);
        primaryStage.show();
    }

    /**
     * Inicia a simulação com as configurações personalizadas
     * @param config Configuração com os parâmetros definidos pelo usuário
     * @param primaryStage O Stage principal da aplicação
     * @param selectedMap Nome do mapa selecionado pelo usuário
     */
    public void iniciarSimulacao(Config config, Stage primaryStage, String selectedMap) {
        Grafo graph;

        try {
            // Selecionar o arquivo de mapa com base na escolha do usuário
            String resourcePath;
            if (selectedMap.contains("Centro")) {
                resourcePath = "/mapas/CentroTeresinaPiauiBrazil.json";
            } else {
                resourcePath = "/mapas/JoqueiTeresinaPiauiBrazil.json";
            }
            
            InputStream jsonInputStream = getClass().getResourceAsStream(resourcePath);
            if (jsonInputStream == null) {
                String errorMessage = "Erro Crítico: Não foi possível localizar o arquivo JSON do mapa: " + resourcePath;
                System.err.println(errorMessage);
                mostrarErroFatal(primaryStage, errorMessage);
                return;
            }
            graph = JsonParser.loadGraphFromStream(jsonInputStream, config);

        } catch (Exception e) {
            String errorMessage = "Erro Crítico ao carregar o grafo do JSON: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            String errorMessage = "Erro Crítico: Falha ao carregar o grafo ou o grafo está vazio.";
            System.err.println(errorMessage);
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        // Exibir informações sobre a configuração usada
       

        // Iniciar o simulador e o visualizador
        this.simulator = new Simulador(graph, config);
        Visualizer visualizer = new Visualizer(graph, this.simulator);

        try {
            visualizer.start(primaryStage);
            
            // Aplicar o CSS à cena da simulação
            if (primaryStage.getScene() != null) {
                String cssPath = getClass().getResource("/css/modern-style.css").toExternalForm();
                primaryStage.getScene().getStylesheets().add(cssPath);
            }
            
        } catch (Exception e) {
            String errorMessage = "Erro Crítico ao iniciar o Visualizer: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        simulationThread = new Thread(this.simulator);
        simulationThread.setName("SimulationLoopThread");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private void mostrarErroFatal(Stage stage, String mensagem) {
        Pane errorPane = new Pane(new Text(20, 50, mensagem));
        Scene errorScene = new Scene(errorPane, Math.max(400, mensagem.length() * 7), 100);
        stage.setTitle("Erro na Aplicação");
        stage.setScene(errorScene);
        stage.show();
        Platform.exit();
    }

    @Override
    public void stop() {
        if (simulator != null) {
            simulator.stopSimulation();
        }
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
            try {
                simulationThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}