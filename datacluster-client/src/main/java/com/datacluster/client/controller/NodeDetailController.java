package com.datacluster.client.controller;

import com.datacluster.client.service.RmiClientService;
import com.datacluster.common.model.Metric;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur de la vue détaillée d'un nœud.
 * Affiche un LineChart CPU/RAM/disque sur les 5 dernières minutes glissantes.
 */
public class NodeDetailController implements Initializable {

    private static final Logger LOGGER    = Logger.getLogger(NodeDetailController.class.getName());
    private static final int    MAX_POINTS = 100; // ~5 min à 3s/point

    @FXML private ComboBox<String>     cmbNode;
    @FXML private LineChart<Number, Number> chartMetrics;
    @FXML private NumberAxis           xAxis;
    @FXML private NumberAxis           yAxis;
    @FXML private Label                lblCurrentCpu;
    @FXML private Label                lblCurrentRam;
    @FXML private Label                lblCurrentDisk;

    private final XYChart.Series<Number, Number> cpuSeries  = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> ramSeries  = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> diskSeries = new XYChart.Series<>();

    private ScheduledService<List<Metric>> refreshService;
    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cpuSeries.setName("CPU");
        ramSeries.setName("RAM");
        diskSeries.setName("Disque");

        chartMetrics.setData(FXCollections.observableArrayList(cpuSeries, ramSeries, diskSeries));
        chartMetrics.setAnimated(false);
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(100);
        yAxis.setTickUnit(10);

        loadNodeList();
        cmbNode.valueProperty().addListener((obs, o, n) -> startRefresh(n));
    }

    private void loadNodeList() {
        try {
            RmiClientService rmi = RmiClientService.getInstance();
            if (!rmi.isConnected()) return;
            rmi.getClusterService().getAllNodes()
               .forEach(n -> cmbNode.getItems().add(n.getId()));
            if (!cmbNode.getItems().isEmpty()) {
                cmbNode.setValue(cmbNode.getItems().get(0));
            }
        } catch (RemoteException e) {
            LOGGER.log(Level.WARNING, "Cannot load node list", e);
        }
    }

    private void startRefresh(String nodeId) {
        if (refreshService != null) refreshService.cancel();
        cpuSeries.getData().clear();
        ramSeries.getData().clear();
        diskSeries.getData().clear();

        refreshService = new ScheduledService<>() {
            @Override
            protected Task<List<Metric>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<Metric> call() throws Exception {
                        RmiClientService rmi = RmiClientService.getInstance();
                        if (!rmi.isConnected() || nodeId == null) return Collections.emptyList();
                        return rmi.getStatsService().getMetricHistory(nodeId, 5);
                    }
                };
            }
        };
        refreshService.setPeriod(Duration.millis(3000));
        refreshService.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            List<Metric> history = (List<Metric>) e.getSource().getValue();
            Platform.runLater(() -> updateChart(history));
        });
        refreshService.setOnFailed(e ->
                LOGGER.log(Level.WARNING, "NodeDetail refresh failed", e.getSource().getException()));
        refreshService.start();
    }

    private void updateChart(List<Metric> history) {
        cpuSeries.getData().clear();
        ramSeries.getData().clear();
        diskSeries.getData().clear();

        // Utilise le timestamp relatif (secondes depuis le premier point) pour l'axe X
        if (history.isEmpty()) return;
        long t0 = history.get(0).getTimestamp();

        for (Metric m : history) {
            double x = (m.getTimestamp() - t0) / 1000.0;
            cpuSeries.getData().add(new XYChart.Data<>(x, m.getCpu()));
            ramSeries.getData().add(new XYChart.Data<>(x, m.getRam()));
            diskSeries.getData().add(new XYChart.Data<>(x, m.getDisk()));
        }

        Metric last = history.get(history.size() - 1);
        lblCurrentCpu.setText(String.format("%.1f%%", last.getCpu()));
        lblCurrentRam.setText(String.format("%.1f%%", last.getRam()));
        lblCurrentDisk.setText(String.format("%.1f%%", last.getDisk()));
    }

    /** Arrête le service de rafraîchissement. */
    public void shutdown() {
        if (refreshService != null) refreshService.cancel();
    }
}
