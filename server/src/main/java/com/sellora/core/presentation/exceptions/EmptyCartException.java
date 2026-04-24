package com.sellora.core.presentation.exceptions;

public class EmptyCartException extends RuntimeException {
  public EmptyCartException(String message) {
    super(message);
  }
}
