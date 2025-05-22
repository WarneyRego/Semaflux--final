package org.semaflux.sim.simulação;

public class Config {
    // Parâmetros gerais de simulação
    private double duracaoSimulacao;
    private double taxaGeracaoVeiculos;
    private double ParadaGeracao;
    private boolean horarioPico;
    private int modoSemaforo;
    private int redirectThreshold;
    
    // Parâmetros do modo fixo
    private double fixedGreenTime;
    private double fixedYellowTime;
    private double fixedRedTime;

    // Parâmetros do modo adaptativo
    private double adaptiveVerdeBase;
    private double adaptiveAmareloBase;
    private double adaptiveMinTempoVerde;
    private double adaptiveMaxVerde;
    private double adaptiveMinTempoVermelho;
    private double adaptiveTempoMaxVermelho;
    private double adaptiveAumento;
    private int adaptiveQueueThreshold;

    // Parâmetros do modo economia de energia
    private double verdeBaseEconomia;
    private double AmareloEconomia;
    private double MinimoVerdeEconomia;
    private double tempoMaximoVerdeEconomia;
    private double MinimoVermelhoEconomia;
    private double MaximoVermelhoEconomia;
    private int limiarEconomia;

    public Config() {
        // Inicialização de parâmetros gerais
        this.duracaoSimulacao = 600.0;
        this.taxaGeracaoVeiculos = 0.3;
        this.ParadaGeracao = 300.0;
        this.horarioPico = false;
        this.modoSemaforo = 1;
        this.redirectThreshold = 0;

        // Inicialização do modo fixo
        this.fixedGreenTime = 15.0;
        this.fixedYellowTime = 3.0;
        this.fixedRedTime = 18.0;

        // Inicialização do modo adaptativo
        this.adaptiveVerdeBase = 10.0;
        this.adaptiveAmareloBase = 3.0;
        this.adaptiveMinTempoVerde = 5.0;
        this.adaptiveMaxVerde = 30.0;
        this.adaptiveMinTempoVermelho = 8.0;
        this.adaptiveTempoMaxVermelho = 33.0;
        this.adaptiveAumento = 1.0;
        this.adaptiveQueueThreshold = 3;

        // Inicialização do modo economia
        this.verdeBaseEconomia = 20.0;
        this.AmareloEconomia = 3.0;
        this.MinimoVerdeEconomia = 7.0;
        this.tempoMaximoVerdeEconomia = 40.0;
        this.MinimoVermelhoEconomia = 10.0;
        this.MaximoVermelhoEconomia = 43.0;
        this.limiarEconomia = 1;
    }

    // Getters e Setters para parâmetros gerais
    public double getDuracaoSimulacao() { return duracaoSimulacao; }
    public void setDuracaoSimulacao(double duration) { this.duracaoSimulacao = duration; }

    public double getTaxaGeracaoVeiculos() { return taxaGeracaoVeiculos; }
    public void setTaxaGeracaoVeiculos(double rate) { this.taxaGeracaoVeiculos = rate; }

    public double getParadaGeracao() { return ParadaGeracao; }
    public void setParadaGeracao(double vehicleGenerationStopTime) { this.ParadaGeracao = vehicleGenerationStopTime; }

    public boolean isHorarioPico() { return horarioPico; }
    public void setHorarioPico(boolean peakHour) { this.horarioPico = peakHour; }

    public int getModoSemaforo() { return modoSemaforo; }
    public void setModoSemaforo(int mode) { this.modoSemaforo = mode; }

    public int getRedirectThreshold() { return redirectThreshold; }
    public void setRedirectThreshold(int threshold) { this.redirectThreshold = threshold; }

    // Getters e Setters para modo fixo
    public double getFixedGreenTime() { return fixedGreenTime; }
    public void setFixedGreenTime(double fixedGreenTime) { this.fixedGreenTime = fixedGreenTime; }

