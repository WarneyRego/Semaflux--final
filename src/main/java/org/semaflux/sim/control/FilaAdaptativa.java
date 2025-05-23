package org.semaflux.sim.control;

import org.semaflux.sim.core.FaseDoSemaforo;
import org.semaflux.sim.core.SinalTransito;
import org.semaflux.sim.simulação.Config;
import org.semaflux.sim.simulação.MudancaDeFase;

public class FilaAdaptativa implements Semaforo {
    // Parâmetros base de configuração
    private double TempoVerdeBase;
    private double TempoAmarelo;
    private double TempoMaximoVerde;     
    private int tempoTransicao;
    private double TempoMinimoVerde;
    private double tempoExtraPorVeiculo;
    private double TempoMinimoVermelho;       
    private double TempoMaximoVermelho;
    
    // Armazenamento de métricas históricas para decisões mais inteligentes
    private double ultimoTempoMedioNorteSul = 0;
    private double ultimoTempoMedioLesteOeste = 0;
    private int filaMediaNorteSul = 0;
    private int filaMediaLesteOeste = 0;
    private int contadorCiclos = 0;
    
    // Fatores de ponderação para melhorar a adaptabilidade
    private static final double FATOR_HORARIO_PICO = 1.5;
    private static final double FATOR_REDUCAO_FILA_VAZIA = 0.6;
    private static final double FATOR_PENALIZACAO_FILA_LONGA = 1.2;
    private static final double PESO_HISTORICO = 0.3;
    private static final double PESO_ATUAL = 0.7;
    private static final double FATOR_COMPENSACAO = 1.2;

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

