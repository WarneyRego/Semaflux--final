package org.semaflux.sim.simulação;

import java.util.ArrayList;
import java.util.List;

import org.semaflux.sim.core.Grafo;
import org.semaflux.sim.core.ListaLigada;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.core.Veiculo;

/**
 * Coleta e calcula estatísticas da simulação.
 * Esta classe rastreia o número de veículos gerados, veículos que chegaram ao destino,
 * tempos totais de viagem e espera, consumo total de combustível, e um índice de
 * congestionamento dinâmico.
 */
public class Estatisticas {
    private int vehiclesGenerated;
    private int vehiclesArrived;
    private double totalTravelTime;
    private double totalWaitTime;
    private double totalFuelConsumed;
    private double currentTime;

    private double currentCongestionIndex;
    private double maxRecordedCongestionRatio; // Para guardar o pico de congestionamento normalizado
    
    // Histórico de dados para gráficos
    private List<Double> timeHistory = new ArrayList<>();
    private List<Double> congestionHistory = new ArrayList<>();
    private List<Integer> activeVehiclesHistory = new ArrayList<>();
    private List<Double> waitTimeHistory = new ArrayList<>();
    private List<Double> fuelConsumptionHistory = new ArrayList<>();
    private double maxTravelTime = 0.0;
    private double maxWaitTime = 0.0;

    /**
     * Construtor padrão para a classe Statistics.
     * Inicializa todas as contagens e totais em zero.
     */
    public Estatisticas() {
        this.vehiclesGenerated = 0;
        this.vehiclesArrived = 0;
        this.totalTravelTime = 0.0;
        this.totalWaitTime = 0.0;
        this.totalFuelConsumed = 0.0;
        this.currentTime = 0.0;
        this.currentCongestionIndex = 0.0;
        this.maxRecordedCongestionRatio = 0.0;
        
        // Inicializar os históricos com um ponto zero
        this.timeHistory.add(0.0);
        this.congestionHistory.add(0.0);
        this.activeVehiclesHistory.add(0);
        this.waitTimeHistory.add(0.0);
        this.fuelConsumptionHistory.add(0.0);
    }

    /**
     * Incrementa o contador de veículos gerados.
     * Deve ser chamado sempre que um novo veículo é introduzido na simulação.
     */
    public synchronized void vehicleGenerated() {
        this.vehiclesGenerated++;
    }

    /**
     * Registra a chegada de um veículo ao seu destino e acumula suas estatísticas.
     *
     * @param travelTime O tempo total de viagem do veículo.
     * @param waitTime O tempo total que o veículo passou esperando.
     * @param fuelConsumedByVehicle O total de combustível consumido pelo veículo.
     */
    public synchronized void vehicleArrived(double travelTime, double waitTime, double fuelConsumedByVehicle) {
        this.vehiclesArrived++;
        this.totalTravelTime += travelTime;
        this.totalWaitTime += waitTime;
        this.totalFuelConsumed += fuelConsumedByVehicle;
        
        // Atualiza valores máximos
        if (travelTime > maxTravelTime) {
            maxTravelTime = travelTime;
        }
        
        if (waitTime > maxWaitTime) {
            maxWaitTime = waitTime;
        }
    }

    /**
     * Atualiza o tempo corrente da simulação para referência nas estatísticas.
     * @param time O tempo atual da simulação.
     */
    public synchronized void updateCurrentTime(double time) {
        this.currentTime = time;
    }

    /**
     * Calcula e atualiza o índice de congestionamento atual da simulação.
     * <p>
     * A métrica de congestionamento é calculada considerando dois componentes principais:
     * <ol>
     * <li><b>Densidade de Veículos:</b> A proporção de veículos ativos em relação ao número total de nós no grafo.
     * Um valor mais alto aqui sugere que a rede está mais "cheia".</li>
     * <li><b>Proporção de Veículos Enfileirados:</b> A porcentagem de veículos ativos que estão atualmente
     * parados em filas de semáforos. Um valor alto aqui indica que muitos veículos estão parados.</li>
     * </ol>
     * Estes dois componentes são combinados, com um peso maior dado à proporção de veículos enfileirados,
     * para gerar um índice de congestionamento normalizado (idealmente entre 0 e 1, mas pode exceder 1
     * em situações extremas se a normalização não for perfeita ou se a rede estiver supersaturada).
     * <p>
     * O método também rastreia o {@code maxRecordedCongestionRatio}.
     *
     * @param activeVehicles Lista de todos os veículos atualmente ativos na simulação.
     * @param graph          O grafo da rede urbana, usado para acessar semáforos e o número total de nós.
     */
    public synchronized void calculateCurrentCongestion(ListaLigada<Veiculo> activeVehicles, Grafo graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty() || activeVehicles == null) {
            this.currentCongestionIndex = 0.0;
            return;
        }

