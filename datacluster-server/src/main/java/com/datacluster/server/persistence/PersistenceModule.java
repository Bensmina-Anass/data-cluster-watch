package com.datacluster.server.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
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
     * Charge les propriétés depuis le classpath.
     *
     * @return {@link Properties} chargées
     * @throws IOException si le fichier n'est pas trouvé
     */
    public static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream is = PersistenceModule.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is == null) {
                throw new IOException("application.properties not found in classpath");
            }
            props.load(is);
        }
        return props;
    }
}
