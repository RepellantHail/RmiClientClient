package com.example.rmiclientclient;

import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatClient extends Remote {
    void receiveMessage(String message) throws RemoteException;
    void updateUserListView(List<String> userList) throws RemoteException;

    void notifyUserJoined(String userName) throws RemoteException;

    void updateUserList(List<String> userList) throws RemoteException;
    void setChatArea(TextArea chatArea) throws RemoteException;
    String getName() throws RemoteException;
}
