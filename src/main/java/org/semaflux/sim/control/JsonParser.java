package org.semaflux.sim.control;

import org.semaflux.sim.core.*;
import org.semaflux.sim.simulação.Config;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

/**
 * Parser para carregar o grafo do arquivo JSON.
 */
public class JsonParser {

    /**
     * Método estático para carregar o grafo a partir de um arquivo JSON.
     *
     * @param filename Caminho para o arquivo JSON.
     * @param config   Objeto de configuração da simulação. // << NOVO PARÂMETRO
     * @return Um objeto Graph carregado do JSON.
     * @throws IOException Se ocorrer um erro na leitura do arquivo.
     * @throws Exception    Se ocorrer um erro no processamento do JSON.
     */
    public static Grafo loadGraph(String filename, Config config) throws IOException, Exception { // << ADICIONADO config
        Grafo graph = new Grafo();
        StringBuilder jsonContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }

        JSONObject json = new JSONObject(jsonContent.toString());
        processJson(json, graph, config); // << PASSAR config
        return graph;
    }

    /**
     * Método estático para carregar o grafo a partir de um InputStream.
     *
     * @param inputStream InputStream do arquivo JSON.
     * @param config      Objeto de configuração da simulação. // << NOVO PARÂMETRO
     * @return Um objeto Graph carregado do JSON.
     * @throws IOException Se ocorrer um erro na leitura do arquivo.
     * @throws Exception    Se ocorrer um erro no processamento do JSON.
     */
    public static Grafo loadGraphFromStream(InputStream inputStream, Config config) throws IOException, Exception { // << ADICIONADO config
        if (inputStream == null) {
            throw new IOException("InputStream é nulo, não foi possível localizar o arquivo JSON.");
        }

        Grafo graph = new Grafo();
        StringBuilder jsonContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }

        JSONObject json = new JSONObject(jsonContent.toString());
        processJson(json, graph, config); // << PASSAR config
        return graph;
    }

    /**
     * Processa o JSON para adicionar nós, arestas e semáforos ao grafo.
     *
     * @param json  Objeto JSON contendo os dados do grafo.
     * @param graph Instância do grafo a ser preenchida.
     * @param config Objeto de configuração da simulação. // << NOVO PARÂMETRO
     */
    private static void processJson(JSONObject json, Grafo graph, Config config) { // << ADICIONADO config
        JSONArray nodesArray = json.getJSONArray("nodes");
        for (int i = 0; i < nodesArray.length(); i++) {
            JSONObject nodeJson = nodesArray.getJSONObject(i);
            No newNode = new No(
                    nodeJson.getString("id"),
                    nodeJson.getDouble("latitude"),
                    nodeJson.getDouble("longitude"),
                    false
            );
            graph.addNode(newNode);
        }

        JSONArray edgesArray = json.getJSONArray("edges");
        for (int i = 0; i < edgesArray.length(); i++) {
            JSONObject edgeJson = edgesArray.getJSONObject(i);
            String edgeId = edgeJson.getString("id");
            String sourceNodeId = edgeJson.getString("source");
            String targetNodeId = edgeJson.getString("target");
            boolean isOneWay = edgeJson.getBoolean("oneway");
            double maxspeed = edgeJson.getDouble("maxspeed");
            double length = edgeJson.getDouble("length");
            double travelTime = (maxspeed > 0) ? (length / (maxspeed * 1000.0 / 3600.0)) : Double.POSITIVE_INFINITY;
            int capacity = (int) (maxspeed / 10); // Pode necessitar de ajuste ou vir do JSON se disponível

            Aresta forwardEdge = new Aresta(edgeId, sourceNodeId, targetNodeId, length, travelTime, isOneWay, maxspeed, capacity);
            graph.addEdge(forwardEdge);
            No sourceNode = graph.getNode(sourceNodeId);
            if (sourceNode != null) {
                sourceNode.addEdge(forwardEdge);
            } 

            if (!isOneWay) {
                String reverseEdgeId = edgeId + "_rev";
                Aresta reverseEdge = new Aresta(reverseEdgeId, targetNodeId, sourceNodeId, length, travelTime, false, maxspeed, capacity);
                graph.addEdge(reverseEdge);
                No targetNodeOriginalEdge = graph.getNode(targetNodeId);
                if (targetNodeOriginalEdge != null) {
                    targetNodeOriginalEdge.addEdge(reverseEdge);
                } 
            }
        }

        if (json.has("traffic_lights")) {
            JSONArray trafficLightsArray = json.getJSONArray("traffic_lights");
            for (int i = 0; i < trafficLightsArray.length(); i++) {
                JSONObject tlJson = trafficLightsArray.getJSONObject(i);
                String trafficLightNodeId = tlJson.getString("id");
                String direction = "unknown";
                if (tlJson.has("attributes")) {
                    JSONObject attributes = tlJson.getJSONObject("attributes");
                    if (attributes.has("traffic_signals:direction")) {
                        direction = attributes.getString("traffic_signals:direction");
                    }
                }

                // Modificação aqui: Passa o objeto 'config' para o construtor de TrafficLight
                graph.addTrafficLight(new SinalTransito(
                        trafficLightNodeId,
                        direction,
                        config // << PASSANDO O OBJETO CONFIGURATION
                ));

                No trafficNode = graph.getNode(trafficLightNodeId);
                if (trafficNode != null) {
                    trafficNode.isTrafficLight = true;
                } 
            }
        }
    }
}