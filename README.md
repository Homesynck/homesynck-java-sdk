# homesynck-java-sdk

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
com.github.homesynck.utils.Connection.setHost("ws://example.com/socket");   // without https server
com.github.homesynck.utils.Connection.setHost("wss://example.com/socket");  // with https server
```

### Authentication

To register, first you will need to send your phone number to the server in order to avoid fake account creation. One phone number can only create one account every thirty days. Then, a token will be sent to the user on their mobile. You can now register to the server by using the register method.
Here is an example:
```java
import com.github.homesynck.accounts.Session;
import java.util.Scanner;

public class Register {
    public static void main(String[] args) {
        Session session = Session.getSession();

        session.phoneValidation("+33000000000", response -> {
            // if you pass here, it means that you are connected
            register(session);
        }, error -> {
            System.err.println(error);
        }); // this is the block which will be called when there is an error while trying to register
    }

    public static void register(Session session){

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the token sent by sms : ");
        String token = sc.next();

        session.register("John doe", "I<3AgeOfEmpires", token , response -> {
            System.out.println("You are now connected !");

            // you can continue your application from here
        }, error -> {
            System.err.println(error);
        });
    }
}
```

To login, the process is very similar but without the phone validation
