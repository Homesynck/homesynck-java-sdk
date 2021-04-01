package com.github.homesynck;

/**
 * Utility class to parse an error message to display only the error message
 */
public class ExceptionParser {
    private ExceptionParser(){}

    /**
     * Simple method to parse an error message for a clearer message
     *
     * @param exception The message of the exception
     * @return          The message parse without the class path at the beginning
     */
   public static String parse(String exception) {
        String[] exceptionSplit = exception.split("Exception: ");
        if(exceptionSplit.length < 2)
            return "error";
        return exceptionSplit[1];
    }
}
