package com.camunda.training;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.assertj.core.api.Assertions.*;


public class ProcessJUnitTest {
  @Rule
  @ClassRule
  public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

  @Before
  public void setup() {
    init(rule.getProcessEngine());
  }

  @Test
  @Deployment(resources="cheescake-process.bpmn")
  public void testHappyPath() {
    Mocks.register("logger", new LoggerDelegate());

   Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("endOfVisit", "2020-11-11T12:13:14Z");

    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("myNYChesscakeProcess");
    assertThat(processInstance).isWaitingAt("NYTripEndedEvent");
    execute(job());
    assertThat(processInstance).isEnded().hasPassed("CheesecakeTestingEndedEndEvent1");

  }

    @Test
    @Deployment(resources="cheescake-process.bpmn")
    public void invitationMessageCheescakeTestingContinues() {
        Mocks.register("logger", new LoggerDelegate());

        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("myNYChesscakeProcess");
        assertThat(processInstance).isWaitingAt("NYTripEndedEvent");

        runtimeService()
                .createMessageCorrelation("Msg_invite")
                .processInstanceId(processInstance.getId())
                .correlateWithResult();

        assertThat(processInstance).isWaitingAt("CheckInvitationTask");
        complete(task(), withVariables("betterThanCake", false));
        assertThat(processInstance).hasPassed("CheesecakeTestingContinuedEndEvent");
        assertThat(processInstance).isWaitingAt("NYTripEndedEvent");

    }

    @Test
    @Deployment(resources="cheescake-process.bpmn")
    public void invitationMessageCheescakeTestingAborted() {
        Mocks.register("logger", new LoggerDelegate());

        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("myNYChesscakeProcess");
        assertThat(processInstance).isWaitingAt("NYTripEndedEvent");

        runtimeService()
                .createMessageCorrelation("Msg_invite")
                .processInstanceId(processInstance.getId())
                .correlateWithResult();

        assertThat(processInstance).isWaitingAt("CheckInvitationTask");
        complete(task(), withVariables("betterThanCake", true));
        assertThat(processInstance).hasPassed("AbortCheesecakeTestingEndEvent");
        assertThat(processInstance).hasPassed("AbortCheesecakeTestingStartEvent");
        assertThat(processInstance).isEnded().hasPassed("CheesecakeTestingEndedEndEvent");
    }

    @Test
    @Deployment(resources="cheescake-process.bpmn")
    public void CheescakeRecommendationMsgHappyPath() {
        Mocks.register("logger", new LoggerDelegate());

        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("myNYChesscakeProcess");
        assertThat(processInstance).isWaitingAt("NYTripEndedEvent");

        runtimeService()
                .createMessageCorrelation("Msg_CheesecakeRecommendation")
                .processInstanceId(processInstance.getId())
                .correlateWithResult();

        assertThat(processInstance).isWaitingAt("TasteCheescakeTask");
        complete(task(), withVariables("approved", true));
        assertThat(processInstance).isWaitingAt("Gateway_1krafy7");

        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();
        assertThat(jobList).hasSize(2);
        Job job = jobList.get(1);
        execute(job);
        assertThat(processInstance).hasPassed("CheesecakeApprovedEndEvent");

    }

    @Test
    @Deployment(resources="cheescake-process.bpmn")
    public void CheescakeRecommendationCheesecakeRejected() {
        Mocks.register("logger", new LoggerDelegate());

        ProcessInstance processInstance = runtimeService()
                .createProcessInstanceByKey("myNYChesscakeProcess")
                .setVariable("approved", false)
                .startAfterActivity("TasteCheescakeTask")
                .execute();
        assertThat(processInstance).hasPassed("SendAComplaintTask").hasPassed("CheescakeRejectedEndEvent");

    }


    @Test
    @Deployment(resources="cheescake-process.bpmn")
    public void CheescakeRecommendationCheesecakeGoneBad() {
        Mocks.register("logger", new LoggerDelegate());

        ProcessInstance processInstance = runtimeService()
                .createProcessInstanceByKey("myNYChesscakeProcess")
                .setVariable("approved", true)
                .startAfterActivity("TasteCheescakeTask")
                .execute();
        assertThat(processInstance).isWaitingAt("Gateway_1krafy7");

        runtimeService().createMessageCorrelation("Msg_foodPoisend").correlateWithResult();

        assertThat(processInstance).hasPassed("BadCheesecakeEvent");
        assertThat(processInstance).hasPassed("DeleteFromListTask");
        assertThat(processInstance).hasPassed("SendAComplaintTask");
        assertThat(processInstance).hasPassed("CheescakeRejectedEndEvent");


    }


}
