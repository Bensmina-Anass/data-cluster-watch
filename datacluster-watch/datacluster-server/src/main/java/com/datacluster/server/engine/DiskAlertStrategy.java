package com.datacluster.server.engine;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.enums.AlertType;
import com.datacluster.common.model.Alert;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.ThresholdConfig;
import com.datacluster.server.model.NodeState;

import java.util.Optional;

/**
 * Stratégie de détection de la saturation disque.
 */
public class DiskAlertStrategy implements AlertStrategy {

    @Override
    public Optional<Alert> evaluate(NodeState state, ThresholdConfig threshold) {
        Metric m = state.getLastMetric();
        if (m == null) return Optional.empty();

        double disk = m.getDisk();
        long   now  = System.currentTimeMillis();

        if (disk >= threshold.getCriticalValue()) {
            return Optional.of(new Alert(
                    state.getNode().getId(), now,
                    AlertType.DISK_HIGH, AlertLevel.CRITICAL,
                    String.format("Disque critique sur %s : %.1f%% (seuil=%.0f%%)",
                            state.getNode().getName(), disk, threshold.getCriticalValue())
            ));
        }
        if (disk >= threshold.getWarningValue()) {
            return Optional.of(new Alert(
                    state.getNode().getId(), now,
                    AlertType.DISK_HIGH, AlertLevel.WARNING,
                    String.format("Disque saturé sur %s : %.1f%% (seuil=%.0f%%)",
                            state.getNode().getName(), disk, threshold.getWarningValue())
            ));
        }
        return Optional.empty();
    }
}
