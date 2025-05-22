package org.semaflux.sim.core;

public enum FaseDoSemaforo {
    // Fases para um cruzamento padrão de 4 braços, onde um par de vias opostas fica verde/amarelo
    // enquanto o outro par fica vermelho.
    NORTE_SUL_VERDE_LESTE_OESTE_VERMELHO,  // Norte-Sul Verde, Leste-Oeste Vermelho
    NORTE_SUL_AMARELO_LESTE_OESTE_VERMELHO, // Norte-Sul Amarelo, Leste-Oeste Vermelho
    NORTE_SUL_VERMELHO_LESTE_OESTE_VERDE,  // Leste-Oeste Verde, Norte-Sul Vermelho
    NORTE_SUL_VERMELHO_LESTE_OESTE_AMARELO  // Leste-Oeste Amarelo, Norte-Sul Vermelho
}