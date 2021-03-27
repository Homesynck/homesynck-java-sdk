package com.github.homesynck.accounts;

import ch.kuon.phoenix.Channel;
import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;
import com.github.homesynck.utils.*;


import ch.kuon.phoenix.Socket;

import java.util.function.Consumer;


public class Session {

    private static Session session;
    private boolean phone;

    private String name;

    private Session(){
    }

    /**
     * Get the unique instance for the session
     * @return the session
     */
    public static Session getSession(){
        if (session == null){
            session = new Session();
        }
        return session;
    }

    /**
     * Get the name set for the session
     * @return the name of the session
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for the session
     * @param name the new name of the session
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Allow user to register to the server.
     * @param username          username of the new user
     * @param password          password of the new user
     * @param token             the token for the new user
     * @param successConsumer   Consumer that receive a array with the auth_token and the user_id when
     *                          the register is successful
     * @param errorConsumer     Consumer that receive the error from the server when the registration
     *                          fail
     */
    public void register(String username, String password, String token,
                         Consumer<String[]> successConsumer, Consumer<String> errorConsumer){
        Socket socket;

        socket = Connection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject registerParams = new JSONObject();
        registerParams.accumulate("register_token", token)
                .accumulate("login", username)
                .accumulate("password", password);

        connect(ch, socket, registerParams, "register", successConsumer, errorConsumer);
    }

    /**
     * Allow user to login to an already created account.
     * @param username  username of the user
     * @param password  password of the user
     * @param successConsumer   Consumer that receive a array with the auth_token and the user_id when
     *                          the login is successful. The auth_token and the user_id are stored by the
     *                          API and are just returned for information
     * @param errorConsumer     Consumer that receive the error from the server when the login
     *                          fail
     */
    public void login(String username, String password,
                      Consumer<String[]> successConsumer, Consumer<String> errorConsumer){
        Socket socket = Connection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject connectionParams = new JSONObject();
        connectionParams.accumulate("login", username)
                .accumulate("password", password);

        connect(ch,socket,connectionParams,"login", successConsumer, errorConsumer);
    }

    private void connect(@NotNull Channel ch, @NotNull Socket socket , JSONObject params, String event,
                         Consumer<String[]> successFunction, Consumer<String> errorFunction){
        ch.push(event, params, socket.getOpts().getTimeout()).receive("ok", msg -> {
            Connection.setUser_id(msg.getString("user_id"));
            Connection.setAuth_token(msg.getString("auth_token"));
            String[] str = new String[]{msg.getString("user_id"), msg.getString("auth_token")};
            successFunction.accept(str);
            return null;
        }).receive("error", msg -> {
            errorFunction.accept(msg.getString("reason"));
            return null;
        });
    }

    /**
     * Send the phone number to the server. This function is require before any creation of
     * a new user.
     * @param phoneNumber       phone number of the new user
     * @param successConsumer   Consumer called when the phone is send to the server
     * @param errorConsumer     Consumer called if the server return an error
     */
    public void phoneValidation(String phoneNumber, Consumer<String> successConsumer, Consumer<String> errorConsumer){
        Socket socket = Connection.getSocket();

        Channel ch =socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject jsonObject = new JSONObject();
        jsonObject.accumulate("phone", phoneNumber);

        this.phone = false;
        Object u;
        ch.push("validate_phone",jsonObject,socket.getOpts().getTimeout()).receive("ok", msg -> {
            this.phone = true;
            successConsumer.accept("");
            return null;
        }).receive("error", msg -> {
            errorConsumer.accept(msg.getString("reason"));
            return null;
        });
    }
}
