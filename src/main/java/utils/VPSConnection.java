package utils;

import java.util.HashMap;

import ch.kuon.phoenix.Socket;

public class VPSConnection {

    private static Socket socket;

    private static String authToken;
    private static String userId;

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

    static {
        socket = getSocket();
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

        socket = new Socket("wss://homesynck.anicetnougaret.fr/socket", opts);
        socket.connect();

        System.out.println("Socket instanciation ->" +socket.toString());

        socket.onError((String msg) -> {
            System.out.println("There was an error with the connection!");
            return null;
        });
        socket.onClose((Integer code, String msg) -> {
            System.out.println("The connection closed!");
            return null;
        });
    }
}