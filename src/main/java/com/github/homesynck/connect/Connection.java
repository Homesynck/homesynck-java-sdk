package com.github.homesynck.connect;

import java.util.HashMap;

import ch.kuon.phoenix.Socket;

public class Connection {

    private static Socket socket;

    private static String host = "wss://homesynck.anicetnougaret.fr/socket";

    private static String authToken;
    private static String userId;
    private static String directoryId;

    public static void setAuth_token(String auth_token) {
        authToken = auth_token;
    }

    public static void setUser_id(String user_id) {
        userId = user_id;
    }

    public static String getAuth_token() {
        return authToken;
    }

    public static String getUser_id() {
        return userId;
    }

    public static String getDirectoryId() {
        return directoryId;
    }

    public static void setDirectoryId(String directoryId) {
        Connection.directoryId = directoryId;
    }

    public static void setHost(String newHost){
        host = newHost;
    }


    public static Socket getSocket(){
        if (socket == null){
            newSocket();
        }
        return socket;
    }

    private static void newSocket() {

        Socket.Options opts = new Socket.Options();
        opts.setTimeout(5000);
        opts.setHeartbeatIntervalMs(100000);
        opts.setRejoinAfterMs((tries) -> tries * 500);
        opts.setReconnectAfterMs((tries) -> tries * 500);
        opts.setLogger((tag, msg) ->
        {
            System.out.println(tag + " " + msg);
            return null;
        });

        HashMap<String, Object> params = new HashMap<>();
        params.put("user_token", "tibo");
        opts.setParams(params); // params

        socket = new Socket(host, opts);
        socket.connect();

        System.out.println("Socket instantiation ->" +socket.toString());

        socket.onError((String msg) -> {
            System.out.println("An error occurred while we trying reach the socket connection!");
            return null;
        });
        socket.onClose((Integer code, String msg) -> {
            System.out.println("The connection closed!");
            return null;
        });
    }
}