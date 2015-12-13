package net.matthiasauer.stwp4j;

import java.util.ArrayDeque;

abstract class ChannelPort<T> {
    private final Class<T> messageType;
    protected final ArrayDeque<T> messages;
    
    protected ChannelPort(Class<T> messageType) {
        this(messageType, new ArrayDeque<T>());
    }
    
    protected ChannelPort(Class<T> messageType, ArrayDeque<T> queue) {
        this.messages = queue;
        this.messageType = messageType;
    }

    public Class<T> getMessageType() {
        return this.messageType;
    }
}
