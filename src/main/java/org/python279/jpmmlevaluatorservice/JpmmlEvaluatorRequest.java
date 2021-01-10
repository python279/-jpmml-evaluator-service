package org.python279.jpmmlevaluatorservice;

import java.sql.Timestamp;


public class JpmmlEvaluatorRequest<T> {
    private String id;
    private Timestamp timestamp;
    private T data;

    public JpmmlEvaluatorRequest(String id, Timestamp timestamp, T data) {
        this.id = id;
        this.timestamp = timestamp;
        this.data = data;
    }

    public String getId() {
        return this.id;
    }

    public void setid(String id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

