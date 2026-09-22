package com.mylog.common.outbox;

public interface OutboxTransport {

    void publish(MessageEnvelope envelope);

    void deadLetter(MessageEnvelope envelope, String errorCode);
}