        int numberOfActiveVehicles = activeVehicles.size();
        int totalNodes = graph.getNodes().size();
        int totalQueuedVehicles = 0;

        if (graph.getTrafficLights() != null) {
            for (SinalTransito tl : graph.getTrafficLights()) {
                if (tl != null) {
                    totalQueuedVehicles += tl.getTotalVehiclesInQueues();
                }
            }
        }

        if (totalNodes == 0) { // Evita divisão por zero se, por algum motivo, não houver nós
            this.currentCongestionIndex = numberOfActiveVehicles + totalQueuedVehicles; // Fallback para a métrica antiga
            return;
        }

        // Componente 1: Densidade de veículos na rede (0 a 1, pode ser > 1 se muitos carros por nó)
        double vehicleDensityRatio = (double) numberOfActiveVehicles / totalNodes;
        
        // Limita a densidade para evitar valores muito altos em redes pequenas
        vehicleDensityRatio = Math.min(1.0, vehicleDensityRatio);

        // Componente 2: Proporção de veículos ativos que estão em filas (0 a 1)
        double queuedVehicleRatio = (numberOfActiveVehicles > 0) ? (double) totalQueuedVehicles / numberOfActiveVehicles : 0.0;
        
        // Limita a proporção de enfileirados para evitar valores acima de 1
        queuedVehicleRatio = Math.min(1.0, queuedVehicleRatio);

        // Combinação ponderada para calcular o índice de congestionamento
        // Pesos ajustados: 40% da densidade, 60% da proporção em fila
        double rawCongestionScore = (0.4 * vehicleDensityRatio) + (0.6 * queuedVehicleRatio);

        // Garante um valor entre 0 e 100%
        this.currentCongestionIndex = Math.min(100.0, rawCongestionScore * 100);

        // Atualiza o pico de congestionamento
        if (this.currentCongestionIndex > this.maxRecordedCongestionRatio) {
            this.maxRecordedCongestionRatio = this.currentCongestionIndex;
        }
        
