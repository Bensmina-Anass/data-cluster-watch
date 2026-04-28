package com.datacluster.client.controller;

import com.datacluster.client.service.RmiClientService;
import com.datacluster.common.enums.AlertLevel;
import com.datacluster.common.model.Alert;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur de la vue des alertes avec filtres et acquittement.
 */
public class AlertController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AlertController.class.getName());
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML private TableView<Alert>            tableAlerts;
    @FXML private TableColumn<Alert, String>  colTimestamp;
    @FXML private TableColumn<Alert, String>  colNode;
    @FXML private TableColumn<Alert, String>  colLevel;
    @FXML private TableColumn<Alert, String>  colType;
    @FXML private TableColumn<Alert, String>  colMessage;
    @FXML private TableColumn<Alert, String>  colAck;
    @FXML private ComboBox<String>            cmbNodeFilter;
    @FXML private ComboBox<String>            cmbLevelFilter;
    @FXML private Button                      btnAcknowledge;
    @FXML private Label                       lblCount;

    private final ObservableList<Alert>  allAlerts = FXCollections.observableArrayList();
    private FilteredList<Alert>          filtered;
    private ScheduledService<List<Alert>> refreshService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        filtered = new FilteredList<>(allAlerts, p -> true);
        tableAlerts.setItems(filtered);

        cmbLevelFilter.setItems(FXCollections.observableArrayList(
                "TOUS", "INFO", "WARNING", "CRITICAL"));
        cmbLevelFilter.setValue("TOUS");
        cmbLevelFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        cmbNodeFilter.valueProperty().addListener((o, a, b) -> applyFilter());

        btnAcknowledge.setOnAction(e -> acknowledgeSelected());
        startRefresh();
    }

    private void setupColumns() {
        colTimestamp.setCellValueFactory(c ->
                new SimpleStringProperty(FMT.format(Instant.ofEpochMilli(c.getValue().getTimestamp()))));
        colNode.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getNodeId()));
        colLevel.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getLevel().name()));
        colType.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getType().name()));
        colMessage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMessage()));
        colAck.setCellValueFactory(c     ->
                new SimpleStringProperty(c.getValue().isAcknowledged() ? "Oui" : "Non"));

        // Code couleur par niveau
        colLevel.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle(switch (item) {
                    case "CRITICAL" -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                    case "WARNING"  -> "-fx-text-fill: #f39c12;";
                    default         -> "-fx-text-fill: #3498db;";
                });
            }
        });
    }

    private void startRefresh() {
        refreshService = new ScheduledService<>() {
            @Override
            protected Task<List<Alert>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<Alert> call() throws Exception {
                        RmiClientService rmi = RmiClientService.getInstance();
                        if (!rmi.isConnected()) return Collections.emptyList();
                        return rmi.getAlertService().getAllAlerts();
                    }
                };
            }
        };
        refreshService.setPeriod(Duration.millis(3000));
        refreshService.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            List<Alert> alerts = (List<Alert>) e.getSource().getValue();
            Platform.runLater(() -> updateTable(alerts));
        });
        refreshService.setOnFailed(e ->
                LOGGER.log(Level.WARNING, "Alert refresh failed", e.getSource().getException()));
        refreshService.start();
    }

    private void updateTable(List<Alert> alerts) {
        // Préserve les nœuds disponibles dans le filtre
        Set<String> nodes = new LinkedHashSet<>();
        nodes.add("TOUS");
        alerts.forEach(a -> nodes.add(a.getNodeId()));
        String currentNode = cmbNodeFilter.getValue();
        cmbNodeFilter.setItems(FXCollections.observableArrayList(nodes));
        cmbNodeFilter.setValue(currentNode != null && nodes.contains(currentNode) ? currentNode : "TOUS");

        allAlerts.setAll(alerts);
        applyFilter();
        lblCount.setText(filtered.size() + " alerte(s)");
    }

    private void applyFilter() {
        String nodeFilter  = cmbNodeFilter.getValue();
        String levelFilter = cmbLevelFilter.getValue();

        Predicate<Alert> predicate = alert -> {
            boolean nodeMatch  = nodeFilter  == null || "TOUS".equals(nodeFilter)  || nodeFilter.equals(alert.getNodeId());
            boolean levelMatch = levelFilter == null || "TOUS".equals(levelFilter) || levelFilter.equals(alert.getLevel().name());
            return nodeMatch && levelMatch;
        };
        filtered.setPredicate(predicate);
        lblCount.setText(filtered.size() + " alerte(s)");
    }

    private void acknowledgeSelected() {
        Alert selected = tableAlerts.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isAcknowledged()) return;
        try {
            RmiClientService.getInstance().getAlertService().acknowledgeAlert(selected.getId());
            selected.setAcknowledged(true);
            tableAlerts.refresh();
        } catch (RemoteException e) {
            LOGGER.log(Level.WARNING, "Acknowledge failed: " + selected.getId(), e);
        }
    }

    /** Arrête le service de rafraîchissement. */
    public void shutdown() {
        if (refreshService != null) refreshService.cancel();
    }
}
