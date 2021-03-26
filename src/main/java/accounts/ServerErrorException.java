package accounts;

public class ServerErrorException extends RuntimeException{
    public ServerErrorException(String s){
        super(s);
    }
}