        // Registrar dados para histórico a cada segundo para ter mais pontos nos gráficos
        if (Math.round(currentTime) % 1 == 0) {
            timeHistory.add(currentTime);
            congestionHistory.add(currentCongestionIndex);
            activeVehiclesHistory.add(numberOfActiveVehicles);
            waitTimeHistory.add(getAverageWaitTime());
            fuelConsumptionHistory.add(totalFuelConsumed);
        }
    }

    /**
     * Retorna informações sobre o tamanho dos históricos coletados
     * @return String com informações de depuração
     */
    public String getDebugInfo() {
        return "Pontos coletados: Tempo=" + timeHistory.size() + 
               ", Congestionamento=" + congestionHistory.size() + 
               ", Veículos=" + activeVehiclesHistory.size() + 
               ", Espera=" + waitTimeHistory.size() + 
               ", Combustível=" + fuelConsumptionHistory.size();
    }

    /**
     * Retorna o índice de congestionamento calculado mais recentemente, como uma porcentagem (0-100).
     * @return O índice de congestionamento atual como porcentagem.
     */
    public synchronized double getCurrentCongestionIndex() {
        return this.currentCongestionIndex;
    }

    /**
     * Retorna o maior índice de congestionamento (como porcentagem) registrado durante a simulação.
     * @return O pico de congestionamento registrado.
     */
    public synchronized double getMaxRecordedCongestionRatio() {
        return maxRecordedCongestionRatio;
    }

    /**
     * Retorna o número total de veículos gerados durante a simulação.
     * @return O total de veículos gerados.
     */
    public synchronized int getTotalVehiclesGenerated() {
        return vehiclesGenerated;
    }

    /**
     * Retorna o número de veículos que chegaram aos seus destinos.
     * @return O total de veículos que chegaram.
     */
    public synchronized int getArrivedCount() {
        return vehiclesArrived;
    }

    /**
     * Calcula e retorna o tempo médio de viagem para os veículos que chegaram ao destino.
     * @return O tempo médio de viagem em segundos, ou 0.0 se nenhum veículo chegou.
     */
    public synchronized double getAverageTravelTime() {
        if (vehiclesArrived == 0) {
            return 0.0;
        }
        return totalTravelTime / vehiclesArrived;
    }

    /**
     * Calcula e retorna o tempo médio de espera para os veículos que chegaram ao destino.
     * @return O tempo médio de espera em segundos, ou 0.0 se nenhum veículo chegou.
     */
    public synchronized double getAverageWaitTime() {
        if (vehiclesArrived == 0) {
            return 0.0;
        }
        return totalWaitTime / vehiclesArrived;
    }

    /**
     * Retorna o consumo total de combustível acumulado de todos os veículos que chegaram ao destino.
     * @return O consumo total de combustível na unidade definida (ex: litros).
     */
    public synchronized double getTotalFuelConsumed() {
        return totalFuelConsumed;
    }

    /**
     * Calcula e retorna o consumo médio de combustível por veículo que chegou ao destino.
     * @return O consumo médio de combustível (ex: em litros), ou 0.0 se nenhum veículo chegou.
     */
    public synchronized double getAverageFuelConsumptionPerVehicle() {
        if (vehiclesArrived == 0) {
            return 0.0;
        }
        return totalFuelConsumed / vehiclesArrived;
    }

    public int getVehiclesArrived() {
        return vehiclesArrived;
    }

    public double getCurrentTime() {
        return currentTime;
    }
    
    /**
     * Retorna o tempo máximo de viagem de qualquer veículo durante a simulação.
     * @return O tempo máximo de viagem em segundos.
     */
    public synchronized double getMaxTravelTime() {
        return maxTravelTime;
    }
    
    /**
     * Retorna o tempo máximo de espera de qualquer veículo durante a simulação.
     * @return O tempo máximo de espera em segundos.
     */
    public synchronized double getMaxWaitTime() {
        return maxWaitTime;
    }
    
    /**
     * Retorna o histórico de tempos da simulação para gráficos.
     * @return Lista com pontos de tempo da simulação.
     */
    public List<Double> getTimeHistory() {
        return new ArrayList<>(timeHistory); // Retorna uma cópia para evitar modificações externas
    }
    
    /**
     * Retorna o histórico de valores de congestionamento para gráficos.
     * @return Lista com níveis de congestionamento ao longo do tempo.
     */
    public List<Double> getCongestionHistory() {
        return new ArrayList<>(congestionHistory);
    }
    
    /**
     * Retorna o histórico de veículos ativos para gráficos.
     * @return Lista com quantidade de veículos ativos ao longo do tempo.
     */
    public List<Integer> getActiveVehiclesHistory() {
        return new ArrayList<>(activeVehiclesHistory);
    }
    
    /**
     * Retorna o histórico de tempo médio de espera para gráficos.
     * @return Lista com tempo médio de espera ao longo do tempo.
     */
    public List<Double> getWaitTimeHistory() {
        return new ArrayList<>(waitTimeHistory);
    }
    
    /**
     * Retorna o histórico de consumo de combustível para gráficos.
     * @return Lista com consumo acumulado de combustível ao longo do tempo.
     */
    public List<Double> getFuelConsumptionHistory() {
        return new ArrayList<>(fuelConsumptionHistory);
    }
    
    /**
     * Calcula e retorna a taxa de veículos que chegaram ao destino em relação aos gerados.
     * @return Taxa de chegada como percentual (0-100%).
     */
    public synchronized double getArrivalRate() {
        if (vehiclesGenerated == 0) {
            return 0.0;
        }
        return (double) vehiclesArrived / vehiclesGenerated * 100.0;
    }

    /**
     * Imprime um resumo das estatísticas da simulação no console.
     * Inclui informações sobre veículos, tempos médios, consumo de combustível e pico de congestionamento.
     */
    public synchronized void printSummary() {
        // Resumo das estatísticas da simulação disponível através dos métodos getters
    }

    /**
     * Calcula e retorna a média de congestionamento durante a simulação
     * @return A média de congestionamento como porcentagem (0-100%)
     */
    public synchronized double getAverageCongestionRatio() {
        if (congestionHistory.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (Double value : congestionHistory) {
            sum += value;
        }
        
        return sum / congestionHistory.size();
    }
}