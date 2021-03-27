package com.github.homesynck.accounts;

public class ServerErrorException extends RuntimeException{
    public ServerErrorException(String s){
        super(s);
    }
}
