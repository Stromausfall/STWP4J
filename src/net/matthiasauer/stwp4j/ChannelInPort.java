package net.matthiasauer.stwp4j;

public final class ChannelInPort<T> extends ChannelPort<T> {
    ChannelInPort(Class<T> channelType) {
        super(channelType);
    }
    
    ChannelInPort(ChannelInPort<T> toShareWith) {
        super(toShareWith.messageType, toShareWith.messages);
    }

    public T poll() {
        return this.messages.poll();
    }
    
    T peek() {
        return this.messages.peek();
    }   
}
