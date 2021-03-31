package com.github.homesynck.connect;

import ch.kuon.phoenix.Channel;
import ch.kuon.phoenix.Socket;
import com.github.homesynck.ExceptionParser;
import com.github.homesynck.Response;
import com.github.openjson.JSONObject;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Class to connect to the Directory on the Homesynck server.
 */
public class Directory {
    private static final String topic = "directories:lobby";

    /**
     * Create a secured directory with a password on the server. You need to be authenticate before create or
     * access a directory
     *
     * @param name            Name of the directory
     * @param description     Description of the new directory
     * @param thumbnailUrl    The thumbnail url of the new directory
     * @param password        user password for the new Directory
     */
    public static Response createSecured(String name, String description, String thumbnailUrl, String password) {
        Socket socket = Connection.getSocket();
        JSONObject channelParams = new JSONObject();
        channelParams.accumulate("auth_token", Connection.getAuth_token())
                .accumulate("user_id", Integer.parseInt(Connection.getUser_id()));

        Channel ch = socket.channel(topic, channelParams);

        ch.join(socket.getOpts().getTimeout());

        JSONObject params = new JSONObject();
        params.accumulate("name", name)
                .accumulate("description", description)
                .accumulate("thumbnail_url", thumbnailUrl)
                .accumulate("is_secured", !password.isEmpty())
                .accumulate("password", password);

        try {
            String response = getDirectory(ch, socket, params, "create").get();
            return new Response(true, response);
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            String message = ExceptionParser.parse(e.getMessage());
            return new Response(false, message);
        }
    }

    private static Future<String> getDirectory(Channel ch, Socket socket, JSONObject params, String event){
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        ch.push(event, params, socket.getOpts().getTimeout()).receive("ok", msg -> {
            Connection.setDirectoryId(msg.getString("directory_id"));
            completableFuture.complete(msg.getString("directory_id"));
            return null;
        }).receive("error", msg -> {
            completableFuture.obtrudeException(new Exception(msg.getString("reason")));
            return null;
        }).receive("timeout", msg -> {
            completableFuture.obtrudeException(new Exception("Channel Timeout"));
            return null;
        });

        return completableFuture;
    }

    /**
     * Create directory were the.
     *
     * @param name            Name of the directory
     * @param description     Description of the new directory
     * @param thumbnail       The thumbnail url of the new directory
     */
    public static Response create(String name, String description, String thumbnail) {
        return createSecured(name, description, thumbnail, "");
    }

    /**
     * open a directory secured with a password on the server.
     * It can create the directory if the directory does not exist.
     *
     * @param name      The name of the directory
     * @param password  the password of the directory
     * @return          Callback if successful
     */
    public static Response openSecured(String name, String password) {
        return createSecured(name, "", "", password);
    }

    /**
     * open a directory on the server. It can create the directory if the directory does not exist.
     *
     * @param name  The name of the directory
     * @return      Callback if successful
     */
    public static Response open(String name) {
        return openSecured(name, "");
    }
}
