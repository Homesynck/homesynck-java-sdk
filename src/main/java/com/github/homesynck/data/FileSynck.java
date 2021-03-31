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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class FileSynck {

    private final FileManager fileManager;
    private Channel channel;
    private final Socket socket;
    private final String password;

    public FileSynck(@NotNull FileManager fileManager, @NotNull String password) {
        this.fileManager = fileManager;
        this.socket = Connection.getSocket();
        this.password = password;
    }

    public String startSyncing() {
        try {
            joinChannel().get();
            return "All si good";
        } catch (InterruptedException | ExecutionException e) {
            return e.getMessage();
        }
    }

    // It will works
    public void setOnUpdate(Consumer<Void> onFileModifiedCallback) {
        this.channel.on("updates", updates -> {
            JSONObject updatesResponse = updates.getResponse();
            applyServerUpdates(updatesResponse);
            onFileModifiedCallback.accept(null);
            return null;
        });
    }


    private Future<String> joinChannel() {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        String topic = "sync:" + Connection.getDirectoryId();
        JSONObject channelParams = new JSONObject();
        channelParams.accumulate("auth_token", Connection.getAuth_token());
        channelParams.accumulate("user_id", Connection.getUser_id());
        channelParams.accumulate("directory_password", password);
        channelParams.accumulate("received_updates", fileManager.getListUpdate());
        this.channel = socket.channel(topic, channelParams);

        this.channel.join(socket.getOpts().getTimeout())
                .receive("ok", success -> {
                    completableFuture.complete("Channel joined");
                    return null;
                })
                .receive("error", error -> {
                    completableFuture.obtrudeValue("Couldn't joined the " + topic + " channel");
                    return null;
                })
                .receive("timeout", timeout -> {
                    completableFuture.obtrudeValue("Timeout on " + topic + " channel");
                    return null;
                });

        return completableFuture;
    }

    public void pushInstructions() {
        List<String> patches = fileManager.getPatch();

        System.out.println("Patches: " + patches);
        for(String patch: patches) {
            try {
                String pushResponse = pushInstruction(patch).get();
                System.out.println("Push response: " + pushResponse);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                System.err.println("There was a problem on pushing: " + e.getMessage());
                return;
            }
        }
    }

    private Future<String> pushInstruction(String instruction) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        int rank = fileManager.getPatchId() +1;

        JSONObject payload = new JSONObject();
        payload.accumulate("rank", rank).accumulate("instructions", instruction);

        this.channel.push("push_update", payload, socket.getOpts().getTimeout())
                .receive("ok", success -> {
                    completableFuture.complete("Instruction accepted");
                    return null;
                })
                .receive("error", error -> {
                    completableFuture.obtrudeValue(error.getString("error"));
                    return null;
                })
                .receive("timeout", timeout -> {
                    completableFuture.obtrudeValue("Timeout on push update event");
                    return null;
                });

        return completableFuture;
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

    static String getFilesPath(String content) {
        String[] lines = content.split(System.lineSeparator());
        if (lines.length > 0) {
            String[] items = lines[1].split("[+]{3}");
            if (items.length == 2)
                return items[1].trim();
        }
        return "";
    }

}