    /**
     * Calcula o tempo de verde adaptativo baseado em múltiplos fatores:
     * - Tamanho atual das filas
     * - Histórico recente de filas
     * - Tendência de crescimento/diminuição das filas
     * - Compensação baseada em desequilíbrios
     * - Horário de pico
     */
    private double calcularTempoVerdeAdaptativo(SinalTransito light, int[] queueSizes, boolean isEastWestGreenPhase, boolean isPeakHour) {
        // Base do tempo adaptativo
        double adaptiveGreenDuration = isPeakHour ? 
                this.TempoVerdeBase * FATOR_HORARIO_PICO : 
                this.TempoVerdeBase;

        // Obter índices das direções relevantes
        Integer northIndex = light.getDirectionIndex("north");
        Integer southIndex = light.getDirectionIndex("south");
        Integer eastIndex = light.getDirectionIndex("east");
        Integer westIndex = light.getDirectionIndex("west");

        // Tamanho das filas nas direções Norte-Sul
        int northQueueSize = (northIndex != null && northIndex >= 0 && northIndex < queueSizes.length) ? 
                queueSizes[northIndex] : 0;
        int southQueueSize = (southIndex != null && southIndex >= 0 && southIndex < queueSizes.length) ? 
                queueSizes[southIndex] : 0;
        int totalNorthSouthQueue = northQueueSize + southQueueSize;
        
        // Tamanho das filas nas direções Leste-Oeste
        int eastQueueSize = (eastIndex != null && eastIndex >= 0 && eastIndex < queueSizes.length) ? 
                queueSizes[eastIndex] : 0;
        int westQueueSize = (westIndex != null && westIndex >= 0 && westIndex < queueSizes.length) ? 
                queueSizes[westIndex] : 0;
        int totalEastWestQueue = eastQueueSize + westQueueSize;

        // Atualizar médias históricas usando média ponderada
        if (isEastWestGreenPhase) {
            filaMediaLesteOeste = (int)(PESO_HISTORICO * filaMediaLesteOeste + PESO_ATUAL * totalEastWestQueue);
        } else {
            filaMediaNorteSul = (int)(PESO_HISTORICO * filaMediaNorteSul + PESO_ATUAL * totalNorthSouthQueue);
        }

        // Selecionar as filas relevantes para a fase atual
        int totalCurrentDirectionQueue = isEastWestGreenPhase ? totalEastWestQueue : totalNorthSouthQueue;
        int totalOppositeDirectionQueue = isEastWestGreenPhase ? totalNorthSouthQueue : totalEastWestQueue;
        int mediaHistoricaAtual = isEastWestGreenPhase ? filaMediaLesteOeste : filaMediaNorteSul;
        
        // Detecção de tendência de crescimento
        boolean filaCrescendo = totalCurrentDirectionQueue > mediaHistoricaAtual;
        
        // Se filas nas direções abertas estão vazias (e não é horário de pico), reduzir o tempo
        if (totalCurrentDirectionQueue == 0 && !isPeakHour) {
            return Math.max(this.TempoMinimoVerde, adaptiveGreenDuration * FATOR_REDUCAO_FILA_VAZIA);
        }

        // Se filas das direções fechadas são muito maiores, penalizar o tempo verde atual
        if (totalOppositeDirectionQueue > totalCurrentDirectionQueue * FATOR_PENALIZACAO_FILA_LONGA 
                && totalOppositeDirectionQueue > this.tempoTransicao * 2) {
            double fatorReducao = Math.min(0.8, 
                    0.9 - (0.1 * Math.min(1.0, (totalOppositeDirectionQueue - totalCurrentDirectionQueue) / 20.0)));
            adaptiveGreenDuration *= fatorReducao;
        }

        // Adicionar tempo extra com base no tamanho da fila
        if (totalCurrentDirectionQueue > this.tempoTransicao) {
            // Crescimento logarítmico para evitar tempos verdes excessivos com filas muito grandes
            double fatorCrescimento = Math.log10(totalCurrentDirectionQueue - this.tempoTransicao + 10) / Math.log10(10);
            double extension = this.tempoExtraPorVeiculo * (totalCurrentDirectionQueue - this.tempoTransicao) * fatorCrescimento;
            
            // Se a fila está crescendo, adicionar tempo extra para compensar
            if (filaCrescendo) {
                extension *= FATOR_COMPENSACAO;
            }
            
            adaptiveGreenDuration += extension;
        }
        
        // Compensação para desequilíbrios persistentes
        if (contadorCiclos > 5) {
            double razaoFilas = (double)(filaMediaNorteSul + 1) / (filaMediaLesteOeste + 1);
            
            if (isEastWestGreenPhase && razaoFilas > 1.5) {
                // Norte-Sul tem consistentemente mais tráfego, reduzir Leste-Oeste
                adaptiveGreenDuration *= 0.9;
            } else if (!isEastWestGreenPhase && razaoFilas < 0.67) {
                // Leste-Oeste tem consistentemente mais tráfego, reduzir Norte-Sul
                adaptiveGreenDuration *= 0.9;
            }
        }

        // Aplicar limites mínimos e máximos
        adaptiveGreenDuration = Math.min(adaptiveGreenDuration, this.TempoMaximoVerde);
        adaptiveGreenDuration = Math.max(adaptiveGreenDuration, this.TempoMinimoVerde);

        // Armazenar tempo para análise histórica
        if (isEastWestGreenPhase) {
            ultimoTempoMedioLesteOeste = adaptiveGreenDuration;
        } else {
            ultimoTempoMedioNorteSul = adaptiveGreenDuration;
        }

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

        double initialDuration = light.isPeakHourEnabled() ? 
                this.TempoVerdeBase * FATOR_HORARIO_PICO : 
                this.TempoVerdeBase; 
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

        // Incrementar contador de ciclos para análise histórica
        if (currentPhase == FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO) {
            contadorCiclos++;
        }

        switch (currentPhase) {
            case NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO:
                nextPhaseDetermined = FaseDoSemaforo.NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO;
                durationDetermined = this.TempoAmarelo;
                break;
            case NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO:
                nextPhaseDetermined = FaseDoSemaforo.NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE;
                durationDetermined = calcularTempoVerdeAdaptativo(light, queueSizes, true, isPeakHour);
                
                // Ajustar para os limites do tempo vermelho
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
                
                // Ajustar para os limites do tempo vermelho
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