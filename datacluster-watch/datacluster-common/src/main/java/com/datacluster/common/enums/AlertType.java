package com.datacluster.common.enums;

/**
 * Type d'alerte pouvant être émise par le moteur d'alertes.
 */
public enum AlertType {
    CPU_HIGH,
    RAM_HIGH,
    DISK_HIGH,
    HEARTBEAT_MISSING,
    FAILED_JOBS,
    NODE_DOWN,
    ANOMALY_DETECTED
}
