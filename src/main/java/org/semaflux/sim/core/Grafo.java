package org.semaflux.sim.core;

import java.util.HashMap; // Importar HashMap
import java.util.Map;    // Importar Map

// Representa o grafo da rede urbana
public class Grafo {
    private ListaLigada<No> nodesList; // Usando a CustomLinkedList refatorada
    private ListaLigada<Aresta> edgesList; // Usando a CustomLinkedList refatorada
    private ListaLigada<SinalTransito> trafficLightsList; // Usando a CustomLinkedList refatorada

    // Estrutura auxiliar permitida para busca rápida de nós por ID
    private Map<String, No> nodeMap;

    public Grafo() {
        this.nodesList = new ListaLigada<>();
        this.edgesList = new ListaLigada<>();
        this.trafficLightsList = new ListaLigada<>();
        this.nodeMap = new HashMap<>(); // Inicializar o HashMap
    }

    public void addNode(No node) {
        if (node != null && node.getId() != null && !node.getId().isEmpty()) {
            if (!this.nodeMap.containsKey(node.getId())) {
                this.nodesList.add(node); // Adiciona à sua lista personalizada
                this.nodeMap.put(node.getId(), node); // Adiciona ao HashMap
            }
        }
    }

    public ListaLigada<No> getNodes() {
        return this.nodesList;
    }

    // Busca um nó específico no grafo pelo seu ID usando o HashMap
    public No getNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        return this.nodeMap.get(nodeId); // Busca O(1) em média
    }

    public void addEdge(Aresta edge) {
        if (edge != null) {
            this.edgesList.add(edge);
        } 
    }

    public ListaLigada<Aresta> getEdges() {
        return this.edgesList;
    }

    public void addTrafficLight(SinalTransito trafficLight) {
        if (trafficLight != null) {
            this.trafficLightsList.add(trafficLight);
        }
    }

    public ListaLigada<SinalTransito> getTrafficLights() {
        return this.trafficLightsList;
    }

    public boolean containsNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return false;
        }
        return this.nodeMap.containsKey(nodeId); // O(1) em média
    }

    public boolean containsEdge(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isEmpty() || targetId.isEmpty()) {
            return false;
        }
        for (Aresta edge : this.edgesList) {
            if (edge == null) continue;
            // Checagem primária
            if (edge.getSource().equals(sourceId) && edge.getDestination().equals(targetId)) {
                return true;
            }
            // Se a lógica de bidirecionalidade ainda for necessária aqui (idealmente o JsonParser já trata isso
            // criando duas arestas unidirecionais para 'oneway:false')
            // if (!edge.isOneway() && edge.getSource().equals(targetId) && edge.getDestination().equals(sourceId)) {
            // return true;
            // }
        }
        return false;
    }
}