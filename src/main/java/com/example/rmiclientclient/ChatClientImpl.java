package com.example.rmiclientclient;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListView;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChatClientImpl extends UnicastRemoteObject implements ChatClient {
    private ListView<String> userListView;
    private TextArea chatArea;
    ChatServer  server;
    private ObservableList<String> userList;
    private String name;

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

            // Iniciar el bucle principal para enviar mensajes
            client.run();
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }
    public ChatClientImpl(String name, ChatServer server) throws RemoteException {
        super();
        this.name = name;
        this.server = server;
        server.register(name, this);
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
    }
    @Override
    public void setUserListView(ListView<String> userListView)  throws RemoteException {
        this.userListView = userListView;
    }


    @Override
    public void notifyUserJoined(String userName) throws RemoteException {
        System.out.println(userName + " se ha unido al chat.");
    }


    private List<String> getUserList() {
        try {
            return server.getUserList();
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bienvenido al chat. Escribe 'salir' para salir.");
        while (true) {
            String message = scanner.nextLine();
            if (message.equalsIgnoreCase("salir")) {
                try {
                    server.unregister(this);
                } catch (RemoteException e) {
                    // Manejar la excepción apropiadamente
                }
                break;
            } else {
                try {
                    server.sendMessage(name, message);
                } catch (RemoteException e) {
                    // Manejar la excepción apropiadamente
                }
            }
        }
    }
    @Override
    public void sendMessage(String message) throws RemoteException {
        server.sendMessage(name, message);
    }

    public void setChatArea(TextArea chatArea) throws RemoteException{
        this.chatArea = chatArea;
    }
    public void appendMessage(String message) {
        // Aquí debes agregar el código necesario para mostrar el mensaje en el cliente
        // Puede ser mediante una etiqueta de texto, un cuadro de chat, etc.
        // Asegúrate de adaptar el código a tu interfaz de usuario específica
        System.out.println(message);
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        System.out.println(message);
    }



}
