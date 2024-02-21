package com.arextest.storage.model.event;

import org.springframework.context.ApplicationEvent;

public class ApplicationCreationEvent extends ApplicationEvent {

  public ApplicationCreationEvent(String appId) {
    super(appId);
  }
}
