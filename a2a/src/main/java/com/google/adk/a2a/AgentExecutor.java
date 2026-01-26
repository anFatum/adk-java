package com.google.adk.a2a;

import com.google.adk.a2a.converters.PartConverter;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.BaseSessionService;
import com.google.adk.sessions.Session;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.genai.types.Content;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public class AgentExecutor implements io.a2a.server.agentexecution.AgentExecutor {

  private final Runner runner;
  private final String UserIdPrefix = "A2A_USER_";
  private final Map<String, Disposable> activeTasks = new ConcurrentHashMap<>();

  protected AgentExecutor(Runner runner) {
    this.runner = runner;
  }

  /** Builder for {@link AgentExecutor}. */
  public static class Builder {
    private Runner runner;

    @CanIgnoreReturnValue
    public Builder runner(Runner runner) {
      this.runner = runner;
      return this;
    }

    @CanIgnoreReturnValue
    public AgentExecutor build() {
      if (runner == null) {
        throw new IllegalStateException("Runner must be provided.");
      }
      return new AgentExecutor(runner);
    }
  }

  @Override
  public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    throw new UnsupportedOperationException("Unimplemented method 'cancel'");
  }

  @Override
  public void execute(RequestContext ctx, EventQueue eventQueue) throws JSONRPCError {
    TaskUpdater updater = new TaskUpdater(ctx, eventQueue);
    Message message = ctx.getMessage();
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null");
    }

    if (ctx.getTask() == null) {
      updater.startWork(message);
      eventQueue.enqueueEvent(statusUpdatedTask(ctx, message, TaskState.SUBMITTED));
    }

    Content c = PartConverter.messageToContent(message);

    Maybe<Session> maybeSession = prepareSession(ctx, runner.sessionService());

    // Group all reactive work for this task into one container
    CompositeDisposable taskDisposables = new CompositeDisposable();
    activeTasks.put(ctx.getTaskId(), taskDisposables);

    taskDisposables.add(
        maybeSession.subscribe(
            session -> {
              eventQueue.enqueueEvent(statusUpdatedTask(ctx, message, TaskState.WORKING));
              taskDisposables.add(
                  runner
                      .runAsync(getUserID(ctx), session.id(), c, null)
                      .subscribe(
                          event -> {},
                          error -> {
                            updater.fail(message);
                            cleanupTask(ctx.getTaskId());
                          },
                          () -> {
                            updater.complete(message);
                            cleanupTask(ctx.getTaskId());
                          }));
            },
            error -> {
              updater.fail(message);
              cleanupTask(ctx.getTaskId());
            }));
  }

  private void cleanupTask(String taskId) {
    Disposable d = activeTasks.remove(taskId);
    if (d != null) {
      d.dispose(); // Stops all streams in the CompositeDisposable
    }
  }

  private String getUserID(RequestContext ctx) {
    return UserIdPrefix + ctx.getContextId();
  }

  private String getAppName() {
    return runner.appName();
  }

  private Maybe<Session> prepareSession(RequestContext ctx, BaseSessionService service) {
    return service
        .getSession(getAppName(), getUserID(ctx), ctx.getContextId(), null)
        .switchIfEmpty(
            Maybe.defer(
                () -> {
                  return service.createSession(getAppName(), getUserID(ctx)).toMaybe();
                }));
  }

  private Task statusUpdatedTask(RequestContext context, @Nonnull Message msg, TaskState state) {
    return new Task.Builder()
        .id(context.getTaskId())
        .contextId(context.getContextId())
        .history(ImmutableList.of(msg))
        .status(new TaskStatus(state))
        .build();
  }
}
