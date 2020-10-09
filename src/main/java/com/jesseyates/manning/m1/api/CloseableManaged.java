package com.jesseyates.manning.m1.api;

import io.dropwizard.lifecycle.Managed;

import java.io.Closeable;

/**
 *
 */
public class CloseableManaged implements Managed {

  private final Closeable closeable;

  public CloseableManaged(Closeable closeable) {
    this.closeable = closeable;
  }

  public void start() throws Exception {
    // noop
  }

  public void stop() throws Exception {
    closeable.close();
  }
}
