package com.datacluster.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Chargeur de configuration unifié.
 * Lit {@code application.properties} depuis le classpath, puis surcharge chaque
 * propriété par la variable d'environnement correspondante.
 *
 * <p>Convention de mapping :
 * <pre>
 *   server.host      →  SERVER_HOST
 *   db.url           →  DB_URL
 *   server.rmi.port  →  SERVER_RMI_PORT
 * </pre>
 * Les points et tirets sont remplacés par des underscores, le tout en majuscules.
 */
public final class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());

    private ConfigLoader() {}

    /**
     * Charge la configuration en fusionnant le fichier de propriétés et les variables
     * d'environnement (les variables d'environnement ont priorité).
     *
     * @return {@link Properties} fusionnées
     */
    public static Properties load() {
        Properties props = new Properties();
        try (InputStream is = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                LOGGER.warning("application.properties introuvable dans le classpath");
            }
        } catch (IOException e) {
            LOGGER.warning("Erreur lors du chargement de application.properties : " + e.getMessage());
        }

        // Surcharge par les variables d'environnement
        for (String key : props.stringPropertyNames()) {
            String envKey = key.toUpperCase()
                    .replace('.', '_')
                    .replace('-', '_');
            String envValue = System.getenv(envKey);
            if (envValue != null && !envValue.isBlank()) {
                props.setProperty(key, envValue);
                LOGGER.fine("Config override via env: " + key + " = " + envKey);
            }
        }
        return props;
    }
}
