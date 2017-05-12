package org.camunda.optimize.test.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.engine.ProcessDefinitionEngineDto;
import org.camunda.optimize.rest.engine.dto.DeploymentDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceDto;
import org.camunda.optimize.rest.engine.dto.TaskDto;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.camunda.optimize.test.util.PropertyUtil;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Rule that performs clean up of engine on integration test startup and
 * one more clean up after integration test.
 *
 * Relies on expectation of /purge endpoint available in Tomcat for HTTP GET
 * requests and performing actual purge.
 *
 * @author Askar Akhmerov
 */
public class EngineIntegrationRule extends TestWatcher {

  private static final int MAX_WAIT = 10;
  public static final String COUNT = "count";
  public static final String DEFAULT_PROPERTIES_PATH = "it/it-test.properties";
  private String propertiesPath;

  private Properties properties;
  private Logger logger = LoggerFactory.getLogger(EngineIntegrationRule.class);

  private ObjectMapper objectMapper;

  public EngineIntegrationRule () {
    this(DEFAULT_PROPERTIES_PATH);
  }

  public EngineIntegrationRule(String propertiesLocation) {
    this.propertiesPath = propertiesLocation;
    properties = PropertyUtil.loadProperties(propertiesPath);
    setupObjectMapper();
  }

  public void init() {
    cleanEngine();
  }

