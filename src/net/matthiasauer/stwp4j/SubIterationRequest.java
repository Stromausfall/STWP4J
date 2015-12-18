package net.matthiasauer.stwp4j;

import java.util.LinkedList;
import java.util.List;

public class SubIterationRequest {
    private boolean triggerCalled = false;
    private final List<ChannelInPort<?>> channelsToWatch = new LinkedList<ChannelInPort<?>>();

    SubIterationRequest() {
    }

    public void triggerIfNotEmpty(ChannelInPort<?> channelInPort) {
        this.channelsToWatch.add(channelInPort);
    }

    void reset() {
        this.triggerCalled = false;
        this.channelsToWatch.clear();
    }

    boolean isExecutedInNextSubIteration() {
        boolean atLeastOneChannelsNotEmpty = false;

        for (ChannelInPort<?> channel : channelsToWatch) {
            boolean channelIsNotEmpty = channel.peek() != null;

            atLeastOneChannelsNotEmpty = atLeastOneChannelsNotEmpty || channelIsNotEmpty;
        }

        return this.triggerCalled || atLeastOneChannelsNotEmpty;
    }

    public void forceTrigger() {
        this.triggerCalled = true;
    }
}
