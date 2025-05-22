package org.semaflux.sim.control;

import org.semaflux.sim.core.*;
import org.semaflux.sim.simulação.Config;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

public class leitorJson {

    public static Grafo carregarGrafoDoFluxo(InputStream inputStream, Config config) throws IOException, Exception {
        if (inputStream == null) {
            throw new IOException("InputStream é nulo, não foi possível localizar o arquivo JSON.");
        }

        Grafo grafo = new Grafo();
        StringBuilder conteudoJson = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                conteudoJson.append(linha);
            }
        }

        JSONObject json = new JSONObject(conteudoJson.toString());
        processarJson(json, grafo, config);
        return grafo;
    }

    public static Grafo carregarGrafo(String nomeArquivo, Config config) throws IOException, Exception {
        Grafo grafo = new Grafo();
        StringBuilder conteudoJson = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(nomeArquivo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                conteudoJson.append(linha);
            }
        }

        JSONObject json = new JSONObject(conteudoJson.toString());
        processarJson(json, grafo, config);
        return grafo;
    }

    private static void processarJson(JSONObject json, Grafo grafo, Config config) {
        // Processar nós
        processarNos(json, grafo);
        
        // Processar arestas
        processarArestas(json, grafo);
        
        // Processar semáforos
        processarSemaforos(json, grafo, config);
    }
    
    private static void processarNos(JSONObject json, Grafo grafo) {
        JSONArray arrayNos = json.getJSONArray("nodes");
        for (int i = 0; i < arrayNos.length(); i++) {
            JSONObject jsonNo = arrayNos.getJSONObject(i);
            No novoNo = new No(
                    jsonNo.getString("id"),
                    jsonNo.getDouble("latitude"),
                    jsonNo.getDouble("longitude"),
                    false
            );
            grafo.addNode(novoNo);
        }
    }
    
    private static void processarArestas(JSONObject json, Grafo grafo) {
        JSONArray arrayArestas = json.getJSONArray("edges");
        for (int i = 0; i < arrayArestas.length(); i++) {
            JSONObject jsonAresta = arrayArestas.getJSONObject(i);
            String idAresta = jsonAresta.getString("id");
            String idNoOrigem = jsonAresta.getString("source");
            String idNoDestino = jsonAresta.getString("target");
            boolean unicaSentido = jsonAresta.getBoolean("oneway");
            double velocidadeMax = jsonAresta.getDouble("maxspeed");
            double comprimento = jsonAresta.getDouble("length");
            double tempoViagem = (velocidadeMax > 0) ? (comprimento / (velocidadeMax * 1000.0 / 3600.0)) : Double.POSITIVE_INFINITY;
            int capacidade = (int) (velocidadeMax / 10);

            Aresta arestaForward = new Aresta(idAresta, idNoOrigem, idNoDestino, comprimento, tempoViagem, unicaSentido, velocidadeMax, capacidade);
            grafo.addEdge(arestaForward);
            No noOrigem = grafo.getNode(idNoOrigem);
            if (noOrigem != null) {
                noOrigem.addEdge(arestaForward);
            } 

            if (!unicaSentido) {
                String idArestaReversa = idAresta + "_rev";
                Aresta arestaReversa = new Aresta(idArestaReversa, idNoDestino, idNoOrigem, comprimento, tempoViagem, false, velocidadeMax, capacidade);
                grafo.addEdge(arestaReversa);
                No noDestino = grafo.getNode(idNoDestino);
                if (noDestino != null) {
                    noDestino.addEdge(arestaReversa);
                } 
            }
        }
    }
    
    private static void processarSemaforos(JSONObject json, Grafo grafo, Config config) {
        if (json.has("traffic_lights")) {
            JSONArray arraySemaforos = json.getJSONArray("traffic_lights");
            for (int i = 0; i < arraySemaforos.length(); i++) {
                JSONObject jsonSemaforo = arraySemaforos.getJSONObject(i);
                String idNoSemaforo = jsonSemaforo.getString("id");
                String direcao = "unknown";
                if (jsonSemaforo.has("attributes")) {
                    JSONObject atributos = jsonSemaforo.getJSONObject("attributes");
                    if (atributos.has("traffic_signals:direction")) {
                        direcao = atributos.getString("traffic_signals:direction");
                    }
                }

                grafo.addTrafficLight(new SinalTransito(
                        idNoSemaforo,
                        direcao,
                        config
                ));

                No noSemaforo = grafo.getNode(idNoSemaforo);
                if (noSemaforo != null) {
                    noSemaforo.isTrafficLight = true;
                } 
            }
        }
    }
}