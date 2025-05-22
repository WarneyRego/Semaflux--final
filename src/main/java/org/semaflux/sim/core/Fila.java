package org.semaflux.sim.core;

public class Fila { // Se fosse genérica: public class CustomQueue<T extends Vehicle> ou similar
    private Veiculo front;
    private Veiculo rear;
    private int size; // Adicionado para size O(1)

    public Fila() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    public void enqueue(Veiculo vehicle) {
       
        // O campo 'next' do veículo é específico para esta estrutura de fila.
        // Se o veículo pudesse estar em outra fila ou lista encadeada que também use 'vehicle.next',
        // isso seria um problema. Mas para filas de semáforo dedicadas, é funcional.
        vehicle.next = null;

        if (isEmpty()) { // Usando o novo método isEmpty()
            front = vehicle;
            rear = vehicle;
        } else {
            rear.next = vehicle;
            rear = vehicle;
        }
        size++; // Incrementar tamanho
    }

    public Veiculo dequeue() {
        if (isEmpty()) {
            return null;
        }
        Veiculo vehicleToDequeue = front;
        front = front.next;

        if (front == null) { // Fila ficou vazia
            rear = null;
        }

        vehicleToDequeue.next = null; // Desconectar o veículo
        size--; // Decrementar tamanho
        return vehicleToDequeue;
    }

    // Agora size() é O(1)
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return front == null; // ou size == 0;
    }

    public Veiculo peek() { // Opcional: ver o primeiro sem remover
        return front;
    }
}