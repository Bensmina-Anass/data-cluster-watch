package com.datacluster.server.engine;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.enums.AlertType;
import com.datacluster.common.model.Alert;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.ThresholdConfig;
import com.datacluster.server.model.NodeState;

import java.util.Optional;

/**
 * Stratégie de détection des jobs en échec.
 * Le seuil WARNING est atteint dès qu'au moins un job a échoué.
 */
public class FailedJobsAlertStrategy implements AlertStrategy {

    @Override
    public Optional<Alert> evaluate(NodeState state, ThresholdConfig threshold) {
        Metric m = state.getLastMetric();
        if (m == null) return Optional.empty();

        int failed = m.getFailedJobs();
        if (failed >= (int) threshold.getWarningValue()) {
            return Optional.of(new Alert(
                    state.getNode().getId(), System.currentTimeMillis(),
                    AlertType.FAILED_JOBS, AlertLevel.WARNING,
                    String.format("%d job(s) en échec sur %s",
                            failed, state.getNode().getName())
            ));
        }
        return Optional.empty();
    }
}
