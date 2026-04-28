package com.datacluster.client.service;

import com.datacluster.common.constants.AppConstants;
import com.datacluster.common.rmi.*;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service client qui établit et gère la connexion RMI vers le serveur.
 * Singleton d'accès aux proxies RMI.
 */
public class RmiClientService {

    private static final Logger LOGGER = Logger.getLogger(RmiClientService.class.getName());

    private static volatile RmiClientService instance;

    private IClusterService clusterService;
    private IAlertService   alertService;
    private IJobService     jobService;
    private IStatsService   statsService;
    private IConfigService  configService;

    private final String serverHost;
    private final int    rmiPort;

    private RmiClientService(Properties config) {
        this.serverHost = config.getProperty("server.host", AppConstants.DEFAULT_SERVER_HOST);
        this.rmiPort    = Integer.parseInt(config.getProperty("server.rmi.port",
                String.valueOf(AppConstants.DEFAULT_RMI_PORT)));
    }

    /**
     * Retourne l'instance unique du service client RMI.
     *
     * @return instance singleton
     */
    public static RmiClientService getInstance() {
        if (instance == null) {
            synchronized (RmiClientService.class) {
                if (instance == null) {
                    instance = new RmiClientService(loadConfig());
                }
            }
        }
        return instance;
    }

    /**
     * Établit la connexion RMI vers le serveur.
     *
     * @throws RemoteException  en cas d'erreur réseau
     * @throws NotBoundException si un service n'est pas enregistré dans le registry
     */
    public void connect() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(serverHost, rmiPort);
        clusterService = (IClusterService)  registry.lookup(AppConstants.RMI_SERVICE_NAME);
        alertService   = (IAlertService)    registry.lookup(AppConstants.RMI_ALERT_SERVICE);
        jobService     = (IJobService)      registry.lookup(AppConstants.RMI_JOB_SERVICE);
        statsService   = (IStatsService)    registry.lookup(AppConstants.RMI_STATS_SERVICE);
        configService  = (IConfigService)   registry.lookup(AppConstants.RMI_CONFIG_SERVICE);
        LOGGER.info("Connected to RMI server at " + serverHost + ":" + rmiPort);
    }

    /** Vérifie si la connexion est établie. */
    public boolean isConnected() {
        return clusterService != null;
    }

    public IClusterService getClusterService()  { return clusterService; }
    public IAlertService   getAlertService()    { return alertService; }
    public IJobService     getJobService()       { return jobService; }
    public IStatsService   getStatsService()     { return statsService; }
    public IConfigService  getConfigService()    { return configService; }

    private static Properties loadConfig() {
        return com.datacluster.common.util.ConfigLoader.load();
    }
}
