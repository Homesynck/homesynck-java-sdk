package com.github.homesynck.connect;

import ch.kuon.phoenix.Channel;
import ch.kuon.phoenix.Socket;
import com.github.homesynck.ExceptionParser;
import com.github.homesynck.Response;
import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class Session {

    private static Session session;
    private boolean phone;

    private String name;

    private Session() {
    }

    /**
     * Get the unique instance for the session
     *
     * @return the session
     */
    public static Session getSession() {
        if (session == null) {
            session = new Session();
        }
        return session;
    }

    /**
     * Get the name set for the session
     *
     * @return the name of the session
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for the session
     *
     * @param name the new name of the session
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Allow user to register to the server.
     *  @param username        username of the new user
     * @param password        password of the new user
     * @param token           the token for the new user
     * @return
     */
    public Response register(String username, String password, String token) {
        Socket socket;

        socket = Connection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject registerParams = new JSONObject();
        registerParams.accumulate("register_token", token)
                .accumulate("login", username)
                .accumulate("password", password);

        try {
           String registerRes = connect(ch, socket, registerParams, "register").get();
            return new Response(true, registerRes);
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            String message = ExceptionParser.parse(e.getMessage());
            return new Response(false, message);

        }
    }

    /**
     * Allow user to login to an already created account.
     *
     * @param username        username of the user
     * @param password        password of the user
     */
    public Response login(String username, String password) {
        Socket socket = Connection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject connectionParams = new JSONObject();
        connectionParams.accumulate("login", username)
                .accumulate("password", password);

        try {
            String loginRes = connect(ch, socket, connectionParams, "login").get();
            System.out.println("ici");
            return new Response(true, loginRes);
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            String message = ExceptionParser.parse(e.getMessage());
            return new Response(false, message);
        }
    }

    public Future<String> connect(@NotNull ch.kuon.phoenix.Channel ch, @NotNull Socket socket, JSONObject params, String event) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        ch.push(event, params, socket.getOpts().getTimeout()).receive("ok", msg -> {
            Connection.setUser_id(msg.getString("user_id"));
            Connection.setAuth_token(msg.getString("auth_token"));
            String[] str = new String[]{msg.getString("user_id"), msg.getString("auth_token")};
            completableFuture.complete(Arrays.toString(str));
            return null;
        }).receive("error", msg -> {
            completableFuture.obtrudeException(new Exception(msg.getString("reason")));
            return null;
        }).receive("timeout", msg -> {
            completableFuture.obtrudeException(new Exception("Channel timeout"));
            return null;
        });

        return completableFuture;
    }

    /**
     * Send the phone number to the server. This function is require before any creation of
     * a new user.
     *
     * @param phoneNumber     phone number of the new user
     */
    public Response phoneValidation(String phoneNumber) {
        Socket socket = Connection.getSocket();

        Channel ch = socket.channel("auth:lobby", new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject jsonObject = new JSONObject();
        jsonObject.accumulate("phone", phoneNumber);

        this.phone = false;

        try {
            String validPhone = pushPhoneValidation(ch, socket, jsonObject).get();
            return new Response(true, validPhone);
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            String message = ExceptionParser.parse(e.getMessage());
            return new Response(false, message);
        }
    }

    public Future<String> pushPhoneValidation(@NotNull ch.kuon.phoenix.Channel ch, @NotNull Socket socket, JSONObject params) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        ch.push("validate_phone", params, socket.getOpts().getTimeout()).receive("ok", msg -> {
            this.phone = true;
            completableFuture.complete("");
            return null;
        }).receive("error", msg -> {
            completableFuture.obtrudeException(new Exception(msg.getString("reason")));
            return null;
        }).receive("timeout", msg -> {
            completableFuture.obtrudeException(new Exception("Channel timeout"));
            return null;
        });

        return completableFuture;
    }
}
