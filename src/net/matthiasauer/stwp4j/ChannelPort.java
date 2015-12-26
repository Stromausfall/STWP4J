package net.matthiasauer.stwp4j;

import java.util.LinkedList;

abstract class ChannelPort<T> {
    protected final Class<T> messageType;
    protected final LinkedList<T> messages;
    
    protected ChannelPort(Class<T> messageType) {
        this(messageType, new LinkedList<T>());
    }
    
    protected ChannelPort(Class<T> messageType, LinkedList<T> queue) {
        this.messages = queue;
        this.messageType = messageType;
    }

    public Class<T> getMessageType() {
        return this.messageType;
    }
}
