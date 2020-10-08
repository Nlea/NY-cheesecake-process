package com.camunda.flowcovexample;

import io.flowcov.camunda.junit.FlowCovProcessEngineRuleBuilder;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.decisionService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Deployment(resources = "classify-cheesecake.dmn")
public class DMNTest {

    @Rule
    @ClassRule
    public static ProcessEngineRule rule = FlowCovProcessEngineRuleBuilder.create().build();

    private static final String CLASSIFY_CAKE = "ClassifyCake";

    @Test
    public void deploy_dmn() {
        // just deployment
    }

    @Test
    public void traditionalCakeTest() {
        var result = evaluateTable(withVariables(
                "chocolate", "",
                "fruit", "",
                "layered", false, "ingredient",
                "flour", "amount", 200, "price", 5));

        assertEquals("traditional", result.getValue());
    }

    private PrimitiveTypeValueImpl.StringValueImpl evaluateTable(Map<String, Object> variables) {
        return decisionService().evaluateDecisionTableByKey(CLASSIFY_CAKE,
                variables).getSingleEntryTyped();
    }
}
