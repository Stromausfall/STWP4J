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
    
    List<ChannelPortsRequest<?>> getChannelRequests() {
        return this.channelRequests;
    }
    
    abstract ExecutionState execute();

    abstract void initialize(ChannelPortsCreated createdChannelPorts);
    
    void preIteration() {
    }
    
    void postIteration() {
    }
}
