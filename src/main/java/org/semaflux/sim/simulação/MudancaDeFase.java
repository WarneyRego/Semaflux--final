package org.semaflux.sim.simulação;

import org.semaflux.sim.core.FaseDoSemaforo;

public class MudancaDeFase {
    public final FaseDoSemaforo nextPhase;
    public final double duration;

    public MudancaDeFase(FaseDoSemaforo nextPhase, double duration) {
        this.nextPhase = nextPhase;
        this.duration = duration;
    }
}