package com.datacluster.server;

import com.datacluster.common.constants.AppConstants;
import com.datacluster.common.enums.NodeStatus;
import com.datacluster.common.enums.NodeType;
import com.datacluster.common.model.Node;
import com.datacluster.server.dao.*;
import com.datacluster.server.engine.AlertEngine;
import com.datacluster.server.monitor.HeartbeatMonitor;
import com.datacluster.server.network.TCPReceiverModule;
import com.datacluster.server.network.UDPReceiverModule;
import com.datacluster.server.persistence.PersistenceModule;
import com.datacluster.server.processor.MetricsProcessor;
import com.datacluster.server.rmi.*;
import com.datacluster.server.stats.StatisticsEngine;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Point d'entrée du serveur DataCluster Watch.
 * Initialise les couches réseau (UDP/TCP), la persistance,
 * le moteur d'alertes et les services RMI.
 */
public class ServerMain {

    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());

    public static void main(String[] args) {
        Properties config = PersistenceModule.loadConfig();

        // Permet aux stubs RMI d'être joignables depuis l'extérieur (Docker, réseau)
        String rmiHostname = System.getenv("JAVA_RMI_SERVER_HOSTNAME");
        if (rmiHostname == null || rmiHostname.isBlank()) {
            rmiHostname = config.getProperty("java.rmi.server.hostname", "localhost");
        }
        System.setProperty("java.rmi.server.hostname", rmiHostname);
        LOGGER.info("RMI hostname: " + rmiHostname);

        PersistenceModule pm = PersistenceModule.getInstance(config);

        // ─── DAOs ────────────────────────────────────────────────────────────
        MetricDAO    metricDAO    = new MetricDAO(pm);
        AlertDAO     alertDAO     = new AlertDAO(pm);
        JobDAO       jobDAO       = new JobDAO(pm);
        NodeDAO      nodeDAO      = new NodeDAO(pm);
        ThresholdDAO thresholdDAO = new ThresholdDAO(pm);

        seedNodes(nodeDAO);

        // ─── Services métier ──────────────────────────────────────────────────
        AlertEngine      alertEngine  = new AlertEngine(alertDAO, thresholdDAO);
        MetricsProcessor processor    = new MetricsProcessor(metricDAO, nodeDAO, alertEngine);
        StatisticsEngine statsEngine  = new StatisticsEngine(processor, metricDAO);

        // ─── Réseau ───────────────────────────────────────────────────────────
        int udpPort  = Integer.parseInt(config.getProperty("server.udp.port",
                String.valueOf(AppConstants.DEFAULT_UDP_PORT)));
        int tcpPort  = Integer.parseInt(config.getProperty("server.tcp.port",
                String.valueOf(AppConstants.DEFAULT_TCP_PORT)));
        int rmiPort  = Integer.parseInt(config.getProperty("server.rmi.port",
                String.valueOf(AppConstants.DEFAULT_RMI_PORT)));
        int poolSize = Integer.parseInt(config.getProperty("server.tcp.thread.pool.size", "10"));

        UDPReceiverModule udpReceiver = new UDPReceiverModule(udpPort, processor);
        TCPReceiverModule tcpReceiver = new TCPReceiverModule(tcpPort, alertDAO, poolSize);

        // ─── HeartbeatMonitor ─────────────────────────────────────────────────
        long hbTimeout  = Long.parseLong(config.getProperty("heartbeat.timeout.ms", "30000"));
        long hbInterval = Long.parseLong(config.getProperty("heartbeat.check.interval.ms", "15000"));
        HeartbeatMonitor heartbeatMonitor = new HeartbeatMonitor(
                processor, alertDAO, nodeDAO, hbTimeout, hbInterval);

        // ─── RMI ──────────────────────────────────────────────────────────────
        try {
            Registry registry = LocateRegistry.createRegistry(rmiPort);
            registry.bind(AppConstants.RMI_SERVICE_NAME,
                    new ClusterServiceImpl(nodeDAO, processor, alertDAO));
            registry.bind(AppConstants.RMI_ALERT_SERVICE,
                    new AlertServiceImpl(alertDAO));
            registry.bind(AppConstants.RMI_JOB_SERVICE,
                    new JobServiceImpl(jobDAO));
            registry.bind(AppConstants.RMI_STATS_SERVICE,
                    new StatsServiceImpl(statsEngine));
            registry.bind(AppConstants.RMI_CONFIG_SERVICE,
                    new ConfigServiceImpl(alertEngine, config));
            LOGGER.info("RMI registry started on port " + rmiPort);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start RMI registry", e);
            System.exit(1);
        }

        udpReceiver.start();
        tcpReceiver.start();

        // ─── Shutdown hook ────────────────────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down server…");
            udpReceiver.shutdown();
            tcpReceiver.shutdown();
            heartbeatMonitor.stop();
            pm.close();
        }, "shutdown-hook"));

        LOGGER.info("=== DataCluster Watch Server started ===");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void seedNodes(NodeDAO nodeDAO) {
        Node[] defaults = {
                new Node("master-01",  "Master Node 01",  NodeType.MASTER,  NodeStatus.ACTIVE),
                new Node("worker-01",  "Worker Node 01",  NodeType.WORKER,  NodeStatus.ACTIVE),
                new Node("worker-02",  "Worker Node 02",  NodeType.WORKER,  NodeStatus.ACTIVE),
                new Node("worker-03",  "Worker Node 03",  NodeType.WORKER,  NodeStatus.ACTIVE),
                new Node("storage-01", "Storage Node 01", NodeType.STORAGE, NodeStatus.ACTIVE),
        };
        for (Node node : defaults) {
            try {
                nodeDAO.save(node);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to seed node: " + node.getId(), e);
            }
        }
    }
}
