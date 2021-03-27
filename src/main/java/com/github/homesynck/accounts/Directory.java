package com.github.homesynck.accounts;

import ch.kuon.phoenix.Channel;
import ch.kuon.phoenix.Socket;
import com.github.openjson.JSONObject;
import com.github.homesynck.utils.*;

import java.util.function.Consumer;

public class Directory {
    private static String dirId;

    private static final String topic = "directories:lobby";

    /**
     * Create a secured directory with a password on the server. You need to be authenticate before create or
     * access a directory
     * @param name          Name of the directory
     * @param description   Description of the new directory
     * @param thumbnailUrl  The thumbnail url of the new directory
     * @param password      user password for the new Directory
     * @param successConsumer   Consumer called when the directory is correctly
     * @param errorConsumer     Consumer called when the server return an error
     */
    public static void createSecured(String name, String description, String thumbnailUrl, String password,
                                     Consumer<String> successConsumer, Consumer<String> errorConsumer){
        Socket socket = Connection.getSocket();

        JSONObject channelParams = new JSONObject();
        channelParams.accumulate("auth_token", Connection.getAuth_token())
                .accumulate("user_id", Connection.getUser_id());

        Channel ch = socket.channel(topic, channelParams);

        ch.join(socket.getOpts().getTimeout());

        JSONObject createdJson = new JSONObject();
        createdJson.accumulate("name", name)
                .accumulate("description", description)
                .accumulate("thumbnail_url", thumbnailUrl)
                .accumulate("is_secured", password.isEmpty())
                .accumulate("password", password);

        ch.push("create", createdJson,socket.getOpts().getTimeout()).receive("ok", msg -> {
            dirId = msg.getString("directory_id");
            successConsumer.accept(msg.getString("directory_id"));
            return null;
        }).receive("error",msg -> {
            errorConsumer.accept(msg.getString("reason"));
            return null;
        });
    }

    /**
     * Create directory were the.
     * @param name              Name of the directory
     * @param description       Description of the new directory
     * @param thumbnail         The thumbnail url of the new directory
     * @param successConsumer   Consumer called when the directory is correctly
     * @param errorConsumer     Consumer called when the server return an error
     */
    public static void create(String name, String description, String thumbnail,
                              Consumer<String> successConsumer, Consumer<String> errorConsumer){
        createSecured(name, description, thumbnail, "", successConsumer, errorConsumer);
    }

    public static void openSecured(String name, String password, Consumer<String> successConsumer, Consumer<String> errorConsumer){
        Socket socket = Connection.getSocket();

        Channel ch = socket.channel(topic, new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject sendParams = new JSONObject();
        sendParams.accumulate("name", name)
                .accumulate("is_secured", password.isEmpty())
                .accumulate("password", password);

        ch.push("open", sendParams, socket.getOpts().getTimeout()).receive("ok", msg -> {
            dirId = msg.getString("directory_id");
            successConsumer.accept(msg.getString("directory_id"));
            return null;
        }).receive("error", msg -> {
            errorConsumer.accept(msg.getString("reason"));
            return null;
        });
    }

    public static void open(String name, Consumer<String> successConsumer, Consumer<String> errorConsumer){
        openSecured(name, "", successConsumer, errorConsumer);
    }
}
