package com.example.rmiclientclient;

import javafx.application.Application;
import javafx.collections.ListChangeListener.Change;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Optional;

public class ChatApp extends Application  {
    private ChatServer server;
    private ChatClient client;
    private TextArea chatArea;
    private ListView<String> userListView = new ListView<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            server = (ChatServer) Naming.lookup("rmi://localhost/ChatServer");
            System.out.println("Servidor de chat iniciado.");

            String name = showNameDialog();
            client = new ChatClientImpl(name,server);
            server.register(name,client);
            System.out.println("Cliente registrado: " + name);

            userListView.setPrefWidth(200);
            userListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

            chatArea = new TextArea();
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

            HBox root = new HBox(userListView, chatBox);
            root.setSpacing(10);
            root.setPadding(new Insets(10));

            Scene scene = new Scene(root, 600, 400);

            primaryStage.setTitle("Chat App");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> {
                try {
                    server.unregister(client);
                    System.exit(0);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            });

            client.getUserList().addListener((Change<? extends String> change) -> {
                Platform.runLater(() -> {
                    try {
                        userListView.setItems(client.getUserList());
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
            });

            client.getMessageReceived().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                Platform.runLater(() -> {
                    chatArea.appendText(newValue + "\n");
                });
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

    private void openPrivateChat(String recipient) {
        // Lógica para abrir un chat privado con el usuario seleccionado
        // Puedes implementar esta parte según tus necesidades
    }
}