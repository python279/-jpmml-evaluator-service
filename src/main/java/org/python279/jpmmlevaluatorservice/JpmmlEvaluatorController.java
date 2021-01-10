package org.python279.jpmmlevaluatorservice;

import java.io.Console;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.jpmml.evaluator.CacheUtil;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldNameSet;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.FunctionNameStack;
import org.jpmml.evaluator.HasGroupFields;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.OutputFilters;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.ValueFactoryFactory;
import org.jpmml.evaluator.testing.BatchUtil;
import org.jpmml.evaluator.testing.CsvUtil;
import org.jpmml.evaluator.visitors.AttributeFinalizerBattery;
import org.jpmml.evaluator.visitors.AttributeInternerBattery;
import org.jpmml.evaluator.visitors.AttributeOptimizerBattery;
import org.jpmml.evaluator.visitors.ElementFinalizerBattery;
import org.jpmml.evaluator.visitors.ElementInternerBattery;
import org.jpmml.evaluator.visitors.ElementOptimizerBattery;
import org.jpmml.model.visitors.LocatorNullifier;
import org.jpmml.model.visitors.MemoryMeasurer;
import org.jpmml.model.visitors.VisitorBattery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import com.google.common.collect.Sets;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
public class JpmmlEvaluatorController {
    private File model;
    private PMML pmml;
    private Evaluator evaluator;
    private List<InputField> inputFields;
    private List<InputField> groupFields;
    private List<TargetField> targetFields;
    private List<OutputField> outputFields;
    private List<String> missingValues = Arrays.asList("N/A", "NA");
    private boolean sparse = false;
    private String modelEvaluatorFactoryClazz = ModelEvaluatorFactory.class.getName();
    private String valueFactoryFactoryClazz = ValueFactoryFactory.class.getName();
    private boolean filterOutput = false;
    private boolean optimize = false;
    private boolean safe = false;
    private boolean intern = false;
    private boolean measure = false;
    private static final Logger log = LoggerFactory.getLogger(JpmmlEvaluatorController.class);


    public JpmmlEvaluatorController() throws Exception {
        String pmmlPath = System.getenv("PMML_PATH");
        this.model = new File(pmmlPath != null ? pmmlPath : "XGBoostAudit.pmml");
        this.pmml = JpmmlUtils.readPMML(this.model);

        VisitorBattery visitorBattery = new VisitorBattery();

        if (this.intern) {
            visitorBattery.add(LocatorNullifier.class);
        } // End if

        // Optimize first, intern second.
        // The goal is to intern optimized elements (keeps one copy), not optimize interned elements (expands one copy to multiple copies).
        if (this.optimize) {
            visitorBattery.addAll(new AttributeOptimizerBattery());
            visitorBattery.addAll(new ElementOptimizerBattery());
        } // End if

        if (this.intern) {
            visitorBattery.addAll(new AttributeInternerBattery());
            visitorBattery.addAll(new ElementInternerBattery());
        } // End if

        if (this.optimize || this.intern) {
            visitorBattery.addAll(new AttributeFinalizerBattery());
            visitorBattery.addAll(new ElementFinalizerBattery());
        }

        visitorBattery.applyTo(pmml);

        if (this.measure) {
            MemoryMeasurer memoryMeasurer = new MemoryMeasurer();
            memoryMeasurer.applyTo(pmml);

            NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
            numberFormat.setGroupingUsed(true);

            long size = memoryMeasurer.getSize();
            log.info("Bytesize of the object graph: " + numberFormat.format(size));

            Set<Object> objects = memoryMeasurer.getObjects();

            long objectCount = objects.size();

            log.info("Number of distinct Java objects in the object graph: " + numberFormat.format(objectCount));

            long pmmlObjectCount = objects.stream()
                    .filter(PMMLObject.class::isInstance)
                    .count();

            log.info("\t" + "PMML class model objects: " + numberFormat.format(pmmlObjectCount));
            log.info("\t" + "Other objects: " + numberFormat.format(objectCount - pmmlObjectCount));
        }

        ModelEvaluatorBuilder evaluatorBuilder = new ModelEvaluatorBuilder(pmml)
                .setModelEvaluatorFactory((ModelEvaluatorFactory) JpmmlUtils.newInstance(this.modelEvaluatorFactoryClazz))
                .setValueFactoryFactory((ValueFactoryFactory) JpmmlUtils.newInstance(this.valueFactoryFactoryClazz))
                .setOutputFilter(this.filterOutput ? OutputFilters.KEEP_FINAL_RESULTS : OutputFilters.KEEP_ALL);

        if (this.safe) {
            evaluatorBuilder = evaluatorBuilder
                    .setDerivedFieldGuard(new FieldNameSet(8))
                    .setFunctionGuard(new FunctionNameStack(4));
        }

        this.evaluator = evaluatorBuilder.build();

        // Perform self-testing
        //evaluator.verify();

        this.inputFields = evaluator.getInputFields();
        this.groupFields = Collections.emptyList();
        this.targetFields = evaluator.getTargetFields();
        this.outputFields = evaluator.getOutputFields();

        if (evaluator instanceof HasGroupFields) {
            HasGroupFields hasGroupfields = (HasGroupFields) evaluator;

            groupFields = hasGroupfields.getGroupFields();
        } // End if
    }

