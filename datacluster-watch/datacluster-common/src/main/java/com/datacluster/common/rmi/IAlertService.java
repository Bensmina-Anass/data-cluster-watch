package com.datacluster.common.rmi;

import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.model.Alert;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI de gestion des alertes.
 */
public interface IAlertService extends Remote {

    /**
     * Retourne toutes les alertes connues du serveur.
     *
     * @return liste de {@link Alert}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Alert> getAllAlerts() throws RemoteException;

    /**
     * Retourne les alertes relatives à un nœud spécifique.
     *
     * @param nodeId identifiant du nœud
     * @return liste filtrée de {@link Alert}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Alert> getAlertsByNode(String nodeId) throws RemoteException;

    /**
     * Retourne les alertes d'un niveau de sévérité donné.
     *
     * @param level niveau d'alerte
     * @return liste filtrée de {@link Alert}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Alert> getAlertsByLevel(AlertLevel level) throws RemoteException;

    /**
     * Retourne les alertes déclenchées dans une fenêtre temporelle.
     *
     * @param fromEpochMs borne inférieure (epoch ms)
     * @param toEpochMs   borne supérieure (epoch ms)
     * @return liste filtrée de {@link Alert}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Alert> getAlertsBetween(long fromEpochMs, long toEpochMs) throws RemoteException;

    /**
     * Retourne les alertes non encore acquittées.
     *
     * @return liste de {@link Alert}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Alert> getUnacknowledgedAlerts() throws RemoteException;

    /**
     * Acquitte une alerte par son identifiant.
     *
     * @param alertId identifiant de l'alerte
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    void acknowledgeAlert(String alertId) throws RemoteException;
}
