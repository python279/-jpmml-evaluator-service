package org.python279.jpmmlevaluatorservice;

import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;

import java.io.File;
import java.util.List;


public class JpmmlEvaluatorResponse<T> {
    private JpmmlEvaluatorResponseCode code;
    private String msg;
    private T data;

    JpmmlEvaluatorResponse(JpmmlEvaluatorResponseCode code, T data) {
        this.code = code;
        this.msg = code.toString();
        this.data = data;
    }

    JpmmlEvaluatorResponse(JpmmlEvaluatorResponseCode code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
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
