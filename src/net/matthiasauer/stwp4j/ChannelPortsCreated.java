package net.matthiasauer.stwp4j;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChannelPortsCreated {
    private static final Logger logger = LoggerFactory.getLogger(ChannelPortsCreated.class);
    private final Map<String, ChannelInPort<?>> inputChannels;
    private final Map<String, ChannelOutPort<?>> outputChannels;

    public ChannelPortsCreated(
            Map<String, ChannelInPort<?>> inputChannels,
            Map<String, ChannelOutPort<?>> outputChannels) {
        this.inputChannels = inputChannels;
        this.outputChannels = outputChannels;
    }
    
    private void throwError(String message) {
        logger.error(message);
        throw new IllegalArgumentException(message);
    }

    @SuppressWarnings("unchecked")
    public <T> ChannelOutPort<T> getChannelOutPort(String id, Class<T> type) {
        ChannelOutPort<?> result = this.outputChannels.get(id);
        
        if (result == null) {
            this.throwError("found no out port for channelId : " + id);
        }
         
        if (result.getMessageType() != type) {
            this.throwError("type of messages in channel out port with id " + id + " was " + result.getMessageType() + " not " + type);
        }
        
        return (ChannelOutPort<T>) result;
    }

    @SuppressWarnings("unchecked")
    public <T> ChannelInPort<T> getChannelInPort(String id, Class<T> type) {
        ChannelInPort<?> result = this.inputChannels.get(id);
        
        if (result == null) {
            this.throwError("found no in port for channelId : " + id);
        }
        
        if (result.getMessageType() != type) {
            this.throwError("type of messages in channel in port with id " + id + " was " + result.getMessageType() + " not " + type);
        }
        
        return (ChannelInPort<T>) result;
    }

    public Map<String, ChannelOutPort<?>> getChannelOutPorts() {
        return
                Collections.unmodifiableMap(this.outputChannels);
    }

    public Map<String, ChannelInPort<?>> getChannelInPorts() {
        return
                Collections.unmodifiableMap(this.inputChannels);
    }

}
