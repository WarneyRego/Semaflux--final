package org.semaflux.sim.control;

import org.semaflux.sim.core.FaseDoSemaforo;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.simulação.MudancaDeFase;

public class TempoFixo implements Semaforo {

    private double strategyGreenDuration;
    private double strategyYellowDuration;
    private double strategyRedDuration; // Novo parâmetro para duração do vermelho

    public TempoFixo() {
        this.strategyGreenDuration = 15.0;
        this.strategyYellowDuration = 3.0;
        this.strategyRedDuration = 18.0; // 15.0 + 3.0
    }

    public TempoFixo(double greenTime, double yellowTime) {
        this.strategyGreenDuration = greenTime;
        this.strategyYellowDuration = yellowTime;
        this.strategyRedDuration = greenTime + yellowTime; // Por padrão, o tempo vermelho é igual ao tempo verde + amarelo
    }

    public TempoFixo(double greenTime, double yellowTime, double redTime) {
        this.strategyGreenDuration = greenTime;
        this.strategyYellowDuration = yellowTime;
        this.strategyRedDuration = redTime;
    }

    @Override
    public String getEstadoSinalParaAproximacao(SinalTransito light, String approachDirection) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) return "red";
        String dir = approachDirection.toLowerCase();

        switch (currentPhase) {
            case NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO:
                return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO:
                return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE:
                return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO:
                return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default:
                return "red";
        }
    }

    @Override
    public void inicializar(SinalTransito light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        FaseDoSemaforo startPhase = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO; // Padrão
        
        // Ajusta a duração inicial com base na configuração
        double initialDuration = light.isPeakHourEnabled() ? 20.0 : this.strategyGreenDuration;
        
        // Use o tempo vermelho da configuração, se disponível
        if (light.getConfiguration() != null) {
            this.strategyRedDuration = light.getConfiguration().getFixedRedTime();
        }

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE;
        }

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
            case NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO:
                nextPhase = FaseDoSemaforo.NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO;
                duration = activeYellowDuration;
                break;
            case NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO:
                nextPhase = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE;
                // Usar o tempo verde configurado para a direção perpendicular
                duration = activeGreenDuration; 
                break;
            case NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE:
                nextPhase = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO;
                duration = activeYellowDuration;
                break;
            case NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO:
                nextPhase = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO;
                // Usar o tempo verde configurado para a direção perpendicular
                duration = activeGreenDuration; 
                break;
            default:
                nextPhase = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO;
                duration = activeGreenDuration;
                break;
        }
        return new MudancaDeFase(nextPhase, duration);
    }
}