# Homesynck Java SDK

![thumbnail](./thumbnail.png)

## How to install?

### With maven

Add the following dependency to your **pom.xml** to import the api :

```xml
<dependency>
    <groupId>com.github.homesynck</groupId>
    <artifactId>homesynck-java-sdk</artifactId>
    <version>0.2.2</version>
</dependency>
```

### With Gradle

Add the following dependency to your **build.gradle** to import the api :
```gradle
implementation group: 'com.github.homesynck', name: 'homesynck-java-sdk', version: '0.2.2'
```
### Other

You can also check [this website](https://mvnrepository.com/artifact/com.github.homesynck/homesynck-java-sdk) for other configurations and older versions.

## Start

The server address is set to default on our servers. If you self-host the server, you can change the server address by using the following code :
```java
com.github.homesynck.connect.Connection.setHost("ws://example.com/socket");   // without https server
com.github.homesynck.connect.Connection.setHost("wss://example.com/socket");  // with https server
```

### Authentication

To register and connect to the repository of your application on the server, first you will need to send your phone number to the server in order to avoid fake account creation. One phone number can only create one account every thirty days. Then, a token will be sent to you on their mobile. You can now register to the server by using the register method and connect to the directory by using the create function (create also connect if the directory exist).

Here is a simple example:

```java
import com.github.homesynck.Response;
import com.github.homesynck.connect.Directory;
import com.github.homesynck.connect.Session;
import com.github.homesynck.data.FileManager;
import com.github.homesynck.data.FileSynck;
import java.io.IOException;

public class MyApplication {
    private static FileManager fileManager;
    private static FileSynck fileSynck;

    public static void main(String[] args) {
        Session session = Session.getSession();

        Response response = session.phoneValidation("+33000000000");
        if (!response.isCorrect()) {
            System.out.println("Error on send phone validation");
            return;
        }

        response = session.register("John doe", "I<3AgeOfEmpires", "123456");
        if (!response.isCorrect()) {
            System.out.println("Error on account registration");
            return;
        }

        Response directoryResponse = Directory.create("MyDirectory", "The directory for the readme application", "password");
        System.out.println(directoryResponse);
        if (!directoryResponse.isCorrect()) {
            System.out.println("Error on directory registration");
            return;
        }
        
        //...
    }
}
```

To login, the process is very similar but without the phone validation

### Synchronizing files

After connecting to the account, you need to join the synck channel of your directory in order to synchronize your files by creating a new FileSynck and use ` fileSynck.startSyncing();`.

To activate the live update, you need to register a Consumer with `fileSynck.setOnUpdate(Promise<Void> promise);`. You are now ready to edit or create files with `fileManager.editFile(String path, String content);`, get every files in a HashMap<String, String> with `fileManager.getFiles();`, specify a path with `fileManager.getFile(String Path);`, or delete a file with `fileManager.deleteFile(String path);`. If you want to save these datas and push all of these changes on the server, you can use `fileSynck.pushInstructions();`.

Here is a short example where we show "update(s) receive" when we receive one or more updates, then edit and delete two files and push these updates to the server.

```java
public class MyApplication {
    private static FileManager fileManager;
    private static FileSynck fileSynck;

    public static void main(String[] args) {
        //...

        // You need to choose where the API store all of the datas.
        fileManager = new FileManager("./myStorageDirectory/");

        Response response = fileSynck.startSyncing();
        if (!response.isCorrect){
            System.out.println("Error on join: " + response);
            return;
        }

        fileSynck.setOnUpdate(msg -> {
            System.out.println("update(s) receive");
        });

        try {
            fileManager.editFile("/myDocument/PositiveReview.txt", "this documentation is very amazing!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileManager.deleteFile("/myDocument/NegativeReview.txt");

        fileSynck.pushInstructions();
    }
}
```

## License

Licensed under MIT license ([LICENSE-MIT](LICENSE) or http://opensource.org/licenses/MIT).
