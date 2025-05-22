package org.semaflux.sim.simulação;

import org.semaflux.sim.core.*;
import org.semaflux.sim.visualization.ResumoSimulacao;

import java.util.HashMap;
import java.util.Map;

public class Simulador implements Runnable {
    private Grafo graph;
    private Config config;
    private ListaLigada<Veiculo> vehicles;
    private Estatisticas stats;
    private GeradorVeiculos generator;
    private double time;
    private volatile boolean running = true;
    private boolean generationStopped = false; // Adicione esta flag
    
    // Fator de velocidade da simulação (1.0 = velocidade normal)
    private double speedFactor = 1.0;

    public Simulador(Grafo graph, Config config) {
        this.graph = graph;
        this.config = config;
        this.vehicles = new ListaLigada<>();
        this.stats = new Estatisticas();
        this.generator = new GeradorVeiculos(graph, config.getVehicleGenerationRate());
        this.time = 0.0;
        // this.generationStopped = false; // Inicializada na declaração do campo

        validateGraph();
        
        // Verificamos se o grafo é conectado, mas não lançamos exceção
        boolean isConnected = isGraphConnected();
        if (!isConnected) {
            // O grafo não está totalmente conectado, mas a simulação continuará mesmo assim
        }
    }

    @Override
    public void run() {
        double deltaTime = 1.0; // Passo de simulação em segundos

        while (running && time < config.getSimulationDuration()) {
            // Aplicar o fator de velocidade ao deltaTime para ajustar a velocidade da simulação
            double adjustedDeltaTime = deltaTime * speedFactor;
            
            time += adjustedDeltaTime;
            stats.updateCurrentTime(time);

            if (Thread.currentThread().isInterrupted()) {
                this.running = false;
                break;
            }

            // Verifica se deve parar de gerar veículos e atualiza a flag
            if (!generationStopped && time > config.getVehicleGenerationStopTime()) {
                generationStopped = true; // Seta a flag para parar futuras gerações
            }

            // Gera veículos APENAS SE a flag generationStopped for false
            if (!generationStopped) {
                generateVehicles(adjustedDeltaTime);
            }

            updateTrafficLights(adjustedDeltaTime);
            moveVehicles(adjustedDeltaTime);
            logSimulationState();
            stats.calculateCurrentCongestion(this.vehicles, this.graph);

            if (running) {
                sleep(deltaTime); // Mantém o intervalo de sleep fixo para controlar a velocidade
            }
        }
        
        stats.printSummary();
        
        // Mostrar resumo gráfico ao final da simulação
        mostrarResumoGrafico();
    }
    
