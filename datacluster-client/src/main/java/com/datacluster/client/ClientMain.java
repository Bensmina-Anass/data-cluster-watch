package com.datacluster.client;

import com.datacluster.client.service.RmiClientService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Point d'entrée de l'application JavaFX DataCluster Watch.
 */
public class ClientMain extends Application {

    private static final Logger LOGGER = Logger.getLogger(ClientMain.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception {
        connectToServer();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
                getClass().getResource("/css/dark-theme.css").toExternalForm());

        primaryStage.setTitle("DataCluster Watch — Monitoring");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);
        primaryStage.show();
        LOGGER.info("DataCluster Watch client started");
    }

    private void connectToServer() {
        RmiClientService rmi = RmiClientService.getInstance();
        try {
            rmi.connect();
        } catch (RemoteException | NotBoundException e) {
            LOGGER.log(Level.WARNING, "RMI connection failed, running in offline mode", e);
        }
    }

    /**
     * Lance l'application JavaFX.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        launch(args);
    }
}
