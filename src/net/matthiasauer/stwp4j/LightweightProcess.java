package net.matthiasauer.stwp4j;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.matthiasauer.stwp4j.utils.Pair;

public abstract class LightweightProcess {
    private final List<Pair<String, ChannelType>> channelRequests;
    
    protected LightweightProcess(Collection<Pair<String, ChannelType>> channelRequests) {
        this.channelRequests =
                Collections.unmodifiableList(
                        new LinkedList<Pair<String, ChannelType>>(channelRequests));
    }
    
    List<Pair<String, ChannelType>> getChannelRequests() {
        return this.channelRequests;
    }
    
    public abstract ExecutionState execute();

    public abstract void initialize(Collection<Pair<String, InChannel>> inputChannels, Collection<Pair<String, OutChannel>> outputChannels);
}
