package com.datacluster.client.controller;

import com.datacluster.client.service.RmiClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur de la vue statistiques.
 * Affiche un BarChart de comparaison inter-nœuds et les z-scores par nœud sélectionné.
 */
public class StatsController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(StatsController.class.getName());

    @FXML private BarChart<String, Number>    chartComparison;
    @FXML private CategoryAxis               xAxisBar;
    @FXML private NumberAxis                 yAxisBar;
    @FXML private Label                      lblZscoreCpu;
    @FXML private Label                      lblZscoreRam;
    @FXML private Label                      lblZscoreDisk;
    @FXML private javafx.scene.control.ComboBox<String> cmbZscoreNode;

    private ScheduledService<Map<String, Map<String, Double>>> refreshService;
    private ScheduledService<Map<String, Double>> zscoreService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chartComparison.setAnimated(false);
        yAxisBar.setAutoRanging(false);
        yAxisBar.setLowerBound(0);
        yAxisBar.setUpperBound(100);
        yAxisBar.setTickUnit(10);

        cmbZscoreNode.valueProperty().addListener((o, a, b) -> refreshZscores(b));
        startComparison();
    }

    private void startComparison() {
        refreshService = new ScheduledService<>() {
            @Override
            protected Task<Map<String, Map<String, Double>>> createTask() {
                return new Task<>() {
                    @Override
                    protected Map<String, Map<String, Double>> call() throws Exception {
                        RmiClientService rmi = RmiClientService.getInstance();
                        if (!rmi.isConnected()) return Collections.emptyMap();
                        return rmi.getStatsService().getClusterComparison();
                    }
                };
            }
        };
        refreshService.setPeriod(Duration.millis(5000));
        refreshService.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Double>> data =
                    (Map<String, Map<String, Double>>) e.getSource().getValue();
            Platform.runLater(() -> updateBarChart(data));
        });
        refreshService.setOnFailed(e ->
                LOGGER.log(Level.WARNING, "Stats refresh failed", e.getSource().getException()));
        refreshService.start();
    }

    private void updateBarChart(Map<String, Map<String, Double>> comparison) {
        chartComparison.getData().clear();

        XYChart.Series<String, Number> cpuSeries  = new XYChart.Series<>(); cpuSeries.setName("CPU");
        XYChart.Series<String, Number> ramSeries  = new XYChart.Series<>(); ramSeries.setName("RAM");
        XYChart.Series<String, Number> diskSeries = new XYChart.Series<>(); diskSeries.setName("Disque");

        List<String> nodes = new ArrayList<>(comparison.keySet());
        for (String nodeId : nodes) {
            Map<String, Double> stats = comparison.get(nodeId);
            cpuSeries.getData().add(new XYChart.Data<>(nodeId, stats.getOrDefault("cpu", 0.0)));
            ramSeries.getData().add(new XYChart.Data<>(nodeId, stats.getOrDefault("ram", 0.0)));
            diskSeries.getData().add(new XYChart.Data<>(nodeId, stats.getOrDefault("disk", 0.0)));
        }

        chartComparison.setData(FXCollections.observableArrayList(cpuSeries, ramSeries, diskSeries));

        // Mise à jour de la combo si de nouveaux nœuds sont apparus
        if (cmbZscoreNode.getItems().isEmpty()) {
            cmbZscoreNode.setItems(FXCollections.observableArrayList(nodes));
            if (!nodes.isEmpty()) cmbZscoreNode.setValue(nodes.get(0));
        }
    }

    private void refreshZscores(String nodeId) {
        if (zscoreService != null) zscoreService.cancel();
        if (nodeId == null) return;

        zscoreService = new ScheduledService<>() {
            @Override
            protected Task<Map<String, Double>> createTask() {
                return new Task<>() {
                    @Override
                    protected Map<String, Double> call() throws Exception {
                        RmiClientService rmi = RmiClientService.getInstance();
                        if (!rmi.isConnected()) return Collections.emptyMap();
                        return rmi.getStatsService().getZScores(nodeId);
                    }
                };
            }
        };
        zscoreService.setPeriod(Duration.millis(5000));
        zscoreService.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            Map<String, Double> scores = (Map<String, Double>) e.getSource().getValue();
            Platform.runLater(() -> {
                lblZscoreCpu.setText(String.format("%.2f", scores.getOrDefault("cpu", 0.0)));
                lblZscoreRam.setText(String.format("%.2f", scores.getOrDefault("ram", 0.0)));
                lblZscoreDisk.setText(String.format("%.2f", scores.getOrDefault("disk", 0.0)));
            });
        });
        zscoreService.start();
    }

    /** Arrête les services de rafraîchissement. */
    public void shutdown() {
        if (refreshService != null) refreshService.cancel();
        if (zscoreService  != null) zscoreService.cancel();
    }
}
