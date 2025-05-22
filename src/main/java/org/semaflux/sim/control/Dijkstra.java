package org.semaflux.sim.control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.semaflux.sim.core.Aresta;
import org.semaflux.sim.core.Grafo;
import org.semaflux.sim.core.ListaLigada;
import org.semaflux.sim.core.No;

public class Dijkstra {

    public static ListaLigada<String> calcularRota(Grafo graph, String originId, String destinationId) {
       
        Map<String, Integer> distances = new HashMap<>(); 
        Map<String, String> previousNodeIds = new HashMap<>(); 
        HashSet<String> visitedNodeIds = new HashSet<>(); 

        Map<String, Integer> unvisitedNodesWithDistance = new HashMap<>();

        No originNode = graph.getNode(originId);
        No destinationNode = graph.getNode(destinationId);

        

        for (No node : graph.getNodes()) { 
            if (node != null) {
                distances.put(node.getId(), Integer.MAX_VALUE);
                unvisitedNodesWithDistance.put(node.getId(), Integer.MAX_VALUE);
            }
        }

        distances.put(originId, 0);
        unvisitedNodesWithDistance.put(originId, 0);

        while (!unvisitedNodesWithDistance.isEmpty()) {
            String currentNodeId = getNoNaoVisitadoMaisProximo(unvisitedNodesWithDistance, visitedNodeIds);

            if (currentNodeId == null || distances.get(currentNodeId) == Integer.MAX_VALUE) {
               
                break; 
            }

            if (currentNodeId.equals(destinationId)) {
                break; 
            }

            visitedNodeIds.add(currentNodeId);
            unvisitedNodesWithDistance.remove(currentNodeId); 

            No currentNodeObject = graph.getNode(currentNodeId);
            if (currentNodeObject == null || currentNodeObject.getEdges() == null) {
                
                continue;
            }

            for (Aresta edge : currentNodeObject.getEdges()) { 
                if (edge == null) continue;

                String neighborNodeId = edge.getTarget();
                if (visitedNodeIds.contains(neighborNodeId)) {
                            continue; 
                }

                No neighborNodeObject = graph.getNode(neighborNodeId);  
                if (neighborNodeObject == null) {
                    
                    continue;
                }

                double edgeTravelTime = edge.getTravelTime();
                if (edgeTravelTime <= 0 || edgeTravelTime == Double.POSITIVE_INFINITY) {
                    
                    continue; 
                }

                
                int newDist = distances.get(currentNodeId) + (int) (edgeTravelTime * 100.0);

                if (newDist < distances.getOrDefault(neighborNodeId, Integer.MAX_VALUE)) {
                    distances.put(neighborNodeId, newDist);
                    previousNodeIds.put(neighborNodeId, currentNodeId);
                    unvisitedNodesWithDistance.put(neighborNodeId, newDist); 
                }
            }
        }

        if (!previousNodeIds.containsKey(destinationId) && !originId.equals(destinationId)) {
           
            return new ListaLigada<>(); 
        }

        return buildPath(previousNodeIds, originId, destinationId);
    }

    private static String getNoNaoVisitadoMaisProximo(Map<String, Integer> unvisitedNodesWithDistance, HashSet<String> visitedNodeIds) {
        String closestNodeId = null;
        int minDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, Integer> entry : unvisitedNodesWithDistance.entrySet()) {
            String nodeId = entry.getKey();
            int distance = entry.getValue();
            if (!visitedNodeIds.contains(nodeId) && distance < minDistance) {
                minDistance = distance;
                closestNodeId = nodeId;
            }
        }
        return closestNodeId;
    }

    private static ListaLigada<String> buildPath(Map<String, String> previousNodeIds, String originId, String destinationId) {
        ListaLigada<String> path = new ListaLigada<>();
        String currentNodeId = destinationId;

        if (!previousNodeIds.containsKey(currentNodeId) && !currentNodeId.equals(originId)) {
           
            return path; 
        }

        while (currentNodeId != null) {
            path.addFirst(currentNodeId);
            if (currentNodeId.equals(originId)) {
                break; 
            }
            currentNodeId = previousNodeIds.get(currentNodeId); 
            if (currentNodeId == null && !path.getFirst().equals(originId)) {
               
                return new ListaLigada<>(); 
            }
        }

       
        if (path.isEmpty() || !path.getFirst().equals(originId)) {
            if (!originId.equals(destinationId)) { 
               
                return new ListaLigada<>(); 
            }
        }

        return path;
    }
}