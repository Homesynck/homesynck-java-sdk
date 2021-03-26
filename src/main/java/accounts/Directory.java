package accounts;

import ch.kuon.phoenix.Channel;
import ch.kuon.phoenix.Socket;
import com.github.openjson.JSONObject;
import utils.VPSConnection;

import java.util.function.Function;

public class Directory {
    private static String dirId;

    /**
     * Create a secured directory with a password on the server. You need to be authentificate before create or
     * access a directory
     * @param name          Name of the directory
     * @param description   Description of the new directory
     * @param thumbnailUrl  The thumbnail url of the new directory
     * @param password      user password for the new Directory
     * @param function      the function executed if the directory has been created
     */
    public static void createSecured(String name, String description, String thumbnailUrl, String password, Function<String, Void> function){
        Socket socket = VPSConnection.getSocket();

        JSONObject channelParams = new JSONObject();
        channelParams.accumulate("auth_token", VPSConnection.getAuth_token())
                .accumulate("user_id", VPSConnection.getUser_id());

        Channel ch = socket.channel("directories:lobby", channelParams);

        ch.join(socket.getOpts().getTimeout());

        JSONObject createdJson = new JSONObject();
        createdJson.accumulate("name", name)
                .accumulate("description", description)
                .accumulate("thumbnail_url", thumbnailUrl)
                .accumulate("is_secured", password.isEmpty())
                .accumulate("password", password);

        ch.push("create", createdJson,socket.getOpts().getTimeout()).receive("ok", msg -> {
            dirId = msg.getString("directory_id");
            function.apply(msg.getString("directory_id"));
            return null;
        }).receive("error",msg -> {
            throw new ServerErrorException(msg.getString("error"));
        });
    }

    /**
     * Create directory were the.
     * @param name          Name of the directory
     * @param description   Description of the new directory
     * @param thumbnail     The thumbnail url of the new directory
     * @param function      the function executed if the directory has been created
     */
    public static void create(String name, String description, String thumbnail, Function<String, Void> function){
        createSecured(name, description, thumbnail, "", function);
    }

    public static void open(String name){

    }
}
