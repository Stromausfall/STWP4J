package net.matthiasauer.stwp4j;

import java.util.concurrent.LinkedBlockingQueue;

abstract class ChannelPort<T> {
    private final Class<T> messageType;
    protected final LinkedBlockingQueue<T> messages;
    
    protected ChannelPort(Class<T> messageType) {
        this(messageType, new LinkedBlockingQueue<T>());
    }
    
    protected ChannelPort(Class<T> messageType, LinkedBlockingQueue<T> queue) {
        this.messages = queue;
        this.messageType = messageType;
    }

    public Class<T> getMessageType() {
        return this.messageType;
    }
}
