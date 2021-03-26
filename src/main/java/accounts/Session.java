package accounts;

import ch.kuon.phoenix.Channel;
import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;
import utils.VPSConnection;

import ch.kuon.phoenix.Socket;


public class Session {

    private static Session session;

    private String name;
    private boolean connected;

    private Session(){
    }

    public static Session getSession(){
        if (session == null){
            session = new Session();
        }
        return session;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean register(String username, String password, String token){
        Socket socket = VPSConnection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject registerParams = new JSONObject();
        registerParams.accumulate("register_token", token)
                .accumulate("login", username)
                .accumulate("password", password);

        connect(ch, socket, registerParams, "register");
        return this.connected;
    }

    public boolean login(String username, String password){
        Socket socket = VPSConnection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject connectionParams = new JSONObject();
        connectionParams.accumulate("login", username)
                .accumulate("password", password);

        connect(ch,socket,connectionParams,"login");
        return this.connected;
    }

    private void connect(@NotNull Channel ch, @NotNull Socket socket , JSONObject params, String event){
        ch.push(event, params, socket.getOpts().getTimeout()).receive("ok", msg -> {
            this.connected = true;
            return null;
        }).receive("error", msg -> {
            this.connected = false;
            return null;
        });
    }

    private boolean phoneValidation(String phoneNumber){
        Socket socket = VPSConnection.getSocket();

        Channel ch =socket.channel("auth:lobby", new JSONObject());

        return false;
    }
}
