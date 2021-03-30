package com.github.homesynck.connect;

import ch.kuon.phoenix.Channel;
import ch.kuon.phoenix.Socket;
import com.github.homesynck.Response;
import com.github.openjson.JSONObject;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

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
        } catch (InterruptedException | ExecutionException e) {
            return new Response(false, e.getMessage());
        }
    }

    private static Future<String> getDirectory(Channel ch, Socket socket, JSONObject params, String event){
        CompletableFuture<String> completableFuture = new CompletableFuture<String>();

        ch.push(event, params, socket.getOpts().getTimeout()).receive("ok", msg -> {
            Connection.setDirectoryId(msg.getString("directory_id"));
            completableFuture.complete(msg.getString("directory_id"));
            return null;
        }).receive("error", msg -> {
            completableFuture.obtrudeValue(msg.getString("reason"));
            return null;
        }).receive("timeout", msg -> {
            completableFuture.obtrudeValue("Channel Timeout");
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

    public static Response openSecured(String name, String password) {
        return createSecured(name, "", "", password);
    }

    public static Response open(String name) {
        return openSecured(name, "");
    }
}
