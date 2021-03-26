package accounts;

import ch.kuon.phoenix.Channel;
import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;
import utils.VPSConnection;

import ch.kuon.phoenix.Socket;

import java.util.function.Function;


public class Session {

    private static Session session;
    private boolean phone;
    private String userId;

    private String name;

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

    /**
     * Allow user to register to the server.
     * @param username  username of the new user
     * @param password  password of the new user
     * @param token     the token for the new user
     * @param function  the function to execute if the new user was created
     */
    public void register(String username, String password, String token, Function<String[], Void> function){
        Socket socket;

        socket = VPSConnection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject registerParams = new JSONObject();
        registerParams.accumulate("register_token", token)
                .accumulate("login", username)
                .accumulate("password", password);

        connect(ch, socket, registerParams, "register", function);
    }

    /**
     * Allow user to login to an already created account.
     * @param username  username of the user
     * @param password  password of the user
     * @param function  the function to execute when you are connected
     */
    public void login(String username, String password, Function<String[], Void> function){
        Socket socket = VPSConnection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject connectionParams = new JSONObject();
        connectionParams.accumulate("login", username)
                .accumulate("password", password);

        connect(ch,socket,connectionParams,"login", function);
    }

    private void connect(@NotNull Channel ch, @NotNull Socket socket , JSONObject params, String event, Function<String[], Void> function){
        this.userId = null;
        ch.push(event, params, socket.getOpts().getTimeout()).receive("ok", msg -> {
            VPSConnection.setUser_id(msg.getString("user_id"));
            VPSConnection.setAuth_token(msg.getString("auth_token"));
            String[] str = new String[]{msg.getString("user_id"), msg.getString("auth_token")};
            function.apply(str);
            this.userId = msg.getString("user_id");
            return null;
        }).receive("error", msg -> {
            throw new ServerErrorException(msg.getString("error"));
        });
    }

    /**
     * Send the phone number to the server. This function is require before any creation of
     * a new user.
     * @param phoneNumber   phone number of the new user
     * @param function      the function to execute when the server response that this phone number is correct
     */
    public void phoneValidation(String phoneNumber, Function<String, Void> function){
        Socket socket = VPSConnection.getSocket();

        Channel ch =socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject jsonObject = new JSONObject();
        jsonObject.accumulate("phone", phoneNumber);

        this.phone = false;
        Object u;
        ch.push("validate_phone",jsonObject,socket.getOpts().getTimeout()).receive("ok", msg -> {
            this.phone = true;
            function.apply("");
            return null;
        }).receive("error", msg -> {
            throw new ServerErrorException(msg.getString("reason"));
        });
    }
}