    public double getFixedYellowTime() { return fixedYellowTime; }
    public void setFixedYellowTime(double fixedYellowTime) { this.fixedYellowTime = fixedYellowTime; }

    public double getFixedRedTime() { return fixedRedTime; }
    public void setFixedRedTime(double fixedRedTime) { this.fixedRedTime = fixedRedTime; }

    // Getters e Setters para modo adaptativo
    public double getAdaptiveVerdeBase() { return adaptiveVerdeBase; }
    public void setAdaptiveVerdeBase(double adaptiveBaseGreen) { this.adaptiveVerdeBase = adaptiveBaseGreen; }

    public double getAdaptiveAmareloBase() { return adaptiveAmareloBase; }
    public void setAdaptiveAmareloBase(double adaptiveYellowTime) { this.adaptiveAmareloBase = adaptiveYellowTime; }

    public double getAdaptiveMinTempoVerde() { return adaptiveMinTempoVerde; }
    public void setAdaptiveMinTempoVerde(double adaptiveMinGreenTime) { this.adaptiveMinTempoVerde = adaptiveMinGreenTime; }

    public double getAdaptiveMaxVerde() { return adaptiveMaxVerde; }
    public void setAdaptiveMaxVerde(double adaptiveMaxGreen) { this.adaptiveMaxVerde = adaptiveMaxGreen; }

    public double getAdaptiveMinTempoVermelho() { return adaptiveMinTempoVermelho; }
    public void setAdaptiveMinTempoVermelho(double adaptiveMinRedTime) { this.adaptiveMinTempoVermelho = adaptiveMinRedTime; }

    public double getAdaptiveTempoMaxVermelho() { return adaptiveTempoMaxVermelho; }
    public void setAdaptiveTempoMaxVermelho(double adaptiveMaxRedTime) { this.adaptiveTempoMaxVermelho = adaptiveMaxRedTime; }

    public double getAdaptiveAumento() { return adaptiveAumento; }
    public void setAdaptiveAumento(double adaptiveIncrement) { this.adaptiveAumento = adaptiveIncrement; }

    public int getAdaptiveQueueThreshold() { return adaptiveQueueThreshold; }
    public void setAdaptiveQueueThreshold(int adaptiveQueueThreshold) { this.adaptiveQueueThreshold = adaptiveQueueThreshold; }

    // Getters e Setters para modo economia
    public double getVerdeBaseEconomia() { return verdeBaseEconomia; }
    public void setVerdeBaseEconomia(double energySavingBaseGreen) { this.verdeBaseEconomia = energySavingBaseGreen; }

    public double getAmareloEconomia() { return AmareloEconomia; }
    public void setAmareloEconomia(double energySavingYellowTime) { this.AmareloEconomia = energySavingYellowTime; }

    public double getMinimoVerdeEconomia() { return MinimoVerdeEconomia; }
    public void setMinimoVerdeEconomia(double energySavingMinGreen) { this.MinimoVerdeEconomia = energySavingMinGreen; }

    public double getTempoMaximoVerdeEconomia() { return tempoMaximoVerdeEconomia; }
    public void setTempoMaximoVerdeEconomia(double energySavingMaxGreenTime) { this.tempoMaximoVerdeEconomia = energySavingMaxGreenTime; }

    public double getMinimoVermelhoEconomia() { return MinimoVermelhoEconomia; }
    public void setMinimoVermelhoEconomia(double energySavingMinRedTime) { this.MinimoVermelhoEconomia = energySavingMinRedTime; }

    public double getMaximoVermelhoEconomia() { return MaximoVermelhoEconomia; }
    public void setMaximoVermelhoEconomia(double energySavingMaxRedTime) { this.MaximoVermelhoEconomia = energySavingMaxRedTime; }

    public int getLimiarEconomia() { return limiarEconomia; }
    public void setLimiarEconomia(int energySavingThreshold) { this.limiarEconomia = energySavingThreshold; }
}