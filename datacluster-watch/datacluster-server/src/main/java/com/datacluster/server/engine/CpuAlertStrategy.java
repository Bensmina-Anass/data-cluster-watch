package com.datacluster.server.engine;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.enums.AlertType;
import com.datacluster.common.model.Alert;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.ThresholdConfig;
import com.datacluster.server.model.NodeState;

import java.util.Optional;

/**
 * Stratégie de détection des surcharges CPU.
 */
public class CpuAlertStrategy implements AlertStrategy {

    @Override
    public Optional<Alert> evaluate(NodeState state, ThresholdConfig threshold) {
        Metric m = state.getLastMetric();
        if (m == null) return Optional.empty();

        double cpu = m.getCpu();
        long   now = System.currentTimeMillis();

        if (cpu >= threshold.getCriticalValue()) {
            return Optional.of(new Alert(
                    state.getNode().getId(), now,
                    AlertType.CPU_HIGH, AlertLevel.CRITICAL,
                    String.format("CPU critique sur %s : %.1f%% (seuil=%.0f%%)",
                            state.getNode().getName(), cpu, threshold.getCriticalValue())
            ));
        }
        if (cpu >= threshold.getWarningValue()) {
            return Optional.of(new Alert(
                    state.getNode().getId(), now,
                    AlertType.CPU_HIGH, AlertLevel.WARNING,
                    String.format("CPU élevé sur %s : %.1f%% (seuil=%.0f%%)",
                            state.getNode().getName(), cpu, threshold.getWarningValue())
            ));
        }
        return Optional.empty();
    }
}
