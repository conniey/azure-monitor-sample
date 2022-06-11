package org.conniey;

public class QueryException extends RuntimeException {
    private final String display;
    private final String code;
    private final String message;
    private final String query;

    public QueryException(String code, String message, String query) {
        super("Exception occurred querying service.");
        this.code = code;
        this.message = message;
        this.query = query;
        this.display = String.format("Code: %s. Message: %s. Query:%n%s%n", code, message, query);
    }


    @Override
    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return display;
    }
}
