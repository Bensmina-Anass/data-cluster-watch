package com.datacluster.client.controller;

import com.datacluster.client.service.RmiClientService;
import com.datacluster.common.model.ClusterSummary;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur du tableau de bord principal.
 * Affiche les cards synthétiques : nœuds actifs, alertes, jobs, santé cluster.
 */
public class DashboardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    @FXML private Label lblActiveNodes;
    @FXML private Label lblDownNodes;
    @FXML private Label lblActiveJobs;
    @FXML private Label lblCriticalAlerts;
    @FXML private Label lblTotalAlerts;
    @FXML private Label lblClusterHealth;
    @FXML private Label lblAvgCpu;
    @FXML private Label lblAvgRam;
    @FXML private Label lblAvgDisk;
    @FXML private VBox  healthBar;
    @FXML private Label lblConnectionStatus;

    private ScheduledService<ClusterSummary> refreshService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startRefreshService();
    }

    private void startRefreshService() {
        refreshService = new ScheduledService<>() {
            @Override
            protected Task<ClusterSummary> createTask() {
                return new Task<>() {
                    @Override
                    protected ClusterSummary call() throws Exception {
                        RmiClientService rmi = RmiClientService.getInstance();
                        if (!rmi.isConnected()) return null;
                        return rmi.getClusterService().getClusterSummary();
                    }
                };
            }
        };

        refreshService.setPeriod(Duration.millis(3000));
        refreshService.setOnSucceeded(e -> {
            ClusterSummary summary = (ClusterSummary) e.getSource().getValue();
            Platform.runLater(() -> updateUI(summary));
        });
        refreshService.setOnFailed(e -> {
            LOGGER.log(Level.WARNING, "Dashboard refresh failed",
                    e.getSource().getException());
            Platform.runLater(() -> lblConnectionStatus.setText("Déconnecté"));
        });
        refreshService.start();
    }

    private void updateUI(ClusterSummary summary) {
        if (summary == null) {
            lblConnectionStatus.setText("Hors ligne");
            return;
        }
        lblConnectionStatus.setText("Connecté");
        lblActiveNodes.setText(String.valueOf(summary.getActiveNodes()));
        lblDownNodes.setText(String.valueOf(summary.getDownNodes()));
        lblActiveJobs.setText(String.valueOf(summary.getActiveJobs()));
        lblCriticalAlerts.setText(String.valueOf(summary.getCriticalAlerts()));
        lblTotalAlerts.setText(String.valueOf(summary.getTotalAlerts()));
        lblAvgCpu.setText(String.format("%.1f%%", summary.getAvgCpu()));
        lblAvgRam.setText(String.format("%.1f%%", summary.getAvgRam()));
        lblAvgDisk.setText(String.format("%.1f%%", summary.getAvgDisk()));

        double health = summary.getClusterHealth();
        lblClusterHealth.setText(String.format("%.0f%%", health));
        if (health >= 80) {
            healthBar.setStyle("-fx-background-color: #27ae60;");
        } else if (health >= 50) {
            healthBar.setStyle("-fx-background-color: #f39c12;");
        } else {
            healthBar.setStyle("-fx-background-color: #e74c3c;");
        }
    }

    /** Arrête le service de rafraîchissement. */
    public void shutdown() {
        if (refreshService != null) refreshService.cancel();
    }
}
