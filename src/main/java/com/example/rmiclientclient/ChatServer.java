package com.example.rmiclientclient;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatServer extends Remote {
    void register(String name,ChatClient client) throws RemoteException;

    void sendMessage(String sender, String message) throws RemoteException;
    void unregister(ChatClient client) throws RemoteException;
    void broadcastMessage(String sender,String message) throws RemoteException;
    void sendMessageToClient(String recipient, String message) throws RemoteException;
    List<String> getUserList() throws RemoteException;
}
