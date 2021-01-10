package org.python279.jpmmlevaluatorservice;

import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;

import java.io.File;
import java.util.List;
import java.sql.Timestamp;


public class JpmmlEvaluatorResponse<T> {
    private String id;
    private Timestamp timestamp;
    private JpmmlEvaluatorResponseCode code;
    private String msg;
    private T data;

    public JpmmlEvaluatorResponse(String id, JpmmlEvaluatorResponseCode code, T data) {
        this(id, code, code.toString(), data);
    }

    public JpmmlEvaluatorResponse(String id, JpmmlEvaluatorResponseCode code, String msg, T data) {
        this.id = id;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public String getId() {
        return this.id;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public int getCode() {
        return this.code.ordinal();
    }

    public String getMsg() {
        return this.msg;
    }

    public T getData() {
        return this.data;
    }
}
