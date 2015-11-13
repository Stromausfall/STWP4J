package net.matthiasauer.stwp4j;

public final class ChannelInPort<T> extends ChannelPort<T> {
    public ChannelInPort(Class<T> channelType) {
        super(channelType);
    }
    
    public ChannelInPort(Class<T> channelType, ChannelInPort<T> toShareWith) {
        super(channelType, toShareWith.messages);
    }

    public T poll() {
        return this.messages.poll();
    }
    
    T peek() {
        return this.messages.peek();
    }   
}
