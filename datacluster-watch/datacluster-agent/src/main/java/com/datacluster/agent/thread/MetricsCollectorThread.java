package com.datacluster.agent.thread;

import com.datacluster.agent.simulation.JobSimulator;
import com.datacluster.common.constants.AppConstants;
import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.enums.AlertType;
import com.datacluster.common.enums.NodeType;
import com.datacluster.common.model.Alert;
import com.datacluster.common.model.Metric;
import com.datacluster.common.util.JsonSerializer;

import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Thread de collecte et simulation des métriques système.
 * Les valeurs sont générées de façon réaliste avec des variations progressives :
 * tendance sinusoïdale (charge journalière) + bruit gaussien + charge des jobs.
 */
public class MetricsCollectorThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(MetricsCollectorThread.class.getName());

    private final String           nodeId;
    private final NodeType         nodeType;
    private final JobSimulator     jobSimulator;
    private final boolean          chaosMode;
    private final UDPSenderThread  udpSender;
    private final TCPAlertSenderThread tcpSender;

    private final long    metricsIntervalMs;
    private final long    heartbeatIntervalMs;
    private final double  cpuCriticalThreshold;
    private final double  ramCriticalThreshold;
    private final double  diskCriticalThreshold;
    private final int     failedJobsThreshold;

    // Simulation state
    private double baseCpu;
    private double baseRam;
    private double baseDisk;
    private double cpuTrend;
    private double ramTrend;
    private final Random random = new Random();

    private long lastHeartbeatTime = 0L;
    private long tickCount         = 0L;

    /**
     * @param nodeId      identifiant du nœud
     * @param nodeType    type de nœud (influence les niveaux de charge de base)
     * @param jobSim      simulateur de jobs qui pilote la charge
     * @param chaosMode   si {@code true}, injecte des spikes et crashs aléatoires
     * @param udpSender   émetteur UDP des métriques
     * @param tcpSender   émetteur TCP des alertes critiques
     * @param config      configuration de l'agent
     */
    public MetricsCollectorThread(String nodeId, NodeType nodeType,
                                   JobSimulator jobSim, boolean chaosMode,
                                   UDPSenderThread udpSender, TCPAlertSenderThread tcpSender,
                                   Properties config) {
        super("collector-" + nodeId);
        setDaemon(true);
        this.nodeId       = nodeId;
        this.nodeType     = nodeType;
        this.jobSimulator = jobSim;
        this.chaosMode    = chaosMode;
        this.udpSender    = udpSender;
        this.tcpSender    = tcpSender;

        this.metricsIntervalMs    = Long.parseLong(config.getProperty("agent.metrics.interval.ms",
                String.valueOf(AppConstants.METRICS_INTERVAL_MS)));
        this.heartbeatIntervalMs  = Long.parseLong(config.getProperty("agent.heartbeat.interval.ms",
                String.valueOf(AppConstants.HEARTBEAT_INTERVAL_MS)));
        this.cpuCriticalThreshold  = Double.parseDouble(config.getProperty("agent.alert.cpu.critical", "90.0"));
        this.ramCriticalThreshold  = Double.parseDouble(config.getProperty("agent.alert.ram.critical", "85.0"));
        this.diskCriticalThreshold = Double.parseDouble(config.getProperty("agent.alert.disk.critical", "95.0"));
        this.failedJobsThreshold   = Integer.parseInt(config.getProperty("agent.alert.failed.jobs.threshold", "1"));

        // Initialisation des niveaux de base selon le type de nœud
        initBaseValues();
    }

    private void initBaseValues() {
        switch (nodeType) {
            case MASTER  -> { baseCpu = 35.0; baseRam = 45.0; baseDisk = 30.0; }
            case WORKER  -> { baseCpu = 55.0; baseRam = 60.0; baseDisk = 40.0; }
            case STORAGE -> { baseCpu = 20.0; baseRam = 35.0; baseDisk = 65.0; }
        }
        cpuTrend  = 0.0;
        ramTrend  = 0.0;
    }

    @Override
    public void run() {
        LOGGER.info("[" + nodeId + "] MetricsCollectorThread started (chaos=" + chaosMode + ")");
        while (!isInterrupted()) {
            try {
                tickCount++;
                Metric metric = collectMetric();
                sendMetric(metric);
                checkAlerts(metric);

                if (shouldSendHeartbeat()) {
                    sendHeartbeat();
                }

                Thread.sleep(metricsIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.warning("[" + nodeId + "] Collection error: " + e.getMessage());
            }
        }
        LOGGER.info("[" + nodeId + "] MetricsCollectorThread stopped");
    }

    private Metric collectMetric() {
        long now = System.currentTimeMillis();

        // Variation sinusoïdale sur 24h simulées en 1h réelle
        double hourFraction = (now % 3_600_000L) / 3_600_000.0;
        double dailyPattern = Math.sin(hourFraction * 2 * Math.PI) * 15.0;

        // Charge due aux jobs actifs
        int activeJobs = jobSimulator.getActiveJobCount();
        double jobLoad = activeJobs * 8.0;

        // Dérive progressive lente (± 0.3 % par tick)
        cpuTrend = clamp(cpuTrend + (random.nextGaussian() * 0.3), -10, 10);
        ramTrend = clamp(ramTrend + (random.nextGaussian() * 0.2), -8, 8);

        // Bruit gaussien léger
        double cpuNoise  = random.nextGaussian() * 2.5;
        double ramNoise  = random.nextGaussian() * 1.8;
        double diskNoise = random.nextGaussian() * 0.5;

        double cpu  = clamp(baseCpu + dailyPattern + jobLoad + cpuTrend + cpuNoise, 0.5, 99.5);
        double ram  = clamp(baseRam + jobLoad * 0.6 + ramTrend + ramNoise, 5.0, 99.5);
        // Disque augmente lentement sur la durée
        baseDisk = clamp(baseDisk + 0.001, baseDisk, 98.0);
        double disk = clamp(baseDisk + diskNoise, 1.0, 99.5);

        if (chaosMode) {
            cpu  = applyChaos(cpu);
            ram  = applyChaos(ram);
        }

        int failedJobs = jobSimulator.getFailedJobCount();
        jobSimulator.resetFailedCount();

        return new Metric(nodeId, now, cpu, ram, disk, activeJobs, failedJobs);
    }

    private double applyChaos(double value) {
        // 3% de chance de spike brutal
        if (random.nextDouble() < 0.03) {
            LOGGER.warning("[" + nodeId + "] CHAOS: spike injected");
            return clamp(value + 40.0 + random.nextDouble() * 20.0, 0, 100);
        }
        // 1% de chance de latence simulée (NOP pour la métrique mais log)
        if (random.nextDouble() < 0.01) {
            LOGGER.warning("[" + nodeId + "] CHAOS: simulated latency");
        }
        return value;
    }

    private void sendMetric(Metric metric) {
        String json = "{\"type\":\"METRIC\"," + JsonSerializer.toJson(metric).substring(1);
        udpSender.enqueue(json);
    }

    private void checkAlerts(Metric metric) {
        long now = metric.getTimestamp();
        if (metric.getCpu() >= cpuCriticalThreshold) {
            tcpSender.enqueue(new Alert(nodeId, now, AlertType.CPU_HIGH, AlertLevel.CRITICAL,
                    String.format("CPU critique sur %s : %.1f%%", nodeId, metric.getCpu())));
        }
        if (metric.getRam() >= ramCriticalThreshold) {
            tcpSender.enqueue(new Alert(nodeId, now, AlertType.RAM_HIGH, AlertLevel.CRITICAL,
                    String.format("RAM critique sur %s : %.1f%%", nodeId, metric.getRam())));
        }
        if (metric.getDisk() >= diskCriticalThreshold) {
            tcpSender.enqueue(new Alert(nodeId, now, AlertType.DISK_HIGH, AlertLevel.CRITICAL,
                    String.format("Disque critique sur %s : %.1f%%", nodeId, metric.getDisk())));
        }
        if (metric.getFailedJobs() >= failedJobsThreshold) {
            tcpSender.enqueue(new Alert(nodeId, now, AlertType.FAILED_JOBS, AlertLevel.WARNING,
                    String.format("%d job(s) en échec sur %s", metric.getFailedJobs(), nodeId)));
        }
    }

    private boolean shouldSendHeartbeat() {
        long now = System.currentTimeMillis();
        if (now - lastHeartbeatTime >= heartbeatIntervalMs) {
            lastHeartbeatTime = now;
            return true;
        }
        return false;
    }

    private void sendHeartbeat() {
        String payload = "{\"type\":\"HEARTBEAT\",\"nodeId\":\"" + nodeId
                + "\",\"timestamp\":" + System.currentTimeMillis() + "}";
        udpSender.enqueue(payload);
        LOGGER.fine("[" + nodeId + "] Heartbeat sent");
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
