package com.jesseyates.manning.common;

import com.jesseyates.manning.common.StreamConfiguration;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

/**
 * Entry point for running a stream processing application.
 */
public class App<T extends StreamConfiguration> extends Application<T> {

  @Override
  public void run(T configuration, Environment environment) throws Exception {
    // noop, nothing interesting needs to be setup. All the interesting work happens in
    // StreamProcessor
  }
}
