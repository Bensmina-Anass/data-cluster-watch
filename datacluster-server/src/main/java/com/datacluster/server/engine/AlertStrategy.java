package com.datacluster.server.engine;

import com.datacluster.common.model.Alert;
import com.datacluster.common.model.ThresholdConfig;
import com.datacluster.server.model.NodeState;

import java.util.Optional;

/**
 * Interface Strategy pour l'évaluation d'une règle d'alerte.
 * Chaque implémentation encapsule la logique de détection d'une anomalie.
 */
public interface AlertStrategy {

    /**
     * Évalue l'état du nœud et retourne une alerte si le seuil est dépassé.
     *
     * @param state     état courant du nœud (métriques + heartbeat)
     * @param threshold seuils configurés pour cette stratégie
     * @return alerte générée, ou {@link Optional#empty()} si tout est normal
     */
    Optional<Alert> evaluate(NodeState state, ThresholdConfig threshold);
}
