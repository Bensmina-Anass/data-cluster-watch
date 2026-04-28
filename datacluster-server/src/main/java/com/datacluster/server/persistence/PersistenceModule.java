package com.datacluster.server.persistence;

import com.datacluster.common.util.ConfigLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Module de persistance — gère le pool de connexions HikariCP.
 * Singleton d'infrastructure partagé par tous les DAO.
 */
public class PersistenceModule {

    private static final Logger LOGGER = Logger.getLogger(PersistenceModule.class.getName());

    private static volatile PersistenceModule instance;
    private final HikariDataSource dataSource;

    private PersistenceModule(Properties config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getProperty("db.url"));
        hikariConfig.setUsername(config.getProperty("db.username"));
        hikariConfig.setPassword(config.getProperty("db.password"));
        hikariConfig.setMaximumPoolSize(Integer.parseInt(
                config.getProperty("db.pool.size", "10")));
        hikariConfig.setMinimumIdle(Integer.parseInt(
                config.getProperty("db.pool.min.idle", "2")));
        hikariConfig.setConnectionTimeout(Long.parseLong(
                config.getProperty("db.pool.connection.timeout", "30000")));
        hikariConfig.setPoolName("DataClusterPool");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        // En environnement Docker, MySQL peut ne pas être prêt immédiatement
        hikariConfig.setInitializationFailTimeout(60_000);
        this.dataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("HikariCP pool initialized: " + config.getProperty("db.url"));
    }

    /**
     * Retourne l'instance unique du module de persistance.
     *
     * @param config configuration de l'application
     * @return instance singleton
     */
    public static PersistenceModule getInstance(Properties config) {
        if (instance == null) {
            synchronized (PersistenceModule.class) {
                if (instance == null) {
                    instance = new PersistenceModule(config);
                }
            }
        }
        return instance;
    }

    /**
     * Emprunte une connexion au pool.
     *
     * @return connexion JDBC active
     * @throws SQLException si aucune connexion n'est disponible
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Ferme le pool de connexions. */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP pool closed");
        }
    }

    /**
     * Charge la configuration (classpath + variables d'environnement).
     *
     * @return {@link Properties} fusionnées
     */
    public static Properties loadConfig() {
        return ConfigLoader.load();
    }
}
