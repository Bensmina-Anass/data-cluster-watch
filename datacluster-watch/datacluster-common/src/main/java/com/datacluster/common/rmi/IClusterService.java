package com.datacluster.common.rmi;

import com.datacluster.common.model.ClusterSummary;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.Node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI principal exposant l'état global du cluster.
 */
public interface IClusterService extends Remote {

    /**
     * Retourne la liste de tous les nœuds enregistrés.
     *
     * @return liste de {@link Node}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Node> getAllNodes() throws RemoteException;

    /**
     * Retourne le nœud identifié par {@code nodeId}.
     *
     * @param nodeId identifiant du nœud
     * @return nœud correspondant, ou {@code null} si introuvable
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    Node getNode(String nodeId) throws RemoteException;

    /**
     * Retourne la dernière métrique collectée pour chaque nœud.
     *
     * @return liste de {@link Metric}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Metric> getLatestMetrics() throws RemoteException;

    /**
     * Retourne la dernière métrique collectée pour un nœud donné.
     *
     * @param nodeId identifiant du nœud
     * @return dernière {@link Metric}, ou {@code null}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    Metric getLatestMetric(String nodeId) throws RemoteException;

    /**
     * Retourne un résumé agrégé de l'état du cluster.
     *
     * @return {@link ClusterSummary}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    ClusterSummary getClusterSummary() throws RemoteException;
}
