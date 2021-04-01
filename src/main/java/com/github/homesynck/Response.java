package com.github.homesynck;

/**
 * utility class to get a response with status and message
 */
public class Response {
    private final boolean correct;
    private final String response;

    /**
     * Create a new response
     *
     * @param correct   the status of the response. True if correct
     * @param response  the reply message
     */
    public Response(boolean correct, String response) {
        this.correct = correct;
        this.response = response;
    }

    /**
     * get the status of the update
     *
     * @return  true if the reply is correct
     */
    public boolean isCorrect() {
        return correct;
    }

    /**
     * Return the message
     *
     * @return  the reply message
     */
    public String getResponse() {
        return response;
    }

    /**
     * Return a formatted String containing status and message of the response
     *
     * @return the String
     */
    @Override
    public String toString() {
        return "Response{" +
                "correct=" + correct +
                ", response='" + response + '\'' +
                '}';
    }
}
