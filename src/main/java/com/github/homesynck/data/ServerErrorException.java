package com.github.homesynck.data;

public class ServerErrorException extends RuntimeException{
    public ServerErrorException(String s){
        super(s);
    }
}
