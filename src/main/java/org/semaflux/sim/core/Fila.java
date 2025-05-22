package org.semaflux.sim.core;

public class Fila { 
    private int size;
    private Veiculo back;
    private Veiculo front;

    public Fila() {
        this.size = 0;
        this.back = null;
        this.front = null;
    }

    public boolean isEmpty() {
        return front == null; 
    }
    
    public int size() {
        return size;
    }
    
    public Veiculo peek() { 
        return front;
    }

    public void enqueue(Veiculo vehicle) {
        vehicle.next = null;

        if (isEmpty()) {
            back = vehicle;
            front = vehicle;
        } else {
            back.next = vehicle;
            back = vehicle;
        }
        size++; 
    }

    public Veiculo dequeue() {
        if (isEmpty()) {
            return null;
        }
        Veiculo vehicleToDequeue = front;
        front = front.next;

        if (front == null) { 
            back = null;
        }

        vehicleToDequeue.next = null; 
        size--; 
        return vehicleToDequeue;
    }
}