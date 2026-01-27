package com.google.adk.webservice;

import com.google.adk.a2a.AgentExecutor;
import com.google.adk.agents.BaseAgent;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.InMemorySessionService;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the transport-only A2A webservice stack.
 *
 * <p>Importers must supply a {@link BaseAgent} bean. The agent remains opaque to this module so the
 * transport can be reused across applications.
 *
 * <p>TODO:
 *
 * <ul>
 *   <li>Expose discovery endpoints (agent card / extended card) so clients can fetch metadata
 *       directly.
 *   <li>Add optional remote-proxy wiring for cases where no local agent bean is available.
 * </ul>
 *
 * <p>**EXPERIMENTAL:** Subject to change, rename, or removal in any future patch release. Do not
 * use in production code.
 */
@Configuration
@ComponentScan(basePackages = "com.google.adk.webservice")
public class A2ARemoteConfiguration {

  private static final String DEFAULT_APP_NAME = "a2a-remote-service";

  @Bean
  public AgentExecutor agentExecutor(
      BaseAgent agent, @Value("${a2a.remote.appName:" + DEFAULT_APP_NAME + "}") String appName) {
    InMemorySessionService sessionService = new InMemorySessionService();
    Runner runnerInstance =
        new Runner.Builder().agent(agent).appName(appName).sessionService(sessionService).build();
    return new AgentExecutor(runnerInstance);
  }

  @Bean
  public AgentCard agentCard(
      BaseAgent agent, @Value("${a2a.remote.appName:" + DEFAULT_APP_NAME + "}") String appName) {
    return new AgentCard.Builder()
        .name(agent.name())
        .description(agent.description())
        .capabilities(new AgentCapabilities.Builder().build())
        .defaultInputModes(List.of("text"))
        .defaultOutputModes(List.of("text"))
        .skills(List.of())
        .url("localhost:8080")
        .version("0.1")
        .build();
  }
}
