package com.datacluster.server.engine;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.enums.AlertType;
import com.datacluster.common.model.Alert;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.ThresholdConfig;
import com.datacluster.server.model.NodeState;

import java.util.Optional;

/**
 * Stratégie de détection de la pression mémoire RAM.
 */
public class RamAlertStrategy implements AlertStrategy {

    @Override
    public Optional<Alert> evaluate(NodeState state, ThresholdConfig threshold) {
        Metric m = state.getLastMetric();
        if (m == null) return Optional.empty();

        double ram = m.getRam();
        long   now = System.currentTimeMillis();

        if (ram >= threshold.getCriticalValue()) {
            return Optional.of(new Alert(
                    state.getNode().getId(), now,
                    AlertType.RAM_HIGH, AlertLevel.CRITICAL,
                    String.format("RAM critique sur %s : %.1f%% (seuil=%.0f%%)",
                            state.getNode().getName(), ram, threshold.getCriticalValue())
            ));
        }
        if (ram >= threshold.getWarningValue()) {
            return Optional.of(new Alert(
                    state.getNode().getId(), now,
                    AlertType.RAM_HIGH, AlertLevel.WARNING,
                    String.format("RAM élevée sur %s : %.1f%% (seuil=%.0f%%)",
                            state.getNode().getName(), ram, threshold.getWarningValue())
            ));
        }
        return Optional.empty();
    }
}