    /**
     * Mostra a janela de resumo com gráficos detalhados ao final da simulação.
     */
    private void mostrarResumoGrafico() {
        try {
            ResumoSimulacao resumo = new ResumoSimulacao(stats);
            resumo.mostrarResumo();
        } catch (Exception e) {
            System.err.println("Erro ao mostrar resumo gráfico: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Para a simulação completamente.
     */
    public void stopSimulation() {
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public Estatisticas getStats() {
        return stats;
    }

    public ListaLigada<Veiculo> getVehicles() {
        return vehicles;
    }
    
    /**
     * Valida o grafo antes da simulação.
     * Verifica se há nós e arestas suficientes para uma simulação válida.
     * 
     * @throws IllegalStateException Se o grafo não tiver nós ou arestas.
     */
    private void validateGraph() {
        if (graph == null) {
            throw new IllegalStateException("Grafo nulo");
        }
        if (graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new IllegalStateException("Grafo sem nós");
        }
        if (graph.getEdges() == null || graph.getEdges().isEmpty()) {
            throw new IllegalStateException("Grafo sem arestas");
        }
    }
    
    /**
     * Verifica se o grafo é conectado usando BFS.
     * 
     * @return true se o grafo for conectado, false caso contrário.
     */
    private boolean isGraphConnected() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            return false;
        }
        
        Map<String, Boolean> visited = new HashMap<>();
        ListaLigada<String> queue = new ListaLigada<>();
        
        // Começar de qualquer nó
        String startNodeId = graph.getNodes().iterator().next().getId();
        queue.add(startNodeId);
        visited.put(startNodeId, true);
        
        while (!queue.isEmpty()) {
            String currentNodeId = queue.iterator().next();
            queue.remove(currentNodeId);
            
            No currentNode = graph.getNode(currentNodeId);
            if (currentNode == null || currentNode.getEdges() == null) continue;
            
            for (Aresta edge : currentNode.getEdges()) {
                if (edge == null) continue;
                String destNodeId = edge.getDestination();
                if (!visited.containsKey(destNodeId)) {
                    visited.put(destNodeId, true);
                    queue.add(destNodeId);
                }
            }
        }
        
        boolean connected = visited.size() == graph.getNodes().size();
        return connected;
    }

    private void generateVehicles(double deltaTime) {
        double numExpectedVehicles = deltaTime * config.getVehicleGenerationRate();
        int numToGenerate = (int) numExpectedVehicles;
        if (Math.random() < (numExpectedVehicles - numToGenerate)) {
            numToGenerate++;
        }

        for (int i = 0; i < numToGenerate; i++) {
            int vehicleId = stats.getTotalVehiclesGenerated() + 1;
            Veiculo vehicle = generator.generateVehicle(vehicleId);

            if (vehicle != null) {
                vehicles.add(vehicle);
                stats.vehicleGenerated();
            }
        }
    }

    private void updateTrafficLights(double deltaTime) {
        if (graph.getTrafficLights() == null) return;
        for (SinalTransito tl : graph.getTrafficLights()) {
            if (tl != null) {
                tl.update(deltaTime, config.isPeakHour());
            }
        }
    }

    private void moveVehicles(double deltaTime) {
        ListaLigada<Veiculo> vehiclesStillActive = new ListaLigada<>();
        for (Veiculo vehicle : vehicles) {
            if (vehicle == null) continue;
            updateVehicle(vehicle, deltaTime);

            if (running && vehicle.getCurrentNode().equals(vehicle.getDestination()) && vehicle.getPosition() == 0.0) {
                stats.vehicleArrived(vehicle.getTravelTime(), vehicle.getWaitTime(), vehicle.getFuelConsumed());
            } else if (running) {
                vehiclesStillActive.add(vehicle);
            }
        }
        if (running) {
            vehicles = vehiclesStillActive;
        }
    }

    private void updateVehicle(Veiculo vehicle, double deltaTime) {
        if (vehicle == null) return;
        vehicle.incrementTravelTime(deltaTime);

        boolean vehicleIsMoving = false;
        if (vehicle.getPosition() == 0.0) {
            // Verificar se o veículo está em um nó com semáforo
            SinalTransito trafficLight = findTrafficLight(vehicle.getCurrentNode());
            
            if (trafficLight != null) {
                String nextNodeId = getNextNodeInRoute(vehicle);
                if (nextNodeId == null) return;  // Veículo já está no destino
                
                // Determinar a direção em que o veículo está viajando
                String direction = determineDirection(vehicle.getCurrentNode(), nextNodeId);
                
                // Verificar se o veículo pode prosseguir com base no estado do semáforo
                if (checkIfVehicleCanProceed(trafficLight, direction)) {
                    // Se houver veículo na fila, remover (não é necessário chamar explicitamente)
                    vehicle.setPosition(vehicle.getPosition() + (deltaTime / 2.0));  // Começa a mover imediatamente
                    vehicleIsMoving = true;
                } else {
                    // Adicionar veículo à fila do semáforo
                    trafficLight.addVehicleToQueue(direction, vehicle);
                    vehicle.incrementWaitTime(deltaTime);
                }
            } else {
                // Não há semáforo, o veículo pode avançar normalmente
                String nextNodeId = getNextNodeInRoute(vehicle);
                if (nextNodeId != null) {
                    vehicle.setPosition(vehicle.getPosition() + (deltaTime / 2.0));  // Começa a mover imediatamente
                    vehicleIsMoving = true;
                }
            }
        } else {
            vehicleIsMoving = true;
            String sourceNodeOfCurrentSegment = vehicle.getCurrentNode();
            String targetNodeOfCurrentSegment = getNextNodeInRoute(vehicle);

            if (targetNodeOfCurrentSegment == null) {
                System.err.println("UPDATE_VEHICLE (EM ARESTA): Veículo " + vehicle.getId() + " na aresta de " + sourceNodeOfCurrentSegment +
                        " mas getNextNodeInRoute é nulo. Posição: " + String.format("%.2f",vehicle.getPosition()));
                vehicle.setPosition(0.0);
                vehicleIsMoving = false;
                if (!sourceNodeOfCurrentSegment.equals(vehicle.getDestination())) {
                    System.err.println("    Veículo " + vehicle.getId() + " parou em " + sourceNodeOfCurrentSegment + " pois a rota terminou inesperadamente.");
                }
                if (!vehicle.getCurrentNode().equals(vehicle.getDestination())) {
                    vehicle.incrementFuelConsumption(vehicle.getFuelConsumptionRateIdle() * deltaTime);
                }
                return;
            }

            Aresta currentEdge = findEdge(sourceNodeOfCurrentSegment, targetNodeOfCurrentSegment);
            if (currentEdge == null) {
                System.err.println("UPDATE_VEHICLE (EM ARESTA): Veículo " + vehicle.getId() +
                        ". Não foi possível encontrar a aresta entre " + sourceNodeOfCurrentSegment + " e " + targetNodeOfCurrentSegment);
                this.running = false;
                return;
            }
            double edgeTravelTime = currentEdge.getTravelTime();
            if (edgeTravelTime <= 0) edgeTravelTime = deltaTime;

            vehicle.setPosition(vehicle.getPosition() + (deltaTime / edgeTravelTime));

            if (vehicle.getPosition() >= 1.0) {
                vehicle.setCurrentNode(targetNodeOfCurrentSegment);
                vehicle.setPosition(0.0);
                vehicleIsMoving = false;
            }
        }

        if (vehicleIsMoving) {
            vehicle.incrementFuelConsumption(vehicle.getFuelConsumptionRateMoving() * deltaTime);
        } else {
            if (!vehicle.getCurrentNode().equals(vehicle.getDestination()) || vehicle.getPosition() > 0) {
                vehicle.incrementFuelConsumption(vehicle.getFuelConsumptionRateIdle() * deltaTime);
            }
        }
    }

    private Aresta findEdge(String sourceNodeId, String targetNodeId) {
        if (sourceNodeId == null || targetNodeId == null) return null;
        No sourceNode = graph.getNode(sourceNodeId);
        if (sourceNode == null || sourceNode.getEdges() == null) return null;
        for (Aresta edge : sourceNode.getEdges()) {
            if (edge != null && edge.getDestination() != null && edge.getDestination().equals(targetNodeId)) {
                return edge;
            }
        }
        return null;
    }
    
    /**
     * Determina a direção aproximada entre dois nós, baseada em suas coordenadas geográficas.
     * @param fromNodeId O nó de origem
     * @param toNodeId O nó de destino
     * @return A direção em formato de string ("north", "east", "south", "west")
     */
    private String determineDirection(String fromNodeId, String toNodeId) {
        if (fromNodeId == null || toNodeId == null) {
            return "north"; // Valor padrão se os IDs forem nulos
        }
        
        No fromNode = graph.getNode(fromNodeId);
        No toNode = graph.getNode(toNodeId);
        
        if (fromNode == null || toNode == null) {
            return "north"; // Valor padrão se os nós não forem encontrados
        }
        
        double deltaLat = toNode.getLatitude() - fromNode.getLatitude();
        double deltaLon = toNode.getLongitude() - fromNode.getLongitude();
        
        // Determinar a direção baseada na maior variação
        if (Math.abs(deltaLat) > Math.abs(deltaLon)) {
            // Movimento principal é norte-sul
            return deltaLat > 0 ? "north" : "south";
        } else {
            // Movimento principal é leste-oeste
            return deltaLon > 0 ? "east" : "west";
        }
    }
    
    /**
     * Procura o semáforo associado a um nó.
     * @param nodeId ID do nó
     * @return O semáforo associado ao nó, ou null se não houver
     */
    private SinalTransito findTrafficLight(String nodeId) {
        if (graph.getTrafficLights() == null || nodeId == null) return null;
        
        for (SinalTransito trafficLight : graph.getTrafficLights()) {
            if (trafficLight != null && nodeId.equals(trafficLight.getNodeId())) {
                return trafficLight;
            }
        }
        return null;
    }
    
    /**
     * Verifica se um veículo pode prosseguir com base no estado do semáforo.
     * @param trafficLight O semáforo
     * @param direction A direção do movimento
     * @return true se o veículo pode prosseguir, false caso contrário
     */
    private boolean checkIfVehicleCanProceed(SinalTransito trafficLight, String direction) {
        if (trafficLight == null) return true;
        
        // Verificar estado do semáforo para a direção específica
        String lightState = trafficLight.getLightStateForApproach(direction);
        
        // Veículo pode prosseguir se o estado for "green"
        return "green".equalsIgnoreCase(lightState);
    }

    private String getNextNodeInRoute(Veiculo vehicle) {
        if (vehicle == null || vehicle.getRoute() == null || vehicle.getRoute().isEmpty()) {
            return null;
        }

        String currentNodeId = vehicle.getCurrentNode();
        boolean foundCurrent = false;
        
        for (String nodeId : vehicle.getRoute()) {
            if (foundCurrent) {
                return nodeId;
            }
            if (nodeId.equals(currentNodeId)) {
                foundCurrent = true;
            }
        }
        
        return null; // Não há próximo nó (fim da rota ou nó atual não encontrado)
    }

    private void logSimulationState() {
        System.out.println("Tempo: " + String.format("%.2f", time) + "s, Veículos: " + (vehicles != null ? vehicles.size() : 0) +
                ", Congestionamento: " + String.format("%.0f", stats.getCurrentCongestionIndex()));
    }

    private void sleep(double deltaTime) {
        try {
            // O tempo de sleep é inversamente proporcional ao fator de velocidade
            // para manter a fluidez visual da simulação
            int millisToSleep = (int)(deltaTime * 1000 / speedFactor);
            Thread.sleep(millisToSleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Ajusta o fator de velocidade da simulação.
     * @param factor O novo fator de velocidade (1.0 = normal, 2.0 = 2x mais rápido, 0.5 = metade da velocidade)
     */
    public void setSpeedFactor(double factor) {
        if (factor > 0) {
            // Limita o fator de velocidade entre 0.1x (muito lento) e 10x (muito rápido)
            this.speedFactor = Math.max(0.1, Math.min(10.0, factor));
            
            // Log para debug (se necessário)
            
            if (this.speedFactor > 5.0) {
                System.err.println("Aviso: Velocidades muito altas podem reduzir a precisão da simulação.");
            }
            
            // Interrompe o sleep atual para aplicar imediatamente a nova velocidade
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Retorna o fator de velocidade atual da simulação.
     * @return O fator de velocidade atual
     */
    public double getSpeedFactor() {
        return this.speedFactor;
    }
}