package org.semaflux.sim.core;

// Representa uma interseção na rede urbana
public class No {
    public String id; // Identificador único (do OSM)
    public double latitude; // Coordenada latitudinal
    public double longitude; // Coordenada longitudinal
    public boolean isTrafficLight; // Indica se tem semáforo
    public No next; // Para lista encadeada

    private ListaLigada<Aresta> edges; // Lista de arestas conectadas ao nó (implementação personalizada)

    // Construtor
    public No(String id, double latitude, double longitude, boolean isTrafficLight) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isTrafficLight = isTrafficLight;
        this.next = null;
        this.edges = new ListaLigada<>(); // Inicializa a lista de arestas com a sua implementação personalizada
    }

    // Getter para o campo id
    public String getId() {
        return id;
    }

    // Adiciona uma aresta conectada ao nó
    public void addEdge(Aresta edge) {
        if (edge != null) {
            edges.add(edge); // Metodo add da CustomLinkedList adiciona a aresta
        }
    }

    // Retorna a lista de arestas conectadas ao nó
    public ListaLigada<Aresta> getEdges() {
        return edges; // Retorna a referência da CustomLinkedList
    }
    // Dentro da classe Node.java
// private boolean isTrafficLight; // Se você mudar para private

    public void setIsTrafficLight(boolean isTrafficLight) {
        this.isTrafficLight = isTrafficLight;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}