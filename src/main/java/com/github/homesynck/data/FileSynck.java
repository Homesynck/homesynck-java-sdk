package com.github.homesynck.data;

import ch.kuon.phoenix.Channel;
import ch.kuon.phoenix.Socket;
import com.github.difflib.patch.PatchFailedException;
import com.github.homesynck.connect.Connection;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class FileSynck {
    private final FileManager fileManager;
    private final Channel ch;
    private final Socket socket;

    public FileSynck(@NotNull FileManager fileManager, @NotNull String password) {
        this.fileManager = fileManager;
        String topic = "sync:" + Connection.getDirectoryId();
        this.socket = Connection.getSocket();

        JSONObject channelParams = new JSONObject();
        channelParams.accumulate("auth_token", Connection.getAuth_token())
                .accumulate("user_id", Connection.getUser_id())
                .accumulate("directory_password", password)
                .accumulate("received-updates", fileManager.getListUpdate());

        this.ch = socket.channel(topic, channelParams);
    }

    static String getFilesPath(String content) {
        String[] lines = content.split(System.lineSeparator());
        String newFilePath = lines[1].split("[+]{3}")[1];
        return newFilePath;
    }

    public void joinSynckChannel(@NotNull Consumer<String> successConsumer, @NotNull Consumer<String> errorConsumer) {

        ch.join(socket.getOpts().getTimeout());

        ch.push("join", new JSONObject(), socket.getOpts().getTimeout())
                .receive("updates", updates -> {
                    int beginnerPatchId = fileManager.getPatchId();
                    applyServerUpdates(updates);
                    List<String> patches = fileManager.getPatch();
                    if (!patches.isEmpty())
                        for (String patch : patches)
                            pushUpdate(String.valueOf(beginnerPatchId++), patch,
                                    successConsumer, errorConsumer);
                    return null;
                }).receive("error", error -> {
                    errorConsumer.accept(error.getString("error"));
                    return null;
                }
        );
    }

    private void applyServerUpdates(JSONObject updates) {
        JSONArray updatesObjects = updates.getJSONArray("updates");
        int nbUpdates = updatesObjects.length();
        for (int i = 0; i < nbUpdates; ++i) {
            String json = updatesObjects.optString(i);
            String fileName = getFilesPath(json);
            String patchId = updates.getString("rank");
            try {
                fileManager.applyPatch(patchId, fileName, json);
            } catch (IOException | PatchFailedException e) {
                e.printStackTrace();
            }
        }
    }

    private void pushUpdate(String rank, String instruction, Consumer<String> successConsumer
            , Consumer<String> errorConsumer) {

        JSONObject payload = initPushChannel(rank, instruction);

        ch.push("push_update", payload, socket.getOpts().getTimeout())
                .receive("ok", success -> {
                    try {
                        fileManager.accept(rank, instruction);
                        successConsumer.accept("Update sent successfully ");
                    } catch (IOException | PatchFailedException e) {
                        errorConsumer.accept("There was an error while we trying to sync on your device");
                    }
                    return null;
                }).receive("error", error -> {
                    errorConsumer.accept(error.getString("error"));
                    return null;
                }
        );
    }

    public void pushUpdate(@NotNull String patch, @NotNull Consumer<String> successConsumer, @NotNull Consumer<String> errorConsumer) {

        String patchId = String.valueOf(fileManager.getPatchId());
        pushUpdate(patchId, patch, successConsumer, errorConsumer);
    }

    private JSONObject initPushChannel(String rank, String instruction) {
        ch.join(socket.getOpts().getTimeout());

        JSONObject payload = new JSONObject();
        payload.accumulate("rank", rank).accumulate("instructions", instruction);
        return payload;
    }
}
