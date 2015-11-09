package net.matthiasauer.stwp4j;

public class ChannelInPort<T> extends ChannelPort<T> {
    public ChannelInPort(Class<T> channelType) {
        super(channelType);
    }
    
    public ChannelInPort(Class<T> channelType, ChannelInPort<T> toShareWith) {
        super(channelType, toShareWith.messages);
    }

    public T poll() {
        return this.messages.poll();
    }
}
