package com.datacluster.client.controller;

import com.datacluster.client.service.RmiClientService;
import com.datacluster.common.model.Metric;
import com.datacluster.common.model.Node;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur de la liste des nœuds avec code couleur selon le statut.
 */
public class NodeListController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(NodeListController.class.getName());

    @FXML private TableView<NodeRow>                   tableNodes;
    @FXML private TableColumn<NodeRow, String>         colId;
    @FXML private TableColumn<NodeRow, String>         colName;
    @FXML private TableColumn<NodeRow, String>         colType;
    @FXML private TableColumn<NodeRow, String>         colStatus;
    @FXML private TableColumn<NodeRow, Number>         colCpu;
    @FXML private TableColumn<NodeRow, Number>         colRam;
    @FXML private TableColumn<NodeRow, Number>         colDisk;
    @FXML private TableColumn<NodeRow, Number>         colJobs;

    private final ObservableList<NodeRow> rows = FXCollections.observableArrayList();
    private ScheduledService<List<Object[]>> refreshService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        tableNodes.setItems(rows);
        setupRowFactory();
        startRefresh();
    }

    private void setupColumns() {
        colId.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().nodeId));
        colName.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().name));
        colType.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().type));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        colCpu.setCellValueFactory(c    -> new SimpleDoubleProperty(c.getValue().cpu));
        colRam.setCellValueFactory(c    -> new SimpleDoubleProperty(c.getValue().ram));
        colDisk.setCellValueFactory(c   -> new SimpleDoubleProperty(c.getValue().disk));
        colJobs.setCellValueFactory(c   -> new SimpleDoubleProperty(c.getValue().activeJobs));

        colCpu.setCellFactory(tc  -> percentCell());
        colRam.setCellFactory(tc  -> percentCell());
        colDisk.setCellFactory(tc -> percentCell());
    }

    private TableCell<NodeRow, Number> percentCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                double v = item.doubleValue();
                setText(String.format("%.1f%%", v));
                if (v >= 90) setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                else if (v >= 75) setStyle("-fx-text-fill: #f39c12;");
                else setStyle("-fx-text-fill: #2ecc71;");
            }
        };
    }

    private void setupRowFactory() {
        tableNodes.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(NodeRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setStyle(""); return; }
                switch (row.status) {
                    case "ACTIVE" -> setStyle("-fx-background-color: #1a3a2a;");
                    case "IDLE"   -> setStyle("-fx-background-color: #2a2a1a;");
                    case "DOWN"   -> setStyle("-fx-background-color: #3a1a1a;");
                    default -> setStyle("");
                }
            }
        });
    }

    private void startRefresh() {
        refreshService = new ScheduledService<>() {
            @Override
            protected Task<List<Object[]>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<Object[]> call() throws Exception {
                        RmiClientService rmi = RmiClientService.getInstance();
                        if (!rmi.isConnected()) return Collections.emptyList();
                        List<Node>   nodes   = rmi.getClusterService().getAllNodes();
                        List<Metric> metrics = rmi.getClusterService().getLatestMetrics();
                        Map<String, Metric> metricMap = new HashMap<>();
                        for (Metric m : metrics) metricMap.put(m.getNodeId(), m);

                        List<Object[]> data = new ArrayList<>();
                        for (Node n : nodes) {
                            data.add(new Object[]{n, metricMap.get(n.getId())});
                        }
                        return data;
                    }
                };
            }
        };
        refreshService.setPeriod(Duration.millis(3000));
        refreshService.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            List<Object[]> data = (List<Object[]>) e.getSource().getValue();
            Platform.runLater(() -> {
                rows.clear();
                for (Object[] pair : data) {
                    Node   node   = (Node)   pair[0];
                    Metric metric = (Metric) pair[1];
                    rows.add(new NodeRow(node, metric));
                }
            });
        });
        refreshService.setOnFailed(e ->
                LOGGER.log(Level.WARNING, "NodeList refresh failed", e.getSource().getException()));
        refreshService.start();
    }

    /** Arrête le service de rafraîchissement. */
    public void shutdown() {
        if (refreshService != null) refreshService.cancel();
    }

    /** DTO interne pour la TableView. */
    public static class NodeRow {
        public final String nodeId;
        public final String name;
        public final String type;
        public final String status;
        public final double cpu;
        public final double ram;
        public final double disk;
        public final int    activeJobs;

        NodeRow(Node node, Metric metric) {
            this.nodeId     = node.getId();
            this.name       = node.getName();
            this.type       = node.getType().name();
            this.status     = node.getStatus().name();
            this.cpu        = metric != null ? metric.getCpu()        : 0;
            this.ram        = metric != null ? metric.getRam()        : 0;
            this.disk       = metric != null ? metric.getDisk()       : 0;
            this.activeJobs = metric != null ? metric.getActiveJobs() : 0;
        }
    }
}
