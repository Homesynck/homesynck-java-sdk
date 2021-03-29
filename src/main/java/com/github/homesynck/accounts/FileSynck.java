package com.github.homesynck.accounts;

import com.github.homesynck.file.FileManager;
import com.github.homesynck.utils.Connection;
import com.github.openjson.JSONArray;
import org.jetbrains.annotations.NotNull;
import ch.kuon.phoenix.Channel;
import ch.kuon.phoenix.Socket;
import com.github.openjson.JSONObject;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class FileSynck {
    private final FileManager fileManager;
    private String topic;

    public FileSynck(@NotNull FileManager fileManager, @NotNull String directoryId) {
        this.fileManager = fileManager;
        this.topic = "sync:" + directoryId;
    }

    // TODO name to change
    public void join(@NotNull String password, Consumer<String[]> successConsumer, Consumer<String> errorConsumer) {
        Socket socket = Connection.getSocket();

        JSONObject channelParams = new JSONObject();
        channelParams.accumulate("auth_token", Connection.getAuth_token())
                .accumulate("user_id", Connection.getUser_id())
                .accumulate("directory_password", password)
                .accumulate("received-updates", fileManager.getListUpdate()); // TODO list of updates stored on file manager [1, 2, 3, 4]


        Channel ch = socket.channel(topic, channelParams);

        ch.join(socket.getOpts().getTimeout());

        ch.push("join", new JSONObject(), socket.getOpts().getTimeout())
            .receive("updates", updates -> {
                JSONArray updatesObjects = updates.getJSONArray("updates");
                int nbUpdates = updatesObjects.length();
                for(int i = 0; i< nbUpdates; ++i) {
                    String json = updatesObjects.optString(i);
                    String[] files = getFilesPath(json);
                    
                }
                // TODO push updates to saveDirectory
                // TODO scan storageDirectory with saveDirectory for differences
                return null;
            }).receive("received_updates", receivedUpdates -> {
                // TODO push received updates on save directory
                // TODO send to the server the files id updated
                return null;
            }).receive("error", error -> {
                errorConsumer.accept(error.getString("error"));
                return null;
            });
    }

    private void pushUpdate(String rank, List<String> instructions, Consumer<String> successConsumer, Consumer<String> errorConsumer) {
        Socket socket = Connection.getSocket();

        Channel ch = socket.channel(topic, new JSONObject());

        ch.join(socket.getOpts().getTimeout());

        JSONObject payload = new JSONObject();
        payload.accumulate("rank", rank).accumulate("instructions", instructions);
        ch.push("push_update", payload, socket.getOpts().getTimeout())
            .receive("ok", sucess -> {
                successConsumer.accept("");
                return null;
            }).receive("error", error -> {
                errorConsumer.accept("");
                return null;
            });
    }

    private String[] getFilesPath(String homesynckFormat) {
        String[] lines = homesynckFormat.split(System.lineSeparator());
        String originalFilePath = lines[0].split("---")[1];
        String newFilePath = lines[1].split("+++")[1];
        return new String[]{originalFilePath, newFilePath};
    }
}
