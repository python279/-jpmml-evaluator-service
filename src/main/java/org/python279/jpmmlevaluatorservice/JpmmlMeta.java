package org.python279.jpmmlevaluatorservice;

import org.dmg.pmml.PMML;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.testing.CsvUtil;
import org.jpmml.model.PMMLUtil;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;
import org.dmg.pmml.Header;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;


public class JpmmlMeta {
    private final File model;
    private final String name;
    private final String version;
    private final String baseVersion;
    private final Header header;
    private final List<InputField> inputFields;
    private final List<InputField> groupFields;
    private final List<TargetField> targetFields;
    private final List<OutputField> outputFields;

    JpmmlMeta(
            File model,
            String name,
            String version,
            String baseVersion,
            Header header,
            List<InputField> inputFields,
            List<InputField> groupFields,
            List<TargetField> targetFields,
            List<OutputField> outputFields) {
        this.model = model;
        this.name = name;
        this.version = version;
        this.baseVersion = baseVersion;
        this.header = header;
        this.inputFields = inputFields;
        this.groupFields = groupFields;
        this.targetFields = targetFields;
        this.outputFields = outputFields;
    }

    public File getModel() {
        return this.model;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getBaseVersion() {
        return this.baseVersion;
    }

    public Header getHeader() {
        return this.header;
    }

    public List<InputField> getInputFields() {
        return this.inputFields;
    }

    public List<InputField> getGroupFields() {
        return this.groupFields;
    }

    public List<TargetField> getTargetFields() {
        return this.targetFields;
    }

    public List<OutputField> getOutputField() {
        return this.outputFields;
    }
}
