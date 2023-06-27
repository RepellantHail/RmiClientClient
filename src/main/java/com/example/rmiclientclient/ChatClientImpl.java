package com.example.rmiclientclient;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ChatClientImpl extends UnicastRemoteObject implements ChatClient {
    private Map<String, List<String>> messageHistory;
    private ListView<String> userListView  = new ListView<>();
    private TextArea chatArea;
    ChatServer  server;
    private ObservableList<String> userList;
    private String name;
    private StringProperty messageReceived = new SimpleStringProperty();
    public static void main(String[] args) {
        try {
            // Establecer conexión RMI con el servidor
            String serverUrl = "rmi://localhost:1099/ChatServer";
            ChatServer server = (ChatServer) Naming.lookup(serverUrl);

            // Solicitar al usuario su nombre
            System.out.print("Ingresa tu nombre: ");
            Scanner scanner = new Scanner(System.in);
            String name = scanner.nextLine();

            // Crear instancia del cliente y unirse al chat
            ChatClientImpl client = new ChatClientImpl(name, server);
            client.setupUI();
            client.startClient(name, server);
            // Iniciar el bucle principal para enviar mensajes
            client.run();
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }

    private void setupUI() {
        Platform.runLater(() -> {
            Stage primaryStage = new Stage();
            primaryStage.setTitle("Chat Client");

            VBox root = new VBox();
            root.setSpacing(10);
            root.setPadding(new Insets(10));

            // Agregar el área de chat
            chatArea = new TextArea();
            chatArea.setEditable(false);
            chatArea.setWrapText(true);
            chatArea.setPrefHeight(400);

            // Agregar la lista de usuarios conectados
            userListView.setPrefHeight(200);
            List<String> userList = null;
            try {
                userList = server.getUserList();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            List<String> finalUserList = userList;
            Platform.runLater(() -> {
                this.userList.setAll(finalUserList);
                userListView.setItems(this.userList);
            });

            // Agregar los componentes a la ventana
            root.getChildren().addAll(chatArea, userListView);
            Scene scene = new Scene(root, 400, 600);
            primaryStage.setScene(scene);
            primaryStage.show();

            // Configurar el cliente con los componentes de la interfaz de usuario
            try {
                setChatArea(chatArea);
                System.out.println("User List: ChatClientSetupUI" + server.getUserList()); // Agregamos esta línea
                updateListView(server.getUserList());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }
    private void startClient(String name, ChatServer server) {
        try {
            if (!server.getUserList().contains(name)) {
                ChatClientImpl client = new ChatClientImpl(name, server);
                List<String> userList = server.getUserList();
                Platform.runLater(() -> {
                    try {
                        updateUserList(userList);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    this.userList.setAll(userList);
                });
                client.run();
            }
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }
    public ChatClientImpl(String name, ChatServer server) throws RemoteException {
        this.name = name;
        this.server = server;
        messageHistory = new HashMap<>();
        userList = FXCollections.observableArrayList();
    }

    public String getName() throws RemoteException {
        return name;
    }
    @Override
    public void updateUserList(List<String> userList) throws RemoteException {
        Platform.runLater(() -> {
            this.userList.setAll(userList);
        });
    }

    private void updateListView(List<String> userList) {
        Platform.runLater(() -> {
            userListView.getItems().clear();
            userListView.getItems().addAll(userList);
        });
    }
    @Override
    public void notifyUserJoined(String userName) throws RemoteException {
        System.out.println(userName + " se ha unido al chat.");
    }
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String message = scanner.nextLine();
            try {
                server.broadcastMessage(name, message);
                addMessageToHistory(name, "Broadcast", name + ": " + message);
                Platform.runLater(() -> chatArea.appendText(name + ": " + message + "\n"));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public ObservableList<String> getUserList() {
        return userList;
    }
    @Override
    public StringProperty getMessageReceived() {
        return messageReceived;
    }
    public void setChatArea(TextArea chatArea) throws RemoteException{
        this.chatArea = chatArea;
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        System.out.println(message);
        Platform.runLater(() -> {
            this.chatArea.appendText(message + "\n");
        });
    }

    @Override
    public List<String> getMessageHistory(String sender) throws RemoteException {
        if (messageHistory.containsKey(sender)) {
            return messageHistory.get(sender);
        }
        return new ArrayList<>();
    }

    private void addMessageToHistory(String sender, String recipient, String message) {
        messageHistory.computeIfAbsent(sender, k -> new ArrayList<>())
                .add(message);
    }

    @Override
    public void updateChatArea(List<String> messages) throws RemoteException {
        StringBuilder chatText = new StringBuilder();
        for (String message : messages) {
            chatText.append(message).append("\n");
        }
        Platform.runLater(() -> chatArea.setText(chatText.toString()));
    }

    @Override
    public void receiveChatHistory(String sender, String recipient, List<String> history) throws RemoteException {
        Platform.runLater(() -> {
            if (recipient.equals(name)) {
                chatArea.clear();
                for (String message : history) {
                    chatArea.appendText(message + "\n");
                }
            }
        });
    }

    private String getMessageKey(String sender, String recipient) {
        return sender + "_" + recipient;
    }

    @Override
    public void clearChatArea() throws RemoteException {
        Platform.runLater(() -> chatArea.clear());
    }

}