    @GetMapping("/meta")
    public JpmmlEvaluatorResponse<JpmmlMeta> meta() {
        try {
            return new JpmmlEvaluatorResponse<JpmmlMeta>(
                    null,
                    JpmmlEvaluatorResponseCode.SUCCESS,
                    new JpmmlMeta(
                            this.model,
                            this.pmml.getVersion(),
                            this.pmml.getBaseVersion(),
                            this.pmml.getHeader(),
                            this.inputFields,
                            this.groupFields,
                            this.targetFields,
                            this.outputFields
                    )
            );
        } catch(Exception ex) {
            return new JpmmlEvaluatorResponse<JpmmlMeta>(
                    null,
                    JpmmlEvaluatorResponseCode.ERROR,
                    ex.getMessage(),
                    null
            );
        }
    }

    private List<Map<FieldName, ?>>JpmmlEvaluatorExecute(List<Map<FieldName, ?>> inputRecords) {
        if(!inputRecords.isEmpty()){
            Map<FieldName, ?> inputRecord = inputRecords.get(0);

            Sets.SetView<FieldName> missingInputFields = Sets.difference(new LinkedHashSet<>(Lists.transform(this.inputFields, InputField::getName)), inputRecord.keySet());
            if(!missingInputFields.isEmpty() && !this.sparse){
                throw new IllegalArgumentException("Missing input field(s): " + missingInputFields);
            }

            Sets.SetView<FieldName> missingGroupFields = Sets.difference(new LinkedHashSet<>(Lists.transform(this.groupFields, InputField::getName)), inputRecord.keySet());
            if(!missingGroupFields.isEmpty()){
                throw new IllegalArgumentException("Missing group field(s): " + missingGroupFields);
            }
        } // End if

        if(this.evaluator instanceof HasGroupFields){
            HasGroupFields hasGroupFields = (HasGroupFields)evaluator;
            inputRecords = (List<Map<FieldName, ?>>)EvaluatorUtil.groupRows(hasGroupFields, inputRecords);
        }

        List<Map<FieldName, ?>> outputRecords = new ArrayList<>(inputRecords.size());
        for (Map<FieldName, ?> inputRecord : inputRecords) {
            Map<FieldName, FieldValue> arguments = JpmmlUtils.getFieldArgumentMap(inputRecord, this.inputFields);
            outputRecords.add(this.evaluator.evaluate(arguments));
        }
        return outputRecords;
    }

    @PostMapping("/single")
    public JpmmlEvaluatorResponse<List<Map<String, Double>>> single(@RequestBody JpmmlEvaluatorRequest<Map<String, Object>> request) {
        try {
            List<Map<FieldName, ?>> inputRecords = new ArrayList<Map<FieldName, ?>>(1);
            inputRecords.add(JpmmlUtils.convertInputRecord(request.getData()));

            return new JpmmlEvaluatorResponse(
                    request.getId(),
                    JpmmlEvaluatorResponseCode.SUCCESS,
                    JpmmlUtils.getOutputResultMap(this.outputFields, this.JpmmlEvaluatorExecute(inputRecords))
            );
        } catch(Exception ex) {
            return new JpmmlEvaluatorResponse(
                    request.getId(),
                    JpmmlEvaluatorResponseCode.ERROR,
                    ex.getMessage(),
                    null
            );
        }
    }

    @PostMapping("/multi")
    public JpmmlEvaluatorResponse<List<Map<String, Double>>> multi(@RequestBody JpmmlEvaluatorRequest<List<Map<String, Object>>> request) {
        try {
            List<Map<FieldName, ?>> inputRecords = new ArrayList<Map<FieldName, ?>>(request.getData().size());
            for (Map<String, Object> req : request.getData()){
                inputRecords.add(JpmmlUtils.convertInputRecord(req));
            }

            return new JpmmlEvaluatorResponse(
                    request.getId(),
                    JpmmlEvaluatorResponseCode.SUCCESS,
                    JpmmlUtils.getOutputResultMap(this.outputFields, this.JpmmlEvaluatorExecute(inputRecords))
            );
        } catch(Exception ex) {
            return new JpmmlEvaluatorResponse(
                    request.getId(),
                    JpmmlEvaluatorResponseCode.ERROR,
                    ex.getMessage(),
                    null
            );
        }
    }
}
