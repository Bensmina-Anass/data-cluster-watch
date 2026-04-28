package com.datacluster.server.rmi;

import com.datacluster.common.model.ThresholdConfig;
import com.datacluster.common.rmi.IConfigService;
import com.datacluster.server.engine.AlertEngine;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Implémentation RMI du service de configuration des seuils.
 */
public class ConfigServiceImpl extends UnicastRemoteObject implements IConfigService {

    private static final Logger LOGGER = Logger.getLogger(ConfigServiceImpl.class.getName());

    private final AlertEngine alertEngine;
    private final Properties  serverConfig;

    /**
     * @param alertEngine  moteur d'alertes (détient les seuils)
     * @param serverConfig propriétés de configuration du serveur
     * @throws RemoteException si l'export RMI échoue
     */
    public ConfigServiceImpl(AlertEngine alertEngine, Properties serverConfig) throws RemoteException {
        super();
        this.alertEngine  = alertEngine;
        this.serverConfig = serverConfig;
    }

    @Override
    public List<ThresholdConfig> getThresholds() throws RemoteException {
        return alertEngine.getThresholds();
    }

    @Override
    public void setThreshold(String metric, double warningValue, double criticalValue)
            throws RemoteException, IllegalArgumentException {
        if (warningValue < 0 || criticalValue < 0 || warningValue >= criticalValue) {
            throw new IllegalArgumentException(
                    "warningValue must be >= 0 and < criticalValue");
        }
        ThresholdConfig config = new ThresholdConfig(metric, warningValue, criticalValue);
        alertEngine.updateThreshold(config);
        LOGGER.info("Threshold updated: " + config);
    }

    @Override
    public Map<String, String> getServerConfig() throws RemoteException {
        Map<String, String> safeConfig = new TreeMap<>();
        for (String key : serverConfig.stringPropertyNames()) {
            // Filtre les informations sensibles
            if (!key.contains("password") && !key.contains("secret")) {
                safeConfig.put(key, serverConfig.getProperty(key));
            }
        }
        return safeConfig;
    }
}
