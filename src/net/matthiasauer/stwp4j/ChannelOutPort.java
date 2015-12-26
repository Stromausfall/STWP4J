package net.matthiasauer.stwp4j;

import java.util.Queue;

public final class ChannelOutPort<T> extends ChannelPort<T> {
    ChannelOutPort(Class<T> channelType) {
        super(channelType);
    }

    public boolean offer(T message) {
        return this.messages.offer(message);
    }

    void drainTo(Queue<T> toDrainTo) {
        toDrainTo.addAll(this.messages);
        this.messages.clear();
    }
}
