package org.semaflux.sim.simulação;

import org.semaflux.sim.control.Dijkstra;
import org.semaflux.sim.core.Grafo;
import org.semaflux.sim.core.ListaLigada;
import org.semaflux.sim.core.No;
import org.semaflux.sim.core.Veiculo;

import java.util.Random;

// Gera veículos aleatoriamente
public class GeradorVeiculos {
    private Grafo graph;
    private double generationRate; // Veículos por segundo
    private Random random;

    public GeradorVeiculos(Grafo graph, double generationRate) {
        this.graph = graph;
        this.generationRate = generationRate;
        this.random = new Random();
    }



    public Veiculo generateVehicle(int id) {
        // Verificar se o grafo contém nós e não está vazio
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("Erro: Grafo está vazio ou não foi inicializado. Não é possível gerar veículo.");
            return null;
        }

        // Criar uma lista com os IDs de todos os nós no grafo
        ListaLigada<String> nodeIds = new ListaLigada<>();
        for (No node : graph.getNodes()) {
            nodeIds.add(node.getId());
        }

        int size = nodeIds.size();
        if (size <= 1) {
            System.err.println("Erro: Grafo não possui nós suficientes para origem e destino. Não é possível gerar veículo.");
            return null;
        }

        // Fazer várias tentativas para encontrar um par origem-destino com rota válida
        int maxAttempts = 50; // Máximo de tentativas
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Escolher origem e destino aleatórios
            String origin = getRandomNodeId(nodeIds, size);
            String destination = getRandomNodeId(nodeIds, size);

            // Garantir que origem e destino sejam diferentes
            int retries = 0; // Evitar loop infinito
            while (destination.equals(origin) && retries < 20) {
                destination = getRandomNodeId(nodeIds, size);
                retries++;
            }

            // Verificar se origem e destino existem de fato no grafo
            if (!nodeIds.contains(origin) || !nodeIds.contains(destination)) {
                continue; // Tentar novo par
            }

            // Calcular a rota com Dijkstra
            ListaLigada<String> route = Dijkstra.calcularRota(graph, origin, destination);

            // Verificar se a rota foi calculada corretamente
            if (route == null || route.isEmpty()) {
                if (attempt == maxAttempts - 1) {
                    System.err.println("Erro ao gerar veículo V" + id + ": não foi possível encontrar uma rota válida após " + maxAttempts + " tentativas.");
                }
                continue; // Tentar novo par
            }
            
            // Criar e retornar o veículo com rota válida
            Veiculo vehicle = new Veiculo("V" + id, origin, destination, route);
            return vehicle;
        }
        
        System.err.println("Não foi possível gerar veículo V" + id + " após várias tentativas.");
        return null; // Falha após todas as tentativas
    }

    private String getRandomNodeId(ListaLigada<String> nodeIds, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Lista de IDs de nós está vazia. Não é possível selecionar um nó aleatório.");
        }

        int index = random.nextInt(size); // Escolhe um índice aleatório
        int currentIndex = 0;

        for (String nodeId : nodeIds) {
            if (currentIndex++ == index) {
                return nodeId;
            }
        }

        throw new IllegalStateException("Erro na seleção de nó aleatório: índice fora do intervalo.");
    }

    public double getGenerationRate() {
        return generationRate;
    }

    public void setGenerationRate(double rate) {
        this.generationRate = rate;
    }
}