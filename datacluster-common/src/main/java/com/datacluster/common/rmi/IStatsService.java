package com.datacluster.common.rmi;

import com.datacluster.common.model.Metric;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Service RMI de statistiques avancées : moyennes mobiles, z-scores, historique.
 */
public interface IStatsService extends Remote {

    /**
     * Retourne les moyennes mobiles CPU/RAM/disk sur une fenêtre glissante.
     *
     * @param nodeId     identifiant du nœud
     * @param windowSize nombre de points à inclure dans la fenêtre
     * @return map {cpu, ram, disk} → valeur moyenne
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    Map<String, Double> getMovingAverages(String nodeId, int windowSize) throws RemoteException;

    /**
     * Retourne les z-scores courants CPU/RAM/disk pour un nœud.
     * Un z-score > 2 ou < -2 signale une anomalie statistique.
     *
     * @param nodeId identifiant du nœud
     * @return map {cpu, ram, disk} → z-score
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    Map<String, Double> getZScores(String nodeId) throws RemoteException;

    /**
     * Retourne l'historique des métriques sur les {@code minutes} dernières minutes.
     *
     * @param nodeId  identifiant du nœud
     * @param minutes fenêtre temporelle en minutes
     * @return liste de {@link Metric} triée par timestamp croissant
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Metric> getMetricHistory(String nodeId, int minutes) throws RemoteException;

    /**
     * Retourne une comparaison inter-nœuds des métriques moyennes.
     *
     * @return map nodeId → {cpu, ram, disk} → valeur moyenne
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    Map<String, Map<String, Double>> getClusterComparison() throws RemoteException;
}
