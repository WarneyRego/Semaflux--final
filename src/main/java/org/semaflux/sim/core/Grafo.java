package org.semaflux.sim.core;

import java.util.HashMap; 
import java.util.Map;    

public class Grafo {
    private ListaLigada<No> nodesList; 
    private ListaLigada<Aresta> edgesList; 
    private ListaLigada<SinalTransito> trafficLightsList; 
    private Map<String, No> nodeMap;

    public Grafo() {
        this.nodesList = new ListaLigada<>();
        this.edgesList = new ListaLigada<>();
        this.trafficLightsList = new ListaLigada<>();
        this.nodeMap = new HashMap<>();
    }

    public No getNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        return this.nodeMap.get(nodeId); 
    }

    public void addNode(No node) {
        if (node != null && node.getId() != null && !node.getId().isEmpty()) {
            if (!this.nodeMap.containsKey(node.getId())) {
                this.nodesList.add(node);
                this.nodeMap.put(node.getId(), node);
            }
        }
    }

    public ListaLigada<No> getNodes() {
        return this.nodesList;
    }

    public boolean containsNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return false;
        }
        return this.nodeMap.containsKey(nodeId); // O(1) em média
    }

    // Métodos relacionados a arestas
    public void addEdge(Aresta edge) {
        if (edge != null) {
            this.edgesList.add(edge);
        } 
    }

    public ListaLigada<Aresta> getEdges() {
        return this.edgesList;
    }

    public boolean containsEdge(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isEmpty() || targetId.isEmpty()) {
            return false;
        }
        for (Aresta edge : this.edgesList) {
            if (edge == null) continue;
            if (edge.getSource().equals(sourceId) && edge.getDestination().equals(targetId)) {
                return true;
            }
         
        }
        return false;
    }

    // Métodos relacionados a sinais de trânsito
    public void addTrafficLight(SinalTransito trafficLight) {
        if (trafficLight != null) {
            this.trafficLightsList.add(trafficLight);
        }
    }

    public ListaLigada<SinalTransito> getTrafficLights() {
        return this.trafficLightsList;
    }
}