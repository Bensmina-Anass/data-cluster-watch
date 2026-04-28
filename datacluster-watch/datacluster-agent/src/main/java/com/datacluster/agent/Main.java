package com.datacluster.agent;

import com.datacluster.common.enums.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Point d'entrée du module agent.
 * Lance 5 agents (master-01, worker-01..03, storage-01) dans des threads séparés.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        boolean chaosMode = args.length > 0 && "--chaos".equalsIgnoreCase(args[0]);
        if (chaosMode) {
            LOGGER.warning("=== CHAOS MODE ENABLED ===");
        }

        List<NodeAgent> agents = buildAgents(chaosMode);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown signal received — stopping agents");
            agents.forEach(NodeAgent::stop);
        }, "shutdown-hook"));

        agents.forEach(NodeAgent::start);
        LOGGER.info("All " + agents.size() + " agents started");

        // Bloquer le thread principal pour maintenir les daemons en vie
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<NodeAgent> buildAgents(boolean chaosMode) {
        List<NodeAgent> agents = new ArrayList<>();
        agents.add(new NodeAgent("master-01",  "Master Node 01",  NodeType.MASTER,  chaosMode));
        agents.add(new NodeAgent("worker-01",  "Worker Node 01",  NodeType.WORKER,  chaosMode));
        agents.add(new NodeAgent("worker-02",  "Worker Node 02",  NodeType.WORKER,  chaosMode));
        agents.add(new NodeAgent("worker-03",  "Worker Node 03",  NodeType.WORKER,  chaosMode));
        agents.add(new NodeAgent("storage-01", "Storage Node 01", NodeType.STORAGE, chaosMode));
        return agents;
    }
}
