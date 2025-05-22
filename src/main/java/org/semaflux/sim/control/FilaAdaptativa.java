package org.semaflux.sim.control;

import org.semaflux.sim.core.FaseDoSemaforo;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.simulação.Config;
import org.semaflux.sim.simulação.MudancaDeFase;

public class FilaAdaptativa implements Semaforo {
    private double baseGreenTimeParam;
    private double yellowTimeParam;
    private double maxGreenTimeParam;      // Teto máximo absoluto para o verde
    private int queueThresholdParam;
    private double minGreenTimeParam;
    private double incrementPerVehicleParam;
    private double minRedTimeParam;        // Tempo mínimo de vermelho
    private double maxRedTimeParam;        // Tempo máximo de vermelho

    public FilaAdaptativa(double baseGreen, double yellow, double maxGreen,
                                 int threshold, double minGreen, double incrementPerVehicle,
                                 double minRedTime, double maxRedTime) {
        this.baseGreenTimeParam = baseGreen;
        this.yellowTimeParam = yellow;
        this.maxGreenTimeParam = maxGreen; // Teto máximo para o tempo verde total
        this.queueThresholdParam = threshold;
        this.minGreenTimeParam = minGreen;
        this.incrementPerVehicleParam = incrementPerVehicle;
        this.minRedTimeParam = minRedTime;
        this.maxRedTimeParam = maxRedTime;
    }

    public FilaAdaptativa(double baseGreen, double yellow, double maxGreen,
                                 int threshold, double minGreen, double incrementPerVehicle) {
        this(baseGreen, yellow, maxGreen, threshold, minGreen, incrementPerVehicle, 
             baseGreen + yellow, maxGreen + yellow); // Valores padrão para min/max vermelho
    }

    @Override
    public void inicializar(SinalTransito light) {
        Config config = light.getConfiguration(); // Acessa a configuração
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        FaseDoSemaforo startPhase = FaseDoSemaforo.NS_GREEN_EW_RED;

        // Atualiza parâmetros de tempo vermelho a partir da configuração, se disponível
        if (config != null) {
            this.minRedTimeParam = config.getAdaptiveMinRedTime();
            this.maxRedTimeParam = config.getAdaptiveMaxRedTime();
        }

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = FaseDoSemaforo.NS_RED_EW_GREEN;
        }

        double initialDuration = light.isPeakHourEnabled() ? this.baseGreenTimeParam + 5.0 : this.baseGreenTimeParam; // Bônus de pico
        initialDuration = Math.max(initialDuration, this.minGreenTimeParam);
        initialDuration = Math.min(initialDuration, this.maxGreenTimeParam); // Não exceder o teto máximo

        light.setCurrentPhase(startPhase, initialDuration);
        // Não precisa logar a mudança de fase aqui, o TrafficLight.update fará isso.
    }

    @Override
    public MudancaDeFase decidirProximaFase(SinalTransito light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        FaseDoSemaforo nextPhaseDetermined;
        double durationDetermined;

        // Atualiza os parâmetros com base na configuração atual
        if (light.getConfiguration() != null) {
            Config config = light.getConfiguration();
            this.minRedTimeParam = config.getAdaptiveMinRedTime();
            this.maxRedTimeParam = config.getAdaptiveMaxRedTime();
        }

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhaseDetermined = FaseDoSemaforo.NS_YELLOW_EW_RED;
                durationDetermined = this.yellowTimeParam;
                break;
            case NS_YELLOW_EW_RED:
                nextPhaseDetermined = FaseDoSemaforo.NS_RED_EW_GREEN;
                // Calcular o tempo verde adaptativo para Leste-Oeste
                // Considerando as restrições de tempo vermelho para Norte-Sul
                durationDetermined = calcularTempoVerdeAdaptativo(light, queueSizes, true, isPeakHour);
                // Assegurar que o tempo vermelho para Norte-Sul não seja menor que minRedTimeParam
                // e não seja maior que maxRedTimeParam
                if (durationDetermined < this.minRedTimeParam - this.yellowTimeParam) {
                    durationDetermined = this.minRedTimeParam - this.yellowTimeParam;
                }
                if (durationDetermined > this.maxRedTimeParam - this.yellowTimeParam) {
                    durationDetermined = this.maxRedTimeParam - this.yellowTimeParam;
                }
                break;
            case NS_RED_EW_GREEN:
                nextPhaseDetermined = FaseDoSemaforo.NS_RED_EW_YELLOW;
                durationDetermined = this.yellowTimeParam;
                break;
            case NS_RED_EW_YELLOW:
                nextPhaseDetermined = FaseDoSemaforo.NS_GREEN_EW_RED;
                // Calcular o tempo verde adaptativo para Norte-Sul
                // Considerando as restrições de tempo vermelho para Leste-Oeste
                durationDetermined = calcularTempoVerdeAdaptativo(light, queueSizes, false, isPeakHour);
                // Assegurar que o tempo vermelho para Leste-Oeste não seja menor que minRedTimeParam
                // e não seja maior que maxRedTimeParam
                if (durationDetermined < this.minRedTimeParam - this.yellowTimeParam) {
                    durationDetermined = this.minRedTimeParam - this.yellowTimeParam;
                }
                if (durationDetermined > this.maxRedTimeParam - this.yellowTimeParam) {
                    durationDetermined = this.maxRedTimeParam - this.yellowTimeParam;
                }
                break;
            default:
                nextPhaseDetermined = FaseDoSemaforo.NS_GREEN_EW_RED;
                durationDetermined = calcularTempoVerdeAdaptativo(light, queueSizes, false, isPeakHour);
                break;
        }
        return new MudancaDeFase(nextPhaseDetermined, durationDetermined);
    }

    private double calcularTempoVerdeAdaptativo(SinalTransito light, int[] queueSizes, boolean isEastWestGreenPhase, boolean isPeakHour) {
        double adaptiveGreenDuration = isPeakHour ? this.baseGreenTimeParam + 5.0 : this.baseGreenTimeParam;

        Integer relevantIndex1 = isEastWestGreenPhase ? light.getDirectionIndex("east") : light.getDirectionIndex("north");
        Integer relevantIndex2 = isEastWestGreenPhase ? light.getDirectionIndex("west") : light.getDirectionIndex("south");

        int relevantQueue1Size = (relevantIndex1 != null && relevantIndex1 >= 0 && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0;
        int relevantQueue2Size = (relevantIndex2 != null && relevantIndex2 >= 0 && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0;
        int maxRelevantQueue = Math.max(relevantQueue1Size, relevantQueue2Size);

        if (maxRelevantQueue == 0 && !isPeakHour) {
            return Math.max(this.minGreenTimeParam, adaptiveGreenDuration * 0.66);
        }

        if (maxRelevantQueue > this.queueThresholdParam) {
            double extension = (maxRelevantQueue - this.queueThresholdParam) * this.incrementPerVehicleParam;
            adaptiveGreenDuration += extension;
        }

        adaptiveGreenDuration = Math.min(adaptiveGreenDuration, this.maxGreenTimeParam);
        adaptiveGreenDuration = Math.max(adaptiveGreenDuration, this.minGreenTimeParam);

        return adaptiveGreenDuration;
    }

    @Override
    public String getEstadoSinalParaAproximacao(SinalTransito light, String approachDirection) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) return "red";
        String dir = approachDirection.toLowerCase();

        switch (currentPhase) {
            case NS_GREEN_EW_RED: return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NS_YELLOW_EW_RED: return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NS_RED_EW_GREEN: return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NS_RED_EW_YELLOW: return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default: return "red";
        }
    }
}