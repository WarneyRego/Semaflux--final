package org.semaflux.sim.control;

import org.semaflux.sim.core.FaseDoSemaforo;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.simulação.Config;
import org.semaflux.sim.simulação.MudancaDeFase;

public class EconomiaEnergia implements Semaforo {
    private double tempoVerdeBase;
    private double tempoAmarelo;
    private double tempoVerdeMinimo;
    private int limiteTrafegoBaixo;
    private double tempoVerdeMaximo; 
    private double tempoVermelhoMinimo;   
    private double tempoVermelhoMaximo;   

    public EconomiaEnergia() {
        this(20.0, 3.0, 7.0, 1, 40.0, 10.0, 43.0); 
    }
    
    public EconomiaEnergia(double tempoBase, double tempoAmarelo, double tempoMinimo, 
                              int limiteTrafico, double tempoMaximo) {
        this(tempoBase, tempoAmarelo, tempoMinimo, limiteTrafico, tempoMaximo, 
             tempoMinimo + tempoAmarelo, tempoMaximo + tempoAmarelo); 
    }
    
    public EconomiaEnergia(double tempoBase, double tempoAmarelo, double tempoMinimo, 
                              int limiteTrafico, double tempoMaximo, 
                              double tempoVermelhoMin, double tempoVermelhoMax) {
        this.tempoVerdeBase = tempoBase;
        this.tempoAmarelo = tempoAmarelo;
        this.tempoVerdeMinimo = tempoMinimo;
        this.limiteTrafegoBaixo = limiteTrafico;
        this.tempoVerdeMaximo = tempoMaximo; 
        this.tempoVermelhoMinimo = tempoVermelhoMin;
        this.tempoVermelhoMaximo = tempoVermelhoMax;
    }

    private double calcularTempoVerdeEconomia(SinalTransito light, int[] queueSizes, boolean isEastWestPhase, boolean isPeakHour) {
        double greenTime = isPeakHour ? this.tempoVerdeBase + 2.0 : this.tempoVerdeBase; 

        Integer relevantIndex1 = isEastWestPhase ? light.getDirectionIndex("east") : light.getDirectionIndex("north");
        Integer relevantIndex2 = isEastWestPhase ? light.getDirectionIndex("west") : light.getDirectionIndex("south");

        int trafficCount = ((relevantIndex1 != null && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0) +
                ((relevantIndex2 != null && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0);

        if (trafficCount <= this.limiteTrafegoBaixo && !isPeakHour) {
            greenTime = this.tempoVerdeMinimo;
        }
        return Math.min(greenTime, this.tempoVerdeMaximo);
    }

    @Override
    public void inicializar(SinalTransito light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        FaseDoSemaforo startPhase = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO;

        if (light.getConfiguration() != null) {
            Config config = light.getConfiguration();
            this.tempoVermelhoMinimo = config.getMinimoVermelhoEconomia();
            this.tempoVermelhoMaximo = config.getMaximoVermelhoEconomia();
        }

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE;
        }

        double initialDuration = light.isPeakHourEnabled() ? this.tempoVerdeBase + 2.0 : this.tempoVerdeBase;
        initialDuration = Math.max(initialDuration, this.tempoVerdeMinimo);
        initialDuration = Math.min(initialDuration, this.tempoVerdeMaximo); 

        light.setCurrentPhase(startPhase, initialDuration);
    }

    @Override
    public String getEstadoSinalParaAproximacao(SinalTransito semaforo, String direcaoAproximacao) {
        FaseDoSemaforo faseAtual = semaforo.getCurrentPhase();
        if (faseAtual == null || direcaoAproximacao == null) {
            return "red";
        }
        
        String direcao = direcaoAproximacao.toLowerCase();
        boolean isDirecaoNS = direcao.equals("north") || direcao.equals("south");
        boolean isDirecaoLO = direcao.equals("east") || direcao.equals("west");
        
        if (faseAtual == FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO && isDirecaoNS) {
            return "green";
        } else if (faseAtual == FaseDoSemaforo.NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO && isDirecaoNS) {
            return "yellow";
        } else if (faseAtual == FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE && isDirecaoLO) {
            return "green";
        } else if (faseAtual == FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO && isDirecaoLO) {
            return "yellow";
        } else {
            return "red";
        }
    }

    @Override
    public MudancaDeFase decidirProximaFase(SinalTransito light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        FaseDoSemaforo currentPhase = light.getCurrentPhase();
        FaseDoSemaforo nextPhase;
        double duration;

        if (light.getConfiguration() != null) {
            Config config = light.getConfiguration();
            this.tempoVermelhoMinimo = config.getMinimoVermelhoEconomia();
            this.tempoVermelhoMaximo = config.getMaximoVermelhoEconomia();
        }

        switch (currentPhase) {
            case NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO:
                nextPhase = FaseDoSemaforo.NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO;
                duration = this.tempoAmarelo;
                break;
            case NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO:
                nextPhase = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE;
               
                duration = calcularTempoVerdeEconomia(light, queueSizes, true, isPeakHour);
               
                if (duration < this.tempoVermelhoMinimo - this.tempoAmarelo) {
                    duration = this.tempoVermelhoMinimo - this.tempoAmarelo;
                }
                if (duration > this.tempoVermelhoMaximo - this.tempoAmarelo) {
                    duration = this.tempoVermelhoMaximo - this.tempoAmarelo;
                }
                break;
            case NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE:
                nextPhase = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO;
                duration = this.tempoAmarelo;
                break;
            case NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO:
                nextPhase = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO;
                
                duration = calcularTempoVerdeEconomia(light, queueSizes, false, isPeakHour);
             
                if (duration < this.tempoVermelhoMinimo - this.tempoAmarelo) {
                    duration = this.tempoVermelhoMinimo - this.tempoAmarelo;
                }
                if (duration > this.tempoVermelhoMaximo - this.tempoAmarelo) {
                    duration = this.tempoVermelhoMaximo - this.tempoAmarelo;
                }
                break;
            default:
                nextPhase = FaseDoSemaforo.NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO;
                duration = calcularTempoVerdeEconomia(light, queueSizes, false, isPeakHour);
                break;
        }
        return new MudancaDeFase(nextPhase, duration);
    }
}