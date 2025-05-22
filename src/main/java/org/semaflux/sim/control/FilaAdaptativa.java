package org.semaflux.sim.control;

import org.semaflux.sim.core.FaseDoSemaforo;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.simulação.Config;
import org.semaflux.sim.simulação.MudancaDeFase;

public class FilaAdaptativa implements Semaforo {
    private double TempoVerdeBase;
    private double TempoAmarelo;
    private double TempoMaximoVerde;     
    private int tempoTransicao;
    private double TempoMinimoVerde;
    private double tempoExtraPorVeiculo;
    private double TempoMinimoVermelho;       
    private double TempoMaximoVermelho;        

    public FilaAdaptativa(double verdebase, double amarelo, double maxVerde,
                                 int trans, double minVerde, double extraVeiculo,
                                 double minVermelho, double maxVermelho) {
        this.TempoVerdeBase = verdebase;
        this.TempoAmarelo = amarelo;
        this.TempoMaximoVerde = maxVerde; 
        this.tempoTransicao = trans;
        this.TempoMinimoVerde = minVerde;
        this.tempoExtraPorVeiculo = extraVeiculo;
        this.TempoMinimoVermelho = minVermelho;
        this.TempoMaximoVermelho = maxVermelho;
    }

    public FilaAdaptativa(double baseGreen, double yellow, double maxGreen,
                                 int threshold, double minGreen, double incrementPerVehicle) {
        this(baseGreen, yellow, maxGreen, threshold, minGreen, incrementPerVehicle, 
             baseGreen + yellow, maxGreen + yellow); 
    }

    @Override
    public String getEstadoSinalParaAproximacao(SinalTransito light, String approachDirection) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) return "red";
        String dir = approachDirection.toLowerCase();

        switch (currentPhase) {
            case NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO: return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO: return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE: return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO: return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default: return "red";
        }
    }

    private double calcularTempoVerdeAdaptativo(SinalTransito light, int[] queueSizes, boolean isEastWestGreenPhase, boolean isPeakHour) {
        double adaptiveGreenDuration = isPeakHour ? this.TempoVerdeBase + 5.0 : this.TempoVerdeBase;

        Integer relevantIndex1 = isEastWestGreenPhase ? light.getDirectionIndex("east") : light.getDirectionIndex("north");
        Integer relevantIndex2 = isEastWestGreenPhase ? light.getDirectionIndex("west") : light.getDirectionIndex("south");

        int relevantQueue1Size = (relevantIndex1 != null && relevantIndex1 >= 0 && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0;
        int relevantQueue2Size = (relevantIndex2 != null && relevantIndex2 >= 0 && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0;
        int maxRelevantQueue = Math.max(relevantQueue1Size, relevantQueue2Size);

        if (maxRelevantQueue == 0 && !isPeakHour) {
            return Math.max(this.TempoMinimoVerde, adaptiveGreenDuration * 0.66);
        }

        if (maxRelevantQueue > this.tempoTransicao) {
            double extension = (maxRelevantQueue - this.tempoTransicao) * this.tempoExtraPorVeiculo;
            adaptiveGreenDuration += extension;
        }

        adaptiveGreenDuration = Math.min(adaptiveGreenDuration, this.TempoMaximoVerde);
        adaptiveGreenDuration = Math.max(adaptiveGreenDuration, this.TempoMinimoVerde);

        return adaptiveGreenDuration;
    }

    @Override
    public void inicializar(SinalTransito light) {
        Config config = light.getConfiguration(); 
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        FaseDoSemaforo startPhase = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO;

        if (config != null) {
            this.TempoMinimoVermelho = config.getAdaptiveMinTempoVermelho();
            this.TempoMaximoVermelho = config.getAdaptiveTempoMaxVermelho();
        }

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE;
        }

        double initialDuration = light.isPeakHourEnabled() ? this.TempoVerdeBase + 5.0 : this.TempoVerdeBase; 
        initialDuration = Math.max(initialDuration, this.TempoMinimoVerde);
        initialDuration = Math.min(initialDuration, this.TempoMaximoVerde); 

        light.setCurrentPhase(startPhase, initialDuration);
    }

    @Override
    public MudancaDeFase decidirProximaFase(SinalTransito light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        FaseDoSemaforo nextPhaseDetermined;
        double durationDetermined;

        if (light.getConfiguration() != null) {
            Config config = light.getConfiguration();
            this.TempoMinimoVermelho = config.getAdaptiveMinTempoVermelho();
            this.TempoMaximoVermelho = config.getAdaptiveTempoMaxVermelho();
        }

        switch (currentPhase) {
            case NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO:
                nextPhaseDetermined = FaseDoSemaforo.NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO;
                durationDetermined = this.TempoAmarelo;
                break;
            case NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO:
                nextPhaseDetermined = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE;
           
                durationDetermined = calcularTempoVerdeAdaptativo(light, queueSizes, true, isPeakHour);
          
                if (durationDetermined < this.TempoMinimoVermelho - this.TempoAmarelo) {
                    durationDetermined = this.TempoMinimoVermelho - this.TempoAmarelo;
                }
                if (durationDetermined > this.TempoMaximoVermelho - this.TempoAmarelo) {
                    durationDetermined = this.TempoMaximoVermelho - this.TempoAmarelo;
                }
                break;
            case NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE:
                nextPhaseDetermined = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO;
                durationDetermined = this.TempoAmarelo;
                break;
            case NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO:
                nextPhaseDetermined = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO;
           
                durationDetermined = calcularTempoVerdeAdaptativo(light, queueSizes, false, isPeakHour);
               
                if (durationDetermined < this.TempoMinimoVermelho - this.TempoAmarelo) {
                    durationDetermined = this.TempoMinimoVermelho - this.TempoAmarelo;
                }
                if (durationDetermined > this.TempoMaximoVermelho - this.TempoAmarelo) {
                    durationDetermined = this.TempoMaximoVermelho - this.TempoAmarelo;
                }
                break;
            default:
                nextPhaseDetermined = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO;
                durationDetermined = calcularTempoVerdeAdaptativo(light, queueSizes, false, isPeakHour);
                break;
        }
        return new MudancaDeFase(nextPhaseDetermined, durationDetermined);
    }
}