module org.semaflux.sim {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;

    opens org.semaflux.sim to javafx.fxml;
    opens org.semaflux.sim.core to javafx.fxml;
    opens org.semaflux.sim.simulação to javafx.fxml;
    opens org.semaflux.sim.control to javafx.fxml;
    opens org.semaflux.sim.visualization to javafx.fxml;
    
    exports org.semaflux.sim;
    exports org.semaflux.sim.visualization;
    exports org.semaflux.sim.core;
    exports org.semaflux.sim.simulação;
    exports org.semaflux.sim.control;
}