package com.example.rmiclientclient;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatServerImpl extends UnicastRemoteObject implements ChatServer {
    private static final int RMI_PORT = 1099;
    private Map<String, Map<String, List<String>>> messageHistory;
    private List<ChatClient> clients;
    public ChatServerImpl() throws RemoteException {
        clients = new ArrayList<>();
        messageHistory = new HashMap<>();
    }

    public static void main(String[] args) {
        try {
            // Crear instancia del servidor
            ChatServerImpl server = new ChatServerImpl();

            // Registrar el servidor en el registro RMI
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            registry.rebind("ChatServer", server);

            System.out.println("Servidor RMI en ejecución...");
        } catch (RemoteException e) {
            System.err.println("Error al iniciar el servidor RMI: " + e.getMessage());
        }
    }

    @Override
    public void register(String name, ChatClient client) throws RemoteException {
        if (!clients.contains(client)) {
            clients.add(client);
            client.notifyUserJoined(name);
            notifyClients(client);
            broadcastUserList(getUserList());
        }
    }

    private void notifyClients(ChatClient newClient) {
        try {
            List<String> userList = getUserList();
            newClient.updateUserList(userList);
        } catch (RemoteException e) {
            // Manejar la excepción apropiadamente
        }
    }

    @Override
    public List<String> getUserList() throws RemoteException {
        List<String> userList = new ArrayList<>();
        for (ChatClient client : clients) {
            userList.add(client.getName());
        }
        return userList;
    }
    @Override
    public void unregister(ChatClient client) throws RemoteException {
        clients.remove(client);
        System.out.println("Cliente eliminado: " + client.getName());
        broadcastUserList(getUserList()); // Actualizamos la lista de usuarios en los clientes restantes// Agregamos esta línea para actualizar las listas de usuarios en los clientes existentes
    }
    @Override
    public void broadcastMessage(String sender, String message) throws RemoteException {
        String formattedMessage = sender + ": " + message;
        for (ChatClient client : clients) {
            client.receiveMessage(formattedMessage);
        }
    }

    private void broadcastUserList(List<String> userList) {
        for (ChatClient client : clients) {
            try {
                client.updateUserList(userList);
            } catch (RemoteException e) {
                // Manejar la excepción apropiadamente
            }
        }
    }
    @Override
    public synchronized void sendMessage(String sender, String recipient, String message) throws RemoteException {
        String formattedMessage = sender + ": " + message;
        for (ChatClient client : clients) {
            if (client.getName().equals(sender)) {
                client.receiveMessage(formattedMessage);
                addMessageToHistory(sender, recipient, formattedMessage);
            } else if (client.getName().equals(recipient)) {
                client.receiveMessage("[Private] " + sender + ": " + message);
                addMessageToHistory(sender, recipient, "[Private] " + sender + ": " + message);
            }
        }
    }
    @Override
    public List<String> getMessageHistory(String sender, String recipient) throws RemoteException {
        if (messageHistory.containsKey(sender)) {
            Map<String, List<String>> senderHistory = messageHistory.get(sender);
            if (senderHistory.containsKey(recipient)) {
                return senderHistory.get(recipient);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void addMessageToHistory(String sender, String recipient, String message) {
        messageHistory.computeIfAbsent(sender, k -> new HashMap<>())
                .computeIfAbsent(recipient, k -> new ArrayList<>())
                .add(message);
    }

    @Override
    public void sendMessageToClient(String recipient, String message) throws RemoteException {
        for (ChatClient client : clients) {
            if (client.getName().equals(recipient)) {
                String formattedMessage = "[Private] " + client.getName() + ": " + message;
                client.receiveMessage(formattedMessage);
                break;
            }
        }
    }


}