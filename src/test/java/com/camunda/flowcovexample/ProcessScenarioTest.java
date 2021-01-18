package com.camunda.flowcovexample;


import io.flowcov.camunda.junit.FlowCovProcessEngineRuleBuilder;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.camunda.bpm.scenario.ProcessScenario;
import org.camunda.bpm.scenario.Scenario;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskService;
import static org.mockito.Mockito.when;

@Deployment(resources = "cheesecake-process.bpmn")
public class ProcessScenarioTest {

    public static final String PROCESS_KEY = "myNYCheesecakeProcess";
    @Rule
    @ClassRule
    public static ProcessEngineRule rule = FlowCovProcessEngineRuleBuilder.create().build();

    @Mock
    private ProcessScenario tastecheesecake;

    @Before
    public void setupDefaultScenario() {
        Mocks.register("logger", new LoggerDelegate());
        MockitoAnnotations.initMocks(this);

        when(tastecheesecake.waitsAtTimerIntermediateEvent("NYTripEndedEvent")).thenReturn(event -> {
            runtimeService().correlateMessage("Msg_CheesecakeRecommendation");
            event.defer("PT100M", () -> {
            });
        });

        when(tastecheesecake.waitsAtUserTask("TasteCheesecakeTask")).thenReturn((task) -> {
            task.complete(withVariables("approved", true));
        });

        when(tastecheesecake.waitsAtUserTask("CheckInvitationTask")).thenReturn((task) -> {
            task.complete(withVariables("betterThanCake", false));
        });

        when(tastecheesecake.waitsAtEventBasedGateway("EventBasedGateway01")).thenReturn(gateway -> {
            // Do nothing means process moves forward because of the timers
        });
    }

    @Test
    public void testHappyPath() {
        Scenario.run(tastecheesecake).startByKey(PROCESS_KEY).execute();
    }

    @Test
    public void testCheesecakeNotApproved() {
        when(tastecheesecake.waitsAtUserTask("TasteCheesecakeTask")).thenReturn((task) -> {
            task.complete(withVariables("approved", false));
        });
        Scenario.run(tastecheesecake).startByKey(PROCESS_KEY).execute();
    }

    @Test
    public void testCheesecakeFoodPoisendMessage() {
        when(tastecheesecake.waitsAtEventBasedGateway("EventBasedGateway01")).thenReturn(gateway -> gateway.getEventSubscription("FoodPoisedEvent").receive());
        Scenario.run(tastecheesecake).startByKey(PROCESS_KEY).execute();
    }

    @Test
    public void testMoreThanOneCheesecake() {
        when(tastecheesecake.waitsAtUserTask("TasteCheesecakeTask")).thenReturn((task) -> {
            taskService().handleEscalation(task.getId(), "es_MoreCheesecake", withVariables("numberOfCakes", 2));
            task.complete(withVariables("approved", true));
        }, (task) -> {
            task.complete(withVariables("approved", true));
        });

        Scenario.run(tastecheesecake)
                .startByKey(PROCESS_KEY)
                .execute();
    }

    @Test
    public void testCheesecakePlaceNotFound() {
        when(tastecheesecake.waitsAtUserTask("TasteCheesecakeTask")).thenReturn((task) -> {
            taskService().handleBpmnError(task.getId(), "err_NoShop");
        });

        Scenario.run(tastecheesecake).startByKey(PROCESS_KEY).execute();
    }

    @Test
    public void testInvitationOfAFriendMessageAndTestingContinued() {
        when(tastecheesecake.waitsAtTimerIntermediateEvent("NYTripEndedEvent")).thenReturn(event -> {
            runtimeService().correlateMessage("Msg_invite");
        });

        Scenario.run(tastecheesecake).startByKey(PROCESS_KEY).execute();
    }

    @Test
    public void testInvitationOfAFriendMessageAndTestingCanceled() {
        when(tastecheesecake.waitsAtTimerIntermediateEvent("NYTripEndedEvent")).thenReturn(event -> {
            runtimeService().correlateMessage("Msg_invite");
        });

        when(tastecheesecake.waitsAtUserTask("CheckInvitationTask")).thenReturn((task) -> {
            task.complete(withVariables("betterThanCake", true));
        });

        Scenario.run(tastecheesecake).startByKey(PROCESS_KEY).execute();
    }

}
