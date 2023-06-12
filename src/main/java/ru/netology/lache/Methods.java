package ru.netology.lache;

public enum Methods {
    GET,
    POST,
    PUT,
    DELETE;

    public static boolean check(String method) {
        switch (Methods.valueOf(method)) {
            case GET:
                //return true;
            case POST:
                //return true;
            case PUT:
                //return true;
            case DELETE:
                return true;
            default:
                return false;
        }
    }
}
