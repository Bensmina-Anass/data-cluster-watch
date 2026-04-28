package com.datacluster.agent.simulation;

import com.datacluster.common.enums.JobStatus;
import com.datacluster.common.enums.JobType;
import com.datacluster.common.enums.NodeType;
import com.datacluster.common.model.Job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Simule le cycle de vie des jobs Big Data sur un nœud.
 * Chaque type de job a une durée réaliste et un taux d'échec de 5 %.
 */
public class JobSimulator {

    private static final Logger LOGGER = Logger.getLogger(JobSimulator.class.getName());

    /** Durée min/max en secondes par type de job. */
    private static final int[][] JOB_DURATIONS = {
            {600,  1800},  // ETL_VENTES    10–30 min
            {1800, 3600},  // TRAINING_ML   30–60 min
            {300,  900},   // LOG_ANALYSIS   5–15 min
            {180,  600},   // DATA_CLEANING  3–10 min
            {120,  480},   // STAT_AGGREGATION 2–8 min
    };

    private static final double FAILURE_RATE      = 0.05;
    private static final int    MAX_CONCURRENT_JOBS = 3;

    private final String    nodeId;
    private final NodeType  nodeType;
    private final Random    random;
    private final List<Job> activeJobs;
    private final List<Job> completedJobs;

    private ScheduledExecutorService scheduler;
    private volatile boolean running;

    /**
     * @param nodeId   identifiant du nœud simulé
     * @param nodeType type de nœud (influence les types de jobs lancés)
     */
    public JobSimulator(String nodeId, NodeType nodeType) {
        this.nodeId        = nodeId;
        this.nodeType      = nodeType;
        this.random        = new Random();
        this.activeJobs    = new CopyOnWriteArrayList<>();
        this.completedJobs = new CopyOnWriteArrayList<>();
    }

    /** Démarre le simulateur. */
    public void start() {
        running   = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "job-sim-" + nodeId);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::tick, 5, 10, TimeUnit.SECONDS);
        LOGGER.fine("[" + nodeId + "] JobSimulator started");
    }

    /** Arrête le simulateur proprement. */
    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void tick() {
        if (!running) return;
        updateActiveJobs();
        maybeStartNewJob();
    }

    private void updateActiveJobs() {
        long now = System.currentTimeMillis();
        List<Job> toRemove = new ArrayList<>();

        for (Job job : activeJobs) {
            long elapsedSec = (now - job.getStartTime()) / 1000L;
            long durationSec = getDurationFor(job.getType());

            double progress = Math.min(100.0, (elapsedSec * 100.0) / durationSec);
            job.setProgress(progress);

            if (progress >= 100.0) {
                if (random.nextDouble() < FAILURE_RATE) {
                    job.setStatus(JobStatus.FAILED);
                    LOGGER.warning("[" + nodeId + "] Job FAILED: " + job.getId()
                            + " type=" + job.getType());
                } else {
                    job.setStatus(JobStatus.COMPLETED);
                    LOGGER.fine("[" + nodeId + "] Job completed: " + job.getId());
                }
                job.setEndTime(now);
                toRemove.add(job);
                completedJobs.add(job);
            }
        }
        activeJobs.removeAll(toRemove);
    }

    private void maybeStartNewJob() {
        if (activeJobs.size() >= MAX_CONCURRENT_JOBS) return;
        if (random.nextDouble() > 0.4) return;

        JobType type = pickJobType();
        Job job = new Job(nodeId, type, System.currentTimeMillis());
        activeJobs.add(job);
        LOGGER.info("[" + nodeId + "] New job started: " + job.getId() + " type=" + type);
    }

    private JobType pickJobType() {
        JobType[] types = JobType.values();
        if (nodeType == NodeType.MASTER) {
            // Le master préfère les agrégations et analyses de logs
            return random.nextBoolean() ? JobType.STAT_AGGREGATION : JobType.LOG_ANALYSIS;
        }
        if (nodeType == NodeType.STORAGE) {
            return random.nextBoolean() ? JobType.DATA_CLEANING : JobType.ETL_VENTES;
        }
        return types[random.nextInt(types.length)];
    }

    private long getDurationFor(JobType type) {
        int[] range = JOB_DURATIONS[type.ordinal()];
        return range[0] + random.nextInt(range[1] - range[0]);
    }

    /**
     * Retourne le nombre de jobs actifs au moment de l'appel.
     *
     * @return nombre de jobs en statut {@code RUNNING}
     */
    public int getActiveJobCount() {
        return activeJobs.size();
    }

    /**
     * Retourne le nombre de jobs en échec depuis le dernier appel à {@link #resetFailedCount()}.
     *
     * @return nombre de jobs échoués
     */
    public int getFailedJobCount() {
        return (int) completedJobs.stream()
                .filter(j -> j.getStatus() == JobStatus.FAILED)
                .count();
    }

    /** Réinitialise le compteur d'échecs (appelé après avoir émis la métrique). */
    public void resetFailedCount() {
        completedJobs.removeIf(j -> j.getStatus() == JobStatus.FAILED);
    }

    /**
     * Retourne une copie non modifiable des jobs actifs courants.
     *
     * @return liste de jobs actifs
     */
    public List<Job> getActiveJobs() {
        return Collections.unmodifiableList(new ArrayList<>(activeJobs));
    }
}
