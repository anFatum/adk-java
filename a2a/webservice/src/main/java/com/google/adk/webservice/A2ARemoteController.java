package com.google.adk.webservice;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.transport.rest.handler.RestHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing an A2A-compliant JSON-RPC endpoint backed by a local ADK runner.
 *
 * <p>**EXPERIMENTAL:** Subject to change, rename, or removal in any future patch release. Do not
 * use in production code.
 */
@RestController
@RequestMapping("/a2a/remote")
public class A2ARemoteController {

  private static final Logger logger = LoggerFactory.getLogger(A2ARemoteController.class);

  private final AgentRestHandler service;

  public A2ARemoteController(AgentRestHandler service) {
    this.service = service;
  }

  @PostMapping(
      path = "/v1/message:send",
      consumes = "application/json",
      produces = "application/json")
  public RestHandler.HTTPRestResponse sendMessage(
      @RequestHeader HttpHeaders header, @RequestBody String body, HttpServletRequest request) {
    logger.debug("Received remote A2A request: {}", request);
    ServerCallContext ctx = new ServerCallContext(UnauthenticatedUser.INSTANCE, new HashMap<>());
    RestHandler.HTTPRestResponse response = service.getRestHandler().sendMessage(body, ctx);
    logger.debug("Responding with remote A2A payload: {}", response);
    return response;
  }
}
