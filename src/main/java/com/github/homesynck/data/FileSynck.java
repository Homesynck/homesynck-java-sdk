package com.github.homesynck.data;

import ch.kuon.phoenix.Channel;
import ch.kuon.phoenix.Socket;
import com.github.difflib.patch.PatchFailedException;
import com.github.homesynck.connect.Connection;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
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
        channelParams.accumulate("auth_token", Connection.getAuth_token());
        channelParams.accumulate("user_id", Connection.getUser_id());
        channelParams.accumulate("directory_password", password);
        channelParams.accumulate("received_updates", fileManager.getListUpdate());
        // TODO check if there's a problem with channel params


        this.ch = socket.channel(topic, channelParams);
    }

    static String getFilesPath(String content) {
        String[] lines = content.split(System.lineSeparator());
        String[] items =  lines[1].split("[+]{3}");
        if(items.length == 2)
            return items[1].trim();
        return "";
    }

    public void setOnUpdate(Consumer<Void> onFileModifiedCallback) {
        ch.on("updates", updates -> {
            JSONObject updatesResponse = updates.getResponse();
            applyServerUpdates(updatesResponse);
            onFileModifiedCallback.accept(null);
            return null;
        });
    }

    public void joinSynckChannel(@NotNull Consumer<String> successConsumer, @NotNull Consumer<String> errorConsumer) {

        ch.join(socket.getOpts().getTimeout());

        int beginnerPatchId = fileManager.getPatchId();

        List<String> patches = fileManager.getPatch();

        if (!patches.isEmpty()) {
            for (String patch : patches) {
                System.out.println("Before push update");
                pushUpdate(beginnerPatchId++, patch, successConsumer, errorConsumer);
            }
        }
    }

    private void applyServerUpdates(JSONObject updates) {
        JSONArray updatesObjects = updates.getJSONArray("updates");
        int nbUpdates = updatesObjects.length();
        System.out.println(updatesObjects.toString());
        System.out.println(nbUpdates);
        for (int i = 0; i < nbUpdates; ++i) {
            JSONObject updateObject =  updatesObjects.getJSONObject((i));
            String json = updateObject.getString("instructions");
            String fileName = getFilesPath(json);
            int patchId = updateObject.getInt("rank");
            try {
                fileManager.applyPatch(patchId, fileName, json);
            } catch (IOException | PatchFailedException e) {
                e.printStackTrace();
            }
        }
    }

    private void pushUpdate(int rank, String patch, Consumer<String> successConsumer
            , Consumer<String> errorConsumer) {

        JSONObject payload = initPushChannel(rank, patch);
        System.out.println("call Push update");
        ch.push("push_update", payload, socket.getOpts().getTimeout())
                .receive("ok", success -> {
                    try {
                        fileManager.accept(rank, patch);
                        successConsumer.accept("Update sent successfully ");
                    } catch (IOException e) {
                        errorConsumer.accept("There was an error while we trying to sync on your device");
                        e.printStackTrace();
                    }catch (PatchFailedException e){
                        errorConsumer.accept("Patch can't be apply");
                    }
                    return null;
                }).receive("error", error -> {
                    errorConsumer.accept(error.getString("error"));
                    return null;
                }
        );
    }

    public void pushUpdate(@NotNull Consumer<String> successConsumer, @NotNull Consumer<String> errorConsumer) {

        List<String> patches = fileManager.getPatch();
        int patchId = fileManager.getPatchId();
        if(patchId == 0) patchId++;

        if(!patches.isEmpty()) {
            for(String patch: patches) {
                pushUpdate(++patchId, patch,
                        successConsumer, errorConsumer);
            }
        }
    }

    private JSONObject initPushChannel(int rank, String instruction) {
        ch.join(socket.getOpts().getTimeout());

        JSONObject payload = new JSONObject();
        payload.accumulate("rank", rank).accumulate("instructions", instruction);
        System.out.println(payload);
        return payload;
    }


}
