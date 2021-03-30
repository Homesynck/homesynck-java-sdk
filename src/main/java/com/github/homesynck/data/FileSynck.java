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
    private boolean hasError;
    private boolean hasSynced;
    private boolean hasNotFinish;
    private boolean hasNotPushed;

    public FileSynck(@NotNull FileManager fileManager, @NotNull String password) {
        this.fileManager = fileManager;
        String topic = "sync:" + Connection.getDirectoryId();
        this.socket = Connection.getSocket();
        hasError = false;
        hasSynced = false;
        hasNotFinish = true;
        hasNotPushed = true;

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
            System.out.println("After apply");
            if(!hasSynced){
                System.out.println("sync update");
                while(hasNotFinish) {
                    joinSynckOnUpdate((success) -> {}, System.err::println);
                }
                hasNotPushed = true;

                System.out.println("after sync declaration");
                hasSynced = true;
            }
            System.out.println("on end ");

            onFileModifiedCallback.accept(null);
            return null;
        });
    }

    public void joinSynckOnUpdate(Consumer<String> successConsumer, Consumer<String> errorConsumer) {

        List<String> patches = fileManager.getPatch();
        System.out.println("Patches: " + patches);

        if (!patches.isEmpty()) {
            System.out.println("There's some patches");
            for (String patch : patches) {
                System.out.println("Before push update");
                hasError = false;
                while(hasNotPushed)
                    pushUpdate(fileManager.getPatchId(), patch, (thread) -> {}, (error) -> hasError = true);

                if(hasError) {
                    errorConsumer.accept("There was an error while we trying to push!");
                    hasNotFinish = false;
                    return;
                }
            }
        }
        hasNotFinish = false;
        successConsumer.accept("All is good");

    }

    public void joinSynckChannel() {

        ch.join(socket.getOpts().getTimeout());
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

        JSONObject payload = new JSONObject();
        payload.accumulate("rank", rank).accumulate("instructions", patch);
        System.out.println(payload);

        System.out.println("Push payload: " + payload);
        ch.push("push_update", payload, socket.getOpts().getTimeout())
                .receive("ok", success -> {
                    try {
                        System.out.println("Receiving from server");
                        fileManager.accept(rank, patch);
                        System.out.println("All is good for accept method");
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
        hasNotPushed = false;
    }

    public void pushUpdate(@NotNull Consumer<String> successConsumer, @NotNull Consumer<String> errorConsumer) {
        System.out.println("call Push update");
        List<String> patches = fileManager.getPatch();
        int patchId = fileManager.getPatchId();
        if(patchId == 0) patchId++;

        if(!patches.isEmpty()) {
            System.out.println("patches");
            for(String patch: patches) {
                System.out.println("one");
                hasError = false;
                System.out.println("two");
                while(hasNotPushed) {
                    pushUpdate(++patchId, patch, (thread) -> {
                    }, (error) -> hasError = true);
                }
                hasNotPushed = true;
                System.out.println("Update pushed");
                if(hasError) {
                    errorConsumer.accept("There was an error while we trying to push!");
                    return;
                }
            }
            System.out.println("end of pushes");
        }
        successConsumer.accept("All is good");
    }

    private JSONObject initPushChannel(int rank, String instruction) {

        JSONObject payload = new JSONObject();
        payload.accumulate("rank", rank).accumulate("instructions", instruction);
        System.out.println(payload);
        return payload;
    }


}
