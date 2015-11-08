package net.matthiasauer.stwp4j;

public class ChannelInPort<T> extends ChannelPort<T> {
    public ChannelInPort(Class<T> channelType) {
        super(channelType);
    }
    
    public ChannelInPort(Class<T> channelType, ChannelInPort<T> toShareWith) {
        super(channelType, toShareWith.messages);
    }

    public T poll() {
        T xxx = this.messages.poll();
        
        System.err.println(xxx+ " - " + this.messages.hashCode());
        
        return xxx;
        
        //return this.messages.poll();
    }
}