  private void setupObjectMapper() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    DateFormat df = new SimpleDateFormat(properties.getProperty("camunda.optimize.serialization.date.format"));
    objectMapper.setDateFormat(df);
  }

  protected void starting(Description description) {
    this.init();
  }

  protected void finished(Description description) {
    cleanEngine();
  }

  private void cleanEngine() {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpGet getRequest = new HttpGet(properties.get("camunda.optimize.test.purge").toString());
    try {
      CloseableHttpResponse response = client.execute(getRequest);
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new RuntimeException("Something really bad happened during purge, " +
            "please check tomcat logs of engine-purge servlet");
      }
    } catch (IOException e) {
      logger.error("Error during purge request", e);
    } finally {
      closeClient(client);
    }
  }

  public void finishAllUserTasks() {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpGet get = new HttpGet(getTaskListUri());
    try {
      CloseableHttpResponse response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      List<TaskDto> tasks = objectMapper.readValue(responseString, new TypeReference<List<TaskDto>>() {
      });
      for (TaskDto task : tasks) {
        claimAndCompleteUserTask(client, task);
      }
    } catch (IOException e) {
      logger.error("Error while trying to finish the user task!!");
      e.printStackTrace();
    } finally {
      closeClient(client);
    }
  }

  private String getTaskListUri() {
    return getEngineUrl() + "/task";
  }

  private void claimAndCompleteUserTask(CloseableHttpClient client, TaskDto task) throws IOException {
    HttpPost claimPost = new HttpPost(getClaimTaskUri(task.getId()));
    claimPost.setEntity(new StringEntity("{ \"userId\" : " + "\"admin\"" + "}"));
    claimPost.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    CloseableHttpResponse response = client.execute(claimPost);
    if (response.getStatusLine().getStatusCode() != 204) {
      throw new RuntimeException("Could not claim user task!");
    }

    HttpPost completePost = new HttpPost(getCompleteTaskUri(task.getId()));
    completePost.setEntity(new StringEntity("{}"));
    completePost.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    response = client.execute(completePost);
    if (response.getStatusLine().getStatusCode() != 204) {
      throw new RuntimeException("Could not complete user task!");
    }
  }

  private String getClaimTaskUri(String taskId) {
    return getEngineUrl() + "/task/" + taskId + "/claim";
  }

  private String getCompleteTaskUri(String taskId) {
    return getEngineUrl() + "/task/" + taskId + "/complete";
  }

  public String getProcessDefinitionId() {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpRequestBase get = new HttpGet(getProcessDefinitionUri());
    CloseableHttpResponse response;
    try {
      response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      List<ProcessDefinitionEngineDto> procDefs =
        objectMapper.readValue(responseString, new TypeReference<List<ProcessDefinitionEngineDto>>(){});
      assertThat(procDefs.size(), is(1));
      client.close();
      return procDefs.get(0).getId();
    } catch (IOException e) {
      e.printStackTrace();
      throw new OptimizeRuntimeException("Could not fetch the process definition!");
    } finally {
      closeClient(client);
    }
  }

  public String deployAndStartProcessWithVariables(BpmnModelInstance bpmnModelInstance, Map<String, Object> variables) {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    DeploymentDto deployment = deployProcess(bpmnModelInstance, client);
    String processInstanceId = "";
    try {
      List<ProcessDefinitionEngineDto> procDefs = getAllProcessDefinitions(deployment, client);
      assertThat(procDefs.size(), is(1));
      processInstanceId = startProcessInstance(procDefs.get(0).getId(), client, variables);
      client.close();
    } catch (IOException e) {
      logger.error("Could not start the given process model!");
      e.printStackTrace();
    } finally {
      closeClient(client);
    }
    return processInstanceId;
  }

  private void closeClient(CloseableHttpClient client) {
    try {
      client.close();
    } catch (IOException e) {
      logger.error("Could not close clien!");
      e.printStackTrace();
    }
  }

  public String deployAndStartProcess(BpmnModelInstance bpmnModelInstance) {
    return deployAndStartProcessWithVariables(bpmnModelInstance, new HashMap<>());
  }

  public DeploymentDto deployProcess(BpmnModelInstance bpmnModelInstance, CloseableHttpClient client) {
    String process = Bpmn.convertToString(bpmnModelInstance);
    HttpPost deploymentRequest = createDeploymentRequest(process);
    DeploymentDto deployment = new DeploymentDto();
    try {
      CloseableHttpResponse response = client.execute(deploymentRequest);
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new RuntimeException("Something really bad happened during deployment, " +
          "could not create a deployment!");
      }
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      deployment = objectMapper.readValue(responseString, DeploymentDto.class);
    } catch (IOException e) {
      logger.error("Error during deployment request! Could not deploy the given process model!");
      e.printStackTrace();
    }
    return deployment;
  }

  private HttpPost createDeploymentRequest(String process) {
    HttpPost post = new HttpPost(getDeploymentUri());
    HttpEntity entity = MultipartEntityBuilder
      .create()
      .addTextBody("deployment-name", "deployment")
      .addTextBody("enable-duplicate-filtering", "false")
      .addTextBody("deployment-source", "process application")
      .addBinaryBody("data", process.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_OCTET_STREAM, "test.bpmn")
      .build();
    post.setEntity(entity);
    return post;
  }

  private String getDeploymentUri() {
    return getEngineUrl() + "/deployment/create";
  }

  private String getStartProcessInstanceUri(String procDefId) {
    return getEngineUrl() +  "/process-definition/" + procDefId + "/start";
  }

  private String getProcessDefinitionUri() {
    return getEngineUrl() + "/process-definition";
  }

  private String getCountHistoryUri() {
    return getEngineUrl() + "/history/process-instance/count";
  }

  private String getEngineUrl() {
    return properties.get("camunda.optimize.engine.rest").toString() +
        properties.get("camunda.optimize.engine.name").toString();
  }

  public List<ProcessDefinitionEngineDto> getAllProcessDefinitions(DeploymentDto deployment, CloseableHttpClient client) throws IOException {
    HttpRequestBase get = new HttpGet(getProcessDefinitionUri());
    URI uri = null;
    try {
      uri = new URIBuilder(get.getURI())
        .addParameter("deploymentId", deployment.getId())
        .build();
    } catch (URISyntaxException e) {
      logger.error("Could not build uri!");
      e.printStackTrace();
    }
    get.setURI(uri);
    CloseableHttpResponse response = client.execute(get);
    String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
    return objectMapper.readValue(responseString, new TypeReference<List<ProcessDefinitionEngineDto>>(){});
  }

  public String startProcessInstance(String procDefId, CloseableHttpClient client) throws IOException {
    return startProcessInstance(procDefId, client, new HashMap<>());
  }

  private String startProcessInstance(String procDefId, CloseableHttpClient client, Map<String, Object> variables) throws IOException {
    HttpPost post = new HttpPost(getStartProcessInstanceUri(procDefId));
    post.addHeader("content-type", "application/json");
    post.setEntity(new StringEntity(convertVariableMapToJsonString(variables)));
    CloseableHttpResponse response = client.execute(post);
    if (response.getStatusLine().getStatusCode() != 200) {
      throw new RuntimeException("Could not start the process definition " + procDefId +
      ". Reason: " + response.getStatusLine().getReasonPhrase());
    }
    String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
    ProcessInstanceDto instanceDto = objectMapper.readValue(responseString, ProcessInstanceDto.class);
    return instanceDto.getId();

  }

  private String convertVariableMapToJsonString(Map<String, Object> plainVariables) throws JsonProcessingException {
    Map<String, Object> variables = new HashMap<>();
    for (Map.Entry<String, Object> nameToValue : plainVariables.entrySet()) {
      Map<String, Object> fields = new HashMap<>();
      fields.put("value", nameToValue.getValue());
      fields.put("type", nameToValue.getValue().getClass().getSimpleName());
      variables.put(nameToValue.getKey(), fields);
    }
    Map<String, Object> variableWrapper = new HashMap<>();
    variableWrapper.put("variables", variables);
    return objectMapper.writeValueAsString(variableWrapper);
  }

  public void waitForAllProcessesToFinish() throws Exception {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    boolean done = false;
    HttpRequestBase get = new HttpGet(getCountHistoryUri());
    URI uri = null;
    try {
      uri = new URIBuilder(get.getURI())
          .addParameter("unfinished", "true")
          .build();
    } catch (URISyntaxException e) {
      logger.error("Could not build uri!");
      e.printStackTrace();
    }
    get.setURI(uri);
    int iterations = 0;
    Thread.sleep(1000);
    while (!done && iterations < MAX_WAIT) {
      CloseableHttpResponse response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      HashMap<String,Object> parsed = objectMapper.readValue(responseString, new TypeReference<HashMap<String,Object>>() {});
      if (!parsed.containsKey(COUNT)) throw new RuntimeException("Engine could not count PIs");
      if (Integer.valueOf(parsed.get(COUNT).toString()) != 0) {
        Thread.sleep(1000);
        iterations = iterations + 1;
      } else {
        done = true;
      }
    }
    client.close();
  }

  public final WebTarget target() {
    return this.client().target(getBaseUri());
  }

  private String getBaseUri() {
    return properties.getProperty("camunda.optimize.engine.rest");
  }

  public final Client client() {
    return this.getClient();
  }

  private Client getClient() {
    return ClientBuilder.newClient();
  }
}
