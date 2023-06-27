package com.example.rmiclientclient;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatClient extends Remote {
    void receiveMessage(String message) throws RemoteException;
    List<String> getMessageHistory(String sender) throws RemoteException;
    void notifyUserJoined(String userName) throws RemoteException;
    void updateUserList(List<String> userList) throws RemoteException;
    void setChatArea(TextArea chatArea) throws RemoteException;
    String getName() throws RemoteException;
    ObservableList<String> getUserList() throws RemoteException;
    StringProperty getMessageReceived() throws RemoteException;
    void updateChatArea(List<String> messages) throws RemoteException;
    void clearChatArea() throws RemoteException;
    public void receiveChatHistory(String sender, String recipient, List<String> history) throws RemoteException;
}