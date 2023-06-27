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
            ChatServerImpl server = new ChatServerImpl();
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
            sendChatHistory(client, name);
            updateUserList();
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
        broadcastUserList(getUserList());
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
        addMessageToHistory(sender, recipient, formattedMessage);
        for (ChatClient client : clients) {
            if (client.getName().equals(recipient)) {
                client.receiveMessage(formattedMessage);
                return;
            }
        }
        throw new RemoteException("El destinatario no está conectado.");
    }
    @Override
    public List<String> getMessageHistory(String sender, String recipient) throws RemoteException {
        if (messageHistory.containsKey(sender)) {
            Map<String, List<String>> senderHistory = messageHistory.get(sender);
            if (senderHistory.containsKey(recipient)) {
                return senderHistory.get(recipient);
            } else {
                senderHistory.put(recipient, new ArrayList<>());  // Agregar esta línea
                return senderHistory.get(recipient);
            }
        } else {
            messageHistory.put(sender, new HashMap<>());  // Agregar esta línea
            messageHistory.get(sender).put(recipient, new ArrayList<>());  // Agregar esta línea
            return messageHistory.get(sender).get(recipient);
        }
    }

    @Override
    public void addMessageToHistory(String sender, String recipient, String message) {
        messageHistory.computeIfAbsent(sender, k -> new HashMap<>());
        messageHistory.get(sender).computeIfAbsent(recipient, k -> new ArrayList<>()).add(message);
    }

    private void sendChatHistory(ChatClient client, String sender) {
        try {
            for (String recipient : getUserList()) {
                List<String> history = getMessageHistory(sender, recipient);
                client.receiveChatHistory(sender, recipient, history);
            }
        } catch (RemoteException e) {
            // Manejar la excepción apropiadamente
        }
    }

    private void sendChatHistoryToClient(ChatClient client, String sender, String recipient) {
        try {
            List<String> history = getMessageHistory(sender, recipient);
            client.receiveChatHistory(sender, recipient, history);
        } catch (RemoteException e) {
            // Manejar la excepción apropiadamente
        }
    }

    private void updateUserList() {
        List<String> userList = null;
        try {
            userList = getUserList();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        for (ChatClient client : clients) {
            try {
                client.updateUserList(userList);
            } catch (RemoteException e) {
                // Manejar la excepción apropiadamente
            }
        }
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