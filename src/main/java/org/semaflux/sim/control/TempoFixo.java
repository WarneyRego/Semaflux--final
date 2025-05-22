package org.semaflux.sim.control;

import org.semaflux.sim.core.FaseDoSemaforo;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.simulação.MudancaDeFase;

public class TempoFixo implements Semaforo {

    private double strategyGreenDuration;
    private double strategyYellowDuration;
    private double strategyRedDuration; // Novo parâmetro para duração do vermelho

    public TempoFixo(double greenTime, double yellowTime, double redTime) {
        this.strategyGreenDuration = greenTime;
        this.strategyYellowDuration = yellowTime;
        this.strategyRedDuration = redTime;
    }

    public TempoFixo(double greenTime, double yellowTime) {
        this.strategyGreenDuration = greenTime;
        this.strategyYellowDuration = yellowTime;
        this.strategyRedDuration = greenTime + yellowTime; // Por padrão, o tempo vermelho é igual ao tempo verde + amarelo
    }

    public TempoFixo() {
        this.strategyGreenDuration = 15.0;
        this.strategyYellowDuration = 3.0;
        this.strategyRedDuration = 18.0; // 15.0 + 3.0
    }

    @Override
    public void inicializar(SinalTransito light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        FaseDoSemaforo startPhase = FaseDoSemaforo.NS_GREEN_EW_RED; // Padrão
        
        // Ajusta a duração inicial com base na configuração
        double initialDuration = light.isPeakHourEnabled() ? 20.0 : this.strategyGreenDuration;
        
        // Use o tempo vermelho da configuração, se disponível
        if (light.getConfiguration() != null) {
            this.strategyRedDuration = light.getConfiguration().getFixedRedTime();
        }

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = FaseDoSemaforo.NS_RED_EW_GREEN;
        }
        // Para "forward" ou "backward", a lógica para determinar a orientação principal
        // (N-S ou L-O) precisaria de mais informações (ex: geometria do grafo).
        // Por enquanto, o fallback é iniciar com NS_GREEN_EW_RED.

        light.setCurrentPhase(startPhase, initialDuration);
    }

    @Override
    public MudancaDeFase decidirProximaFase(SinalTransito light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        FaseDoSemaforo nextPhase;
        double duration;

        // Atualiza os tempos com base na configuração atual
        if (light.getConfiguration() != null) {
            this.strategyGreenDuration = light.getConfiguration().getFixedGreenTime();
            this.strategyYellowDuration = light.getConfiguration().getFixedYellowTime();
            this.strategyRedDuration = light.getConfiguration().getFixedRedTime();
        }

        double activeGreenDuration = isPeakHour ? 20.0 : this.strategyGreenDuration;
        double activeYellowDuration = this.strategyYellowDuration;
        // Note que activeRedDuration não é usado diretamente no ciclo fixo, mas poderia ser usado
        // para ajustar os tempos das fases perpendiculares
        
        // Lógica de ciclo fixo
        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhase = FaseDoSemaforo.NS_YELLOW_EW_RED;
                duration = activeYellowDuration;
                break;
            case NS_YELLOW_EW_RED:
                nextPhase = FaseDoSemaforo.NS_RED_EW_GREEN;
                // Usar o tempo verde configurado para a direção perpendicular
                duration = activeGreenDuration; 
                break;
            case NS_RED_EW_GREEN:
                nextPhase = FaseDoSemaforo.NS_RED_EW_YELLOW;
                duration = activeYellowDuration;
                break;
            case NS_RED_EW_YELLOW:
                nextPhase = FaseDoSemaforo.NS_GREEN_EW_RED;
                // Usar o tempo verde configurado para a direção perpendicular
                duration = activeGreenDuration; 
                break;
            default:
                nextPhase = FaseDoSemaforo.NS_GREEN_EW_RED;
                duration = activeGreenDuration;
                break;
        }
        return new MudancaDeFase(nextPhase, duration);
    }

    @Override
    public String getEstadoSinalParaAproximacao(SinalTransito light, String approachDirection) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) return "red";
        String dir = approachDirection.toLowerCase();

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NS_YELLOW_EW_RED:
                return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NS_RED_EW_GREEN:
                return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NS_RED_EW_YELLOW:
                return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default:
                return "red";
        }
    }
}