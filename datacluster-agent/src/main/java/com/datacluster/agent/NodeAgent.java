package com.datacluster.agent;

import com.datacluster.agent.simulation.JobSimulator;
import com.datacluster.agent.thread.MetricsCollectorThread;
import com.datacluster.agent.thread.TCPAlertSenderThread;
import com.datacluster.agent.thread.UDPSenderThread;
import com.datacluster.common.enums.NodeType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Agent de monitoring s'exécutant sur un nœud du cluster.
 * Paramétrable par nom, type et mode chaos.
 */
public class NodeAgent {

    private static final Logger LOGGER = Logger.getLogger(NodeAgent.class.getName());

    private final String   nodeId;
    private final String   nodeName;
    private final NodeType nodeType;
    private final boolean  chaosMode;
    private final Properties config;

    private final JobSimulator         jobSimulator;
    private final UDPSenderThread      udpSender;
    private final TCPAlertSenderThread tcpSender;
    private final MetricsCollectorThread collector;

    /**
     * @param nodeId    identifiant unique du nœud (ex. worker-01)
     * @param nodeName  nom lisible
     * @param nodeType  rôle dans le cluster
     * @param chaosMode active l'injection de pannes simulées
     */
    public NodeAgent(String nodeId, String nodeName, NodeType nodeType, boolean chaosMode) {
        this.nodeId    = nodeId;
        this.nodeName  = nodeName;
        this.nodeType  = nodeType;
        this.chaosMode = chaosMode;
        this.config    = loadConfig();

        String serverHost = config.getProperty("server.host", "localhost");
        int    udpPort    = Integer.parseInt(config.getProperty("server.udp.port", "5000"));
        int    tcpPort    = Integer.parseInt(config.getProperty("server.tcp.port", "6000"));

        this.jobSimulator = new JobSimulator(nodeId, nodeType);
        this.udpSender    = new UDPSenderThread(serverHost, udpPort);
        this.tcpSender    = new TCPAlertSenderThread(serverHost, tcpPort);
        this.collector    = new MetricsCollectorThread(
                nodeId, nodeType, jobSimulator, chaosMode, udpSender, tcpSender, config);
    }

    /** Démarre tous les threads de l'agent. */
    public void start() {
        LOGGER.info("Starting agent: " + nodeId + " (" + nodeType + ")");
        jobSimulator.start();
        udpSender.start();
        tcpSender.start();
        collector.start();
    }

    /** Arrête tous les threads de l'agent. */
    public void stop() {
        LOGGER.info("Stopping agent: " + nodeId);
        collector.interrupt();
        tcpSender.interrupt();
        udpSender.interrupt();
        jobSimulator.stop();
    }

    private Properties loadConfig() {
        return com.datacluster.common.util.ConfigLoader.load();
    }

    public String   getNodeId()    { return nodeId; }
    public String   getNodeName()  { return nodeName; }
    public NodeType getNodeType()  { return nodeType; }
    public boolean  isChaosMode()  { return chaosMode; }
}
