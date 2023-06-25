package com.example.rmiclientclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Optional;

public class ChatApp extends Application  {
    private ChatServer server;
    private ChatClient client;
    private ObservableList<String> userList;
    private ListView<String> userListView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            server = (ChatServer) Naming.lookup("rmi://localhost/ChatServer");
            Naming.rebind("ChatServer", server);
            System.out.println("Servidor de chat iniciado.");

            String name = showNameDialog();
            client = new ChatClientImpl(name,server);
            server.register(name,client);
            System.out.println("Cliente registrado: " + name);

            userList = FXCollections.observableArrayList();
            userListView = new ListView<>(userList);
            userListView.setPrefWidth(200);
            userListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            userListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                // Actualizar el destinatario del mensaje cuando se selecciona un usuario de la lista
                // Puedes usar newValue para obtener el nombre del usuario seleccionado
                String recipient = newValue;
            });

            TextArea chatArea = new TextArea();
            chatArea.setEditable(false);
            client.setChatArea(chatArea);

            TextField messageField = new TextField();
            Button sendButton = new Button("Enviar");
            sendButton.setOnAction(e -> {
                String recipient = userListView.getSelectionModel().getSelectedItem();
                if (recipient != null) {
                    String message = messageField.getText().trim();
                    if (!message.isEmpty()) {
                        try {
                            server.sendMessageToClient(recipient, client.getName() + ": " + message);
                            messageField.clear();
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    }
                }else {
                    // Mostrar un mensaje de error si no se ha seleccionado ningún destinatario
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Por favor, seleccione un destinatario.");
                    alert.showAndWait();
                }
            });

            VBox chatBox = new VBox(10, chatArea, messageField, sendButton);
            chatBox.setPadding(new Insets(10));

            BorderPane root = new BorderPane();
            root.setLeft(userListView);
            root.setCenter(chatBox);

            primaryStage.setTitle("Chat");
            primaryStage.setScene(new Scene(root, 400, 300));
            primaryStage.setOnCloseRequest(e -> {
                try {
                    server.unregister(client);
                    System.exit(0);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            });
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String showNameDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nombre de usuario");
        dialog.setHeaderText(null);
        dialog.setContentText("Ingrese su nombre:");

        Optional<String> result = dialog.showAndWait();
        return result.orElse("Usuario");
    }
}
