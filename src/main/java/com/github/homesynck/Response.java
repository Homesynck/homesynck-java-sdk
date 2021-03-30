package com.github.homesynck;

public class Response {
    private final boolean correct;
    private final String response;

    public Response(boolean correct, String response) {
        this.correct = correct;
        this.response = response;
    }

    public boolean isCorrect() {
        return correct;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "Response{" +
                "correct=" + correct +
                ", response='" + response + '\'' +
                '}';
    }
}
