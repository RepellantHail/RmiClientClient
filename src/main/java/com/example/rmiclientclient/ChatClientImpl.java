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
            // Verificar si el cliente ya está registrado
            if (!server.getUserList().contains(name)) {
                // Crear instancia del cliente y unirse al chat
                ChatClientImpl client = new ChatClientImpl(name, server);

                // Obtener la lista de usuarios del servidor y asociarla con la lista de usuarios en la interfaz de usuario del cliente
                List<String> userList = server.getUserList();
                Platform.runLater(() -> {
                    this.userList.setAll(userList);
                });

                // Iniciar el bucle principal para enviar mensajes
                client.run();
            }
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }
    public ChatClientImpl(String name, ChatServer server) throws RemoteException {
        super();
        this.name = name;
        this.server = server;
        server.register(name, this);
        this.userList = FXCollections.observableArrayList();
        this.chatArea = new TextArea();
        this.messageHistory = new HashMap<>();
    }

    public String getName() throws RemoteException {
        return name;
    }
    @Override
    public void updateUserList(List<String> userList) throws RemoteException {
        System.out.println("Usuarios en línea:");
        for (String user : userList) {
            System.out.println(user);
        }
        Platform.runLater(() -> {
            System.out.println("User List Update: " + userList); // Agregamos esta línea
            updateListView(userList); // Actualizamos la lista de usuarios
        });
    }

    private void updateListView(List<String> userList) {
        System.out.println("Updating List View"); // Agregamos esta línea
        this.userList.clear();
        this.userList.addAll(userList);
    }
    @Override
    public void notifyUserJoined(String userName) throws RemoteException {
        System.out.println(userName + " se ha unido al chat.");
    }
    private void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bienvenido al chat. Escribe 'salir' para salir.");
        while (true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("salir")) {
                try {
                    server.unregister(this);
                } catch (RemoteException e) {
                    // Manejar la excepción apropiadamente
                }
                break;
            }  else if (input.equalsIgnoreCase("historial")) {
                System.out.print("Ingrese el nombre del usuario: ");
                String recipient = scanner.nextLine();
                printMessageHistory(recipient);
            }else {
                String[] parts = input.split(" ", 3);
                if (parts.length == 3) {
                    String recipient = parts[0];
                    String message = parts[2];
                    try {
                        server.sendMessage(name, recipient, message);
                    } catch (RemoteException e) {
                        // Manejar la excepción apropiadamente
                    }
                } else {
                    System.out.println("Formato de mensaje incorrecto. El formato debe ser: <destinatario> mensaje");
                }
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

    private void printMessageHistory(String recipient) {
        try {
            List<String> messageHistory = server.getMessageHistory(name, recipient);
            System.out.println("Historial de mensajes con " + recipient + ":");
            for (String message : messageHistory) {
                System.out.println(message);
            }
        } catch (RemoteException e) {
            // Manejar la excepción apropiadamente
        }
    }


}