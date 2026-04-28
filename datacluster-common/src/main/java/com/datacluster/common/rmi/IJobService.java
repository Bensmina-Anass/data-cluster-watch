package com.datacluster.common.rmi;

import com.datacluster.common.enums.JobType;
import com.datacluster.common.model.Job;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI d'interrogation des jobs Big Data.
 */
public interface IJobService extends Remote {

    /**
     * Retourne tous les jobs enregistrés (actifs et terminés).
     *
     * @return liste de {@link Job}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Job> getAllJobs() throws RemoteException;

    /**
     * Retourne les jobs d'un nœud spécifique.
     *
     * @param nodeId identifiant du nœud
     * @return liste filtrée de {@link Job}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Job> getJobsByNode(String nodeId) throws RemoteException;

    /**
     * Retourne les jobs dont le statut est {@code RUNNING}.
     *
     * @return liste de {@link Job}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Job> getRunningJobs() throws RemoteException;

    /**
     * Retourne les jobs d'un type donné.
     *
     * @param type type de job Big Data
     * @return liste filtrée de {@link Job}
     * @throws RemoteException en cas d'erreur de communication RMI
     */
    List<Job> getJobsByType(JobType type) throws RemoteException;
}
