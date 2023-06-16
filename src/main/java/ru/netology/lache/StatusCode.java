package ru.netology.lache;

public enum StatusCode {
    S400 ("400 Bad Request"),
    S404 ("404 Not Found"),
    S200 ("200 OK");

    private String msg;

    StatusCode(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }
}
