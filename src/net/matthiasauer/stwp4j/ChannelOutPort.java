package net.matthiasauer.stwp4j;

import java.util.Queue;

public class ChannelOutPort<T> extends ChannelPort<T> {
    public ChannelOutPort(Class<T> channelType) {
        super(channelType);
    }

    public boolean offer(T message) {
        return this.messages.offer(message);
    }

    void drainTo(Queue<T> toDrainTo) {
        this.messages.drainTo(toDrainTo);
    }
}
