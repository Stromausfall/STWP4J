package net.matthiasauer.stwp4j;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class LightweightProcess {
    private final List<ChannelPortsRequest<?>> channelRequests;
    
    protected LightweightProcess(Collection<ChannelPortsRequest<?>> channelRequests) {
        this.channelRequests =
                Collections.unmodifiableList(
                        new LinkedList<ChannelPortsRequest<?>>(channelRequests));
    }
    
    private static Collection<ChannelPortsRequest<?>> convert(ChannelPortsRequest<?> ... elements) {
        Collection<ChannelPortsRequest<?>> list =
                new LinkedList<ChannelPortsRequest<?>>();
        
        for (ChannelPortsRequest<?> element : elements) {
            list.add(element);
        }
        
        return list;
    }
    
    protected LightweightProcess(ChannelPortsRequest<?> ... channelRequests) {
        this(convert(channelRequests));
    }
    
    final List<ChannelPortsRequest<?>> getChannelRequests() {
        return this.channelRequests;
    }
    
    protected abstract ExecutionState execute();

    protected abstract void initialize(ChannelPortsCreated createdChannelPorts);
    
    protected void preIteration() {
    }
    
    protected void postIteration() {
    }
}
