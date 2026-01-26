package com.google.adk.a2a.common;

/** Exception thrown when the the genai class has an empty field. */
public class GenAIFieldMissingException extends RuntimeException {
  public GenAIFieldMissingException(String message) {
    super(message);
  }

  public GenAIFieldMissingException(String message, Throwable cause) {
    super(message, cause);
  }
}
