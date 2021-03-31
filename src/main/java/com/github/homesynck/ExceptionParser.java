package com.github.homesynck;

public class ExceptionParser {
    private ExceptionParser(){}

   public static String parse(String exception) {
        String[] exceptionSplit = exception.split("Exception: ");
        if(exceptionSplit.length < 2)
            return "error";
        return exceptionSplit[1];
    }
}
