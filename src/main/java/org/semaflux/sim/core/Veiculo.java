package org.semaflux.sim.core;

import javafx.scene.paint.Color;
import java.util.Random;

public class Veiculo {
    private String id; // Identificador único
    private String origin; // Nó de origem
    private String destination; // Nó de destino
    private ListaLigada<String> route; // Rota calculada (lista de nós)
    private String currentNode; // Nó atual
    private double travelTime; // Tempo total de viagem (s)
    private double waitTime; // Tempo total de espera (s)
    private double position; // Posição na aresta atual (0 a 1)
    public Veiculo next; // Para lista encadeada
    private double fuelConsumed;
    private double fuelConsumptionRateMoving; // L/s em movimento
    private double fuelConsumptionRateIdle;   // L/s em marcha lenta
    private int tipoVeiculo; // 0=carro, 1=moto, 2=ônibus, 3=caminhão
    private Color cor; // Cor do veículo para visualização
    private static Random random = new Random();
    
    // Construtor
    public Veiculo(String id, String origin, String destination, ListaLigada<String> route) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.route = (route != null) ? route : new ListaLigada<>(); // Atribuir rota válida
        this.currentNode = origin;
        this.travelTime = 0.0;
        this.waitTime = 0.0;
        this.position = 0.0;
        this.next = null;
        this.fuelConsumed = 0.0;
        // Valores de exemplo, podem vir da Configuration ou ser fixos por tipo de veículo no futuro
        this.fuelConsumptionRateMoving = 0.0005; // Ex: 0.5 ml/s em movimento (aprox. 1.8 L/hora)
        this.fuelConsumptionRateIdle = 0.0002;
        // Atribuir um tipo de veículo aleatoriamente
        this.tipoVeiculo = random.nextInt(4);
        // Atribuir uma cor baseada no tipo
        this.cor = gerarCorVeiculo();
    }

    // Cores possíveis para os veículos
    private static final Color[] CORES_CARROS = {
        Color.DEEPSKYBLUE, Color.ROYALBLUE, Color.DARKBLUE, Color.CORNFLOWERBLUE,
        Color.CRIMSON, Color.FIREBRICK, Color.TOMATO, Color.CORAL,
        Color.DARKGREEN, Color.FORESTGREEN, Color.LIMEGREEN, Color.MEDIUMSEAGREEN,
        Color.GOLD, Color.ORANGE, Color.DARKORANGE, Color.ORANGERED,
        Color.PURPLE, Color.DARKVIOLET, Color.MEDIUMORCHID, Color.MEDIUMPURPLE
    };
    
    private static final Color[] CORES_MOTOS = {
        Color.BLACK, Color.DARKGRAY, Color.DIMGRAY, Color.SILVER
    };
    
    private static final Color[] CORES_ONIBUS = {
        Color.YELLOW, Color.GREENYELLOW, Color.YELLOWGREEN
    };
    
    private static final Color[] CORES_CAMINHOES = {
        Color.DARKRED, Color.BROWN, Color.MAROON, Color.SIENNA
    };
    
    /**
     * Gera uma cor para o veículo baseada no seu tipo
     * @return Uma cor para representação visual
     */
    private Color gerarCorVeiculo() {
        switch(this.tipoVeiculo) {
            case 0: // Carro
                return CORES_CARROS[random.nextInt(CORES_CARROS.length)];
            case 1: // Moto
                return CORES_MOTOS[random.nextInt(CORES_MOTOS.length)];
            case 2: // Ônibus
                return CORES_ONIBUS[random.nextInt(CORES_ONIBUS.length)];
            case 3: // Caminhão
                return CORES_CAMINHOES[random.nextInt(CORES_CAMINHOES.length)];
            default:
                return Color.DEEPSKYBLUE;
        }
    }

    /**
     * Retorna o tamanho do veículo em pixels para visualização
     * @return Tamanho do veículo (raio do círculo)
     */
    public double getTamanhoVeiculo() {
        switch(this.tipoVeiculo) {
            case 0: // Carro
                return 4.0;
            case 1: // Moto
                return 3.0;
            case 2: // Ônibus
                return 6.0;
            case 3: // Caminhão
                return 5.5;
            default:
                return 4.0;
        }
    }

    /**
     * Retorna a cor do veículo para visualização
     * @return Cor do veículo
     */
    public Color getCor() {
        return this.cor;
    }
    
    /**
     * Retorna o tipo do veículo
     * @return Tipo do veículo (0=carro, 1=moto, 2=ônibus, 3=caminhão)
     */
    public int getTipoVeiculo() {
        return this.tipoVeiculo;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public ListaLigada<String> getRoute() {
        return route;
    }

    public void setRoute(ListaLigada<String> route) {
        if (route == null) {
            this.route = new ListaLigada<>(); // Substitui por uma rota vazia
        } else {
            this.route = route;
        }
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public void incrementTravelTime(double deltaTime) {
        this.travelTime += deltaTime;
    }

    public double getWaitTime() {
        return waitTime;
    }

    public void incrementWaitTime(double deltaTime) {
        this.waitTime += deltaTime;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public double getFuelConsumed() {
        return fuelConsumed;
    }

    public void incrementFuelConsumption(double consumption) {
        this.fuelConsumed += consumption;
    }

    public double getFuelConsumptionRateMoving() {
        return fuelConsumptionRateMoving;
    }

    public double getFuelConsumptionRateIdle() {
        return fuelConsumptionRateIdle;
    }

}
