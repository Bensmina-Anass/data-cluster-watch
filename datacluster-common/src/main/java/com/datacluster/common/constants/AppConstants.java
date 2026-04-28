package com.datacluster.common.constants;

/**
 * Constantes globales de l'application.
 * Les valeurs effectives sont surchargées par les fichiers application.properties.
 */
public final class AppConstants {

    private AppConstants() {}

    // ─── Réseau ────────────────────────────────────────────────────────────────
    public static final int DEFAULT_UDP_PORT         = 5000;
    public static final int DEFAULT_TCP_PORT         = 6000;
    public static final int DEFAULT_RMI_PORT         = 1099;
    public static final String DEFAULT_SERVER_HOST   = "localhost";
    public static final String RMI_SERVICE_NAME      = "ClusterService";

    // ─── Intervalles (ms) ──────────────────────────────────────────────────────
    public static final long METRICS_INTERVAL_MS     = 3_000L;
    public static final long HEARTBEAT_INTERVAL_MS   = 10_000L;
    public static final long HEARTBEAT_TIMEOUT_MS    = 30_000L;
    public static final long HEARTBEAT_CHECK_MS      = 15_000L;
    public static final long RMI_REFRESH_MS          = 3_000L;

    // ─── Seuils par défaut ─────────────────────────────────────────────────────
    public static final double CPU_WARNING_THRESHOLD    = 75.0;
    public static final double CPU_CRITICAL_THRESHOLD   = 90.0;
    public static final double RAM_WARNING_THRESHOLD    = 70.0;
    public static final double RAM_CRITICAL_THRESHOLD   = 85.0;
    public static final double DISK_WARNING_THRESHOLD   = 80.0;
    public static final double DISK_CRITICAL_THRESHOLD  = 95.0;

    // ─── Statistiques ─────────────────────────────────────────────────────────
    public static final int MOVING_AVG_WINDOW        = 20;
    public static final int HISTORY_WINDOW_MINUTES   = 5;

    // ─── UDP ──────────────────────────────────────────────────────────────────
    public static final int UDP_BUFFER_SIZE          = 65_535;
    public static final String HEARTBEAT_PAYLOAD     = "{\"type\":\"HEARTBEAT\"}";

    // ─── RMI ──────────────────────────────────────────────────────────────────
    public static final String RMI_ALERT_SERVICE     = "AlertService";
    public static final String RMI_JOB_SERVICE       = "JobService";
    public static final String RMI_STATS_SERVICE     = "StatsService";
    public static final String RMI_CONFIG_SERVICE    = "ConfigService";
}
