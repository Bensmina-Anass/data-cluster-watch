package com.datacluster.common.rmi;

import com.datacluster.common.model.ThresholdConfig;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Service RMI de configuration des seuils d'alertes et paramètres serveur.
 */
public interface IConfigService extends Remote {

    /**
     * Retourne la liste des seuils d'alerte actuellement configurés.
     *
     * @return liste de {@link ThresholdConfig}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<ThresholdConfig> getThresholds() throws RemoteException;

    /**
     * Met à jour ou crée un seuil pour une métrique donnée.
     *
     * @param metric         nom de la métrique (cpu, ram, disk)
     * @param warningValue   nouvelle valeur WARNING
     * @param criticalValue  nouvelle valeur CRITICAL
     * @throws RemoteException              en cas d'erreur de communication RMI
     * @throws IllegalArgumentException     si les valeurs sont invalides
     */
    void setThreshold(String metric, double warningValue, double criticalValue)
            throws RemoteException, IllegalArgumentException;

    /**
     * Retourne les paramètres de configuration du serveur (lecture seule).
     *
     * @return map clé → valeur
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    Map<String, String> getServerConfig() throws RemoteException;
}
