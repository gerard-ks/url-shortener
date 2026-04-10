package org.urlshortener.urlshortener.event;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEventBus {
    void subscribe(String shortCode, SseEmitter emitter);
    void unsubscribe(String shortCode, SseEmitter emitter);
    void onAccessEvent(AccessEvent event);
    int getSubscriberCount(String shortCode);
}
