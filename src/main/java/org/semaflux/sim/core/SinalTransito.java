package org.semaflux.sim.core;

import org.semaflux.sim.control.FilaAdaptativa;
import org.semaflux.sim.control.EconomiaEnergia;
import org.semaflux.sim.control.TempoFixo;
import org.semaflux.sim.simulação.Config;
import org.semaflux.sim.simulação.MudancaDeFase;
import org.semaflux.sim.control.Semaforo;

import java.util.HashMap;
import java.util.Map;

public class SinalTransito {
    private String nodeId;
    private int mode;
    private String initialJsonDirection;

    private FaseDoSemaforo currentPhase;
    private double phaseTimer;

    private Fila[] directionQueues;
    private Map<String, Integer> directionNameToIndexMap;

    private Semaforo controlStrategy;
    private boolean peakHourStatus = false;
    private Config config; // Armazena a referência para a configuração

    public SinalTransito(String nodeId, String jsonOriginalDirection, Config config) { // Recebe Configuration
        this.nodeId = nodeId;
        this.initialJsonDirection = jsonOriginalDirection != null ? jsonOriginalDirection.toLowerCase() : "unknown";
        this.config = config;
        this.mode = config.getModoSemaforo();

        this.directionQueues = new Fila[4];
        for (int i = 0; i < 4; i++) {
            this.directionQueues[i] = new Fila();
        }

        this.directionNameToIndexMap = new HashMap<>();
        directionNameToIndexMap.put("north", 0);
        directionNameToIndexMap.put("east", 1);
        directionNameToIndexMap.put("south", 2);
        directionNameToIndexMap.put("west", 3);

        switch (this.mode) {
            case 1:
                this.controlStrategy = new TempoFixo(
                        config.getFixedGreenTime(),
                        config.getFixedYellowTime(),
                        config.getFixedRedTime() // Adicionando o tempo vermelho configurável
                );
                break;
            case 2:
                this.controlStrategy = new FilaAdaptativa(
                        config.getAdaptiveVerdeBase(),
                        config.getAdaptiveAmareloBase(),
                        config.getAdaptiveMaxVerde(),       // Passando o teto MÁXIMO de verde
                        config.getAdaptiveQueueThreshold(),
                        config.getAdaptiveMinTempoVerde(),   // Passando o MÍNIMO de verde
                        config.getAdaptiveAumento(),      // Passando o incremento por veículo
                        config.getAdaptiveMinTempoVermelho(),     // Adicionando o tempo vermelho mínimo
                        config.getAdaptiveTempoMaxVermelho()      // Adicionando o tempo vermelho máximo
                );
                break;
            case 3:
                this.controlStrategy = new EconomiaEnergia(
                        config.getVerdeBaseEconomia(),
                        config.getAmareloEconomia(),
                        config.getMinimoVerdeEconomia(),
                        config.getLimiarEconomia(),
                        config.getTempoMaximoVerdeEconomia(), // Passando o teto máximo
                        config.getMinimoVermelhoEconomia(),   // Adicionando o tempo vermelho mínimo
                        config.getMaximoVermelhoEconomia()    // Adicionando o tempo vermelho máximo
                );
                break;
            default:
                this.controlStrategy = new TempoFixo(
                        config.getFixedGreenTime(),
                        config.getFixedYellowTime(),
                        config.getFixedRedTime() // Adicionando o tempo vermelho configurável
                );
                break;
        }

        if (this.controlStrategy != null) {
            this.controlStrategy.inicializar(this);
        } 

        if (this.currentPhase == null) {
            // A estratégia DEVE definir a fase inicial. Se não, logar e definir um padrão.
            setCurrentPhase(FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO, config.getFixedGreenTime());
            logPhaseChange(); // Loga a fase de fallback
        }
    }

    public String getNodeId() { return nodeId; }
    public FaseDoSemaforo getCurrentPhase() { return currentPhase; }
    public String getInitialJsonDirection() { return initialJsonDirection; }
    public boolean isPeakHourEnabled() { return peakHourStatus; }
    public Config getConfiguration() { return config; }

    public void setCurrentPhase(FaseDoSemaforo phase, double duration) {
        this.currentPhase = phase;
        this.phaseTimer = duration;
    }

    public Integer getDirectionIndex(String directionName) {
        if (directionName == null) return null;
        return directionNameToIndexMap.get(directionName.toLowerCase());
    }

    public int[] getAllQueueSizes() {
        int[] sizes = new int[4];
        for (int i = 0; i < 4; i++) {
            sizes[i] = (directionQueues[i] != null) ? directionQueues[i].size() : 0;
        }
        return sizes;
    }

    public void addVehicleToQueue(String directionName, Veiculo vehicle) {
        Integer index = getDirectionIndex(directionName);
        if (index != null && index >= 0 && index < directionQueues.length) {
            if (directionQueues[index] == null) {
                directionQueues[index] = new Fila();
            }
            directionQueues[index].enqueue(vehicle);
        }
    }

    public Veiculo popVehicleFromQueue(String directionName) {
        Integer index = getDirectionIndex(directionName);
        if (index != null && index >= 0 && index < directionQueues.length &&
                directionQueues[index] != null && !directionQueues[index].isEmpty()) {
            return directionQueues[index].dequeue();
        }
        return null;
    }

    public void update(double deltaTime, boolean isPeakHour) {
        this.peakHourStatus = isPeakHour;
        this.phaseTimer -= deltaTime;

        if (this.phaseTimer <= 0) {
            if (this.controlStrategy == null) {
                setCurrentPhase(FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO, config.getFixedGreenTime());
                logPhaseChange();
                return;
            }
            MudancaDeFase decision = controlStrategy.decidirProximaFase(this, deltaTime, getAllQueueSizes(), this.peakHourStatus);

            if (decision != null && decision.nextPhase != null) {
                setCurrentPhase(decision.nextPhase, decision.duration);
                logPhaseChange();
            } else {
                this.phaseTimer = config.getFixedGreenTime();
                if (this.currentPhase == null) {
                    setCurrentPhase(FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO, this.phaseTimer);
                    logPhaseChange();
                }
            }
        }
    }

    public String getLightStateForApproach(String approachDirection) {
        if (controlStrategy == null) {
            return "red";
        }
        return controlStrategy.getEstadoSinalParaAproximacao(this, approachDirection);
    }

    private void logPhaseChange() {
        String phaseStr = (this.currentPhase != null) ? this.currentPhase.toString() : "INDEFINIDA";
    }

    public void logCurrentInternalState() {
        String phaseStr = (this.currentPhase != null) ? this.currentPhase.toString() : "INDEFINIDA";
    }

    public synchronized int getTotalVehiclesInQueues() {
        int total = 0;
        if (directionQueues != null) {
            for (int i = 0; i < directionQueues.length; i++) {
                if (directionQueues[i] != null) {
                    total += directionQueues[i].size();
                }
            }
        }
        return total;
    }
}