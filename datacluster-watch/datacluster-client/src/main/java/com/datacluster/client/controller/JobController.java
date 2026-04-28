package com.datacluster.client.controller;

import com.datacluster.client.service.RmiClientService;
import com.datacluster.common.enums.JobType;
import com.datacluster.common.model.Job;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur de la vue des jobs Big Data avec ProgressBar et filtres.
 */
public class JobController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(JobController.class.getName());
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    @FXML private TableView<Job>               tableJobs;
    @FXML private TableColumn<Job, String>     colJobId;
    @FXML private TableColumn<Job, String>     colNode;
    @FXML private TableColumn<Job, String>     colType;
    @FXML private TableColumn<Job, String>     colStatus;
    @FXML private TableColumn<Job, String>     colStart;
    @FXML private TableColumn<Job, Number>     colProgress;
    @FXML private ComboBox<String>             cmbNodeFilter;
    @FXML private ComboBox<String>             cmbTypeFilter;
    @FXML private Label                        lblCount;

    private final ObservableList<Job> allJobs = FXCollections.observableArrayList();
    private FilteredList<Job>         filtered;
    private ScheduledService<List<Job>> refreshService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        filtered = new FilteredList<>(allJobs, p -> true);
        tableJobs.setItems(filtered);

        List<String> types = new ArrayList<>(List.of("TOUS"));
        Arrays.stream(JobType.values()).map(Enum::name).forEach(types::add);
        cmbTypeFilter.setItems(FXCollections.observableArrayList(types));
        cmbTypeFilter.setValue("TOUS");

        cmbNodeFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        cmbTypeFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        startRefresh();
    }

    private void setupColumns() {
        colJobId.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getId()));
        colNode.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getNodeId()));
        colType.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getType().name()));
        colStatus.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStart.setCellValueFactory(c   -> new SimpleStringProperty(
                FMT.format(Instant.ofEpochMilli(c.getValue().getStartTime()))));
        colProgress.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getProgress()));

        colProgress.setCellFactory(tc -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                bar.setProgress(item.doubleValue() / 100.0);
                bar.setPrefWidth(120);
                setGraphic(bar);
            }
        });

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle(switch (item) {
                    case "RUNNING"   -> "-fx-text-fill: #3498db;";
                    case "COMPLETED" -> "-fx-text-fill: #2ecc71;";
                    case "FAILED"    -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                    default          -> "";
                });
            }
        });
    }

    private void startRefresh() {
        refreshService = new ScheduledService<>() {
            @Override
            protected Task<List<Job>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<Job> call() throws Exception {
                        RmiClientService rmi = RmiClientService.getInstance();
                        if (!rmi.isConnected()) return Collections.emptyList();
                        return rmi.getJobService().getAllJobs();
                    }
                };
            }
        };
        refreshService.setPeriod(Duration.millis(3000));
        refreshService.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            List<Job> jobs = (List<Job>) e.getSource().getValue();
            Platform.runLater(() -> {
                Set<String> nodes = new LinkedHashSet<>(List.of("TOUS"));
                jobs.forEach(j -> nodes.add(j.getNodeId()));
                String curNode = cmbNodeFilter.getValue();
                cmbNodeFilter.setItems(FXCollections.observableArrayList(nodes));
                cmbNodeFilter.setValue(curNode != null && nodes.contains(curNode) ? curNode : "TOUS");
                allJobs.setAll(jobs);
                applyFilter();
                lblCount.setText(filtered.size() + " job(s)");
            });
        });
        refreshService.setOnFailed(e ->
                LOGGER.log(Level.WARNING, "Job refresh failed", e.getSource().getException()));
        refreshService.start();
    }

    private void applyFilter() {
        String nodeFilter = cmbNodeFilter.getValue();
        String typeFilter = cmbTypeFilter.getValue();
        filtered.setPredicate(job -> {
            boolean n = nodeFilter == null || "TOUS".equals(nodeFilter) || nodeFilter.equals(job.getNodeId());
            boolean t = typeFilter == null || "TOUS".equals(typeFilter) || typeFilter.equals(job.getType().name());
            return n && t;
        });
        lblCount.setText(filtered.size() + " job(s)");
    }

    /** Arrête le service de rafraîchissement. */
    public void shutdown() {
        if (refreshService != null) refreshService.cancel();
    }
}
