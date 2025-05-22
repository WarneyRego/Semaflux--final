package org.semaflux.sim.control;

import org.semaflux.sim.core.FaseDoSemaforo;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.simulação.Config;
import org.semaflux.sim.simulação.MudancaDeFase;

public class EconomiaEnergia implements Semaforo {
    private double strategyBaseGreenDuration;
    private double strategyYellowDuration;
    private double strategyMinGreenDuration;
    private int strategyLowTrafficThreshold;
    private double strategyMaxGreenDuration; // Teto máximo para o verde
    private double strategyMinRedDuration;   // Tempo mínimo vermelho
    private double strategyMaxRedDuration;   // Tempo máximo vermelho

    // Construtor padrão, se ainda necessário, deve usar valores consistentes ou buscar da config
    public EconomiaEnergia() {
        this(20.0, 3.0, 7.0, 1, 40.0, 10.0, 43.0); // Exemplo de valores default
    }
    
    public EconomiaEnergia(double baseGreen, double yellow, double minGreen, 
                               int threshold, double maxGreen) {
        this(baseGreen, yellow, minGreen, threshold, maxGreen, 
             minGreen + yellow, maxGreen + yellow); // Valores padrão para min/max vermelho
    }
    
    public EconomiaEnergia(double baseGreen, double yellow, double minGreen, 
                               int threshold, double maxGreen, 
                               double minRedTime, double maxRedTime) {
        this.strategyBaseGreenDuration = baseGreen;
        this.strategyYellowDuration = yellow;
        this.strategyMinGreenDuration = minGreen;
        this.strategyLowTrafficThreshold = threshold;
        this.strategyMaxGreenDuration = maxGreen; // Armazena o teto
        this.strategyMinRedDuration = minRedTime;
        this.strategyMaxRedDuration = maxRedTime;
    }

    @Override
    public String getEstadoSinalParaAproximacao(SinalTransito light, String approachDirection) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) {
            return "red";
        }
        
        String dir = approachDirection.toLowerCase();
        boolean isDirNS = dir.equals("north") || dir.equals("south");
        boolean isDirEW = dir.equals("east") || dir.equals("west");
        
        if (currentPhase == FaseDoSemaforo.NS_GREEN_EW_RED && isDirNS) {
            return "green";
        } else if (currentPhase == FaseDoSemaforo.NS_YELLOW_EW_RED && isDirNS) {
            return "yellow";
        } else if (currentPhase == FaseDoSemaforo.NS_RED_EW_GREEN && isDirEW) {
            return "green";
        } else if (currentPhase == FaseDoSemaforo.NS_RED_EW_YELLOW && isDirEW) {
            return "yellow";
        } else {
            return "red";
        }
    }
    
    private double calcularTempoVerdeEconomia(SinalTransito light, int[] queueSizes, boolean isEastWestPhase, boolean isPeakHour) {
        double greenTime = isPeakHour ? this.strategyBaseGreenDuration + 2.0 : this.strategyBaseGreenDuration; // Bônus de pico

        Integer relevantIndex1 = isEastWestPhase ? light.getDirectionIndex("east") : light.getDirectionIndex("north");
        Integer relevantIndex2 = isEastWestPhase ? light.getDirectionIndex("west") : light.getDirectionIndex("south");

        int trafficCount = ((relevantIndex1 != null && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0) +
                ((relevantIndex2 != null && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0);

        if (trafficCount <= this.strategyLowTrafficThreshold && !isPeakHour) {
            greenTime = this.strategyMinGreenDuration;
        }
        // Aplica o teto máximo para o verde no modo economia
        return Math.min(greenTime, this.strategyMaxGreenDuration);
    }

    @Override
    public MudancaDeFase decidirProximaFase(SinalTransito light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        FaseDoSemaforo nextPhase;
        double duration;

        if (light.getConfiguration() != null) {
            Config config = light.getConfiguration();
            this.strategyMinRedDuration = config.getEnergySavingMinRedTime();
            this.strategyMaxRedDuration = config.getEnergySavingMaxRedTime();
        }

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhase = FaseDoSemaforo.NS_YELLOW_EW_RED;
                duration = this.strategyYellowDuration;
                break;
            case NS_YELLOW_EW_RED:
                nextPhase = FaseDoSemaforo.NS_RED_EW_GREEN;
                // Calcular o tempo verde para Leste-Oeste
                // Considerando as restrições de tempo vermelho para Norte-Sul
                duration = calcularTempoVerdeEconomia(light, queueSizes, true, isPeakHour);
                // Assegurar que o tempo vermelho para Norte-Sul não seja menor que minRedDuration
                // e não seja maior que maxRedDuration
                if (duration < this.strategyMinRedDuration - this.strategyYellowDuration) {
                    duration = this.strategyMinRedDuration - this.strategyYellowDuration;
                }
                if (duration > this.strategyMaxRedDuration - this.strategyYellowDuration) {
                    duration = this.strategyMaxRedDuration - this.strategyYellowDuration;
                }
                break;
            case NS_RED_EW_GREEN:
                nextPhase = FaseDoSemaforo.NS_RED_EW_YELLOW;
                duration = this.strategyYellowDuration;
                break;
            case NS_RED_EW_YELLOW:
                nextPhase = FaseDoSemaforo.NS_GREEN_EW_RED;
                // Calcular o tempo verde para Norte-Sul
                // Considerando as restrições de tempo vermelho para Leste-Oeste
                duration = calcularTempoVerdeEconomia(light, queueSizes, false, isPeakHour);
                // Assegurar que o tempo vermelho para Leste-Oeste não seja menor que minRedDuration
                // e não seja maior que maxRedDuration
                if (duration < this.strategyMinRedDuration - this.strategyYellowDuration) {
                    duration = this.strategyMinRedDuration - this.strategyYellowDuration;
                }
                if (duration > this.strategyMaxRedDuration - this.strategyYellowDuration) {
                    duration = this.strategyMaxRedDuration - this.strategyYellowDuration;
                }
                break;
            default:
                nextPhase = FaseDoSemaforo.NS_GREEN_EW_RED;
                duration = calcularTempoVerdeEconomia(light, queueSizes, false, isPeakHour);
                break;
        }
        return new MudancaDeFase(nextPhase, duration);
    }

    @Override
    public void inicializar(SinalTransito light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        FaseDoSemaforo startPhase = FaseDoSemaforo.NS_GREEN_EW_RED;

        // Atualiza parâmetros de tempo vermelho a partir da configuração, se disponível
        if (light.getConfiguration() != null) {
            Config config = light.getConfiguration();
            this.strategyMinRedDuration = config.getEnergySavingMinRedTime();
            this.strategyMaxRedDuration = config.getEnergySavingMaxRedTime();
        }

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = FaseDoSemaforo.NS_RED_EW_GREEN;
        }

        double initialDuration = light.isPeakHourEnabled() ? this.strategyBaseGreenDuration + 2.0 : this.strategyBaseGreenDuration; // Exemplo de bônus
        initialDuration = Math.max(initialDuration, this.strategyMinGreenDuration);
        initialDuration = Math.min(initialDuration, this.strategyMaxGreenDuration); // Respeita o teto

        light.setCurrentPhase(startPhase, initialDuration);
    }
}