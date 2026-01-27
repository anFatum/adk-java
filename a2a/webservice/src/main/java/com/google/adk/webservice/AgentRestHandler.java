package com.google.adk.webservice;

import com.google.adk.a2a.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.AgentCard;
import io.a2a.transport.rest.handler.RestHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;

@Service
public class AgentRestHandler {

  private final Executor executor;
  private final RestHandler restHandler;

  public RestHandler getRestHandler() {
    return restHandler;
  }

  public AgentRestHandler(AgentExecutor customExecutor, AgentCard agentCard) {
    this.executor = Executors.newFixedThreadPool(4);

    // Manual instantiation of the RequestHandler
    // Requires TaskStore and QueueManager (e.g., InMemory implementations)

    PushNotificationConfigStore configStore = new InMemoryPushNotificationConfigStore();
    DefaultRequestHandler requestHandler =
        new DefaultRequestHandler(
            customExecutor,
            new InMemoryTaskStore(),
            new InMemoryQueueManager(),
            configStore,
            new BasePushNotificationSender(configStore),
            executor);

    restHandler = new RestHandler(agentCard, requestHandler);
  }
}
