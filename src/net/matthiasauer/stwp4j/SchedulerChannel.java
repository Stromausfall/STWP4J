package net.matthiasauer.stwp4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import net.matthiasauer.stwp4j.utils.Pair;

class SchedulerChannel<T> {
    private static final Logger logger = Logger.getLogger(SchedulerChannel.class.getName());
    private ChannelType type = null;

    private final Map<ChannelInPort<T>, Pair<LightweightProcess, PortType>> inChannels = new HashMap<ChannelInPort<T>, Pair<LightweightProcess, PortType>>();
    private final Map<ChannelOutPort<T>, Pair<LightweightProcess, PortType>> outChannels = new HashMap<ChannelOutPort<T>, Pair<LightweightProcess, PortType>>();
    private final String id;
    private final Class<T> messageType;

    public SchedulerChannel(String id, Class<T> messageType) {
        this.id = id;
        this.messageType = messageType;
    }

    private void throwError(String message) {
        this.errorMessage(message);

        String errorMessage = "channel : " + this.id + " | " + message;
        throw new IllegalStateException(errorMessage);
    }

    private void debugMessage(String message) {
        String errorMessage = "channel : " + this.id + " | " + message;
        logger.info(errorMessage);
    }

    private void errorMessage(String message) {
        String errorMessage = "channel : " + this.id + " | " + message;
        logger.severe(errorMessage);
    }

    public void build() {
        if ((this.type != ChannelType.Invalid) && (this.type != null)) {
            // nothing to do - we already determined the type
            return;
        }

        PortType inType = null;
        PortType outType = null;

        // check output ports
        for (Pair<LightweightProcess, PortType> channel : outChannels.values()) {
            if (outType == null) {
                // no previous type
                outType = channel.second;
            }

            if (outType == PortType.InputExclusive) {
                // if exclusive there can't be any other output !
                this.throwError("tried adding another output to a channel that is " + outType);
            }

            if (outType != channel.second) {
                this.throwError("tried adding a " + channel.second + " output port to a channel that is " + outType);
            }
        }

        // check input ports
        for (Pair<LightweightProcess, PortType> channel : inChannels.values()) {
            if (inType == null) {
                // no previous type
                inType = channel.second;
            }

            if (inType == PortType.OutputExclusive) {
                // if exclusive there can't be any other output !
                this.throwError("tried adding another input to a channel that is " + outType);
            }

            if (inType != channel.second) {
                this.throwError("tried adding a " + channel.second + " input port to a channel that is " + inType);
            }
        }

        for (ChannelType channelType : ChannelType.values()) {
            if ((channelType.OutPort == outType) && (channelType.InPort == inType)) {
                this.type = channelType;
            }
        }

        if (this.type == null) {
            this.errorMessage("unknown channel for InPort type : " + inType + " and OutPort type : " + outType
                    + " (messages can not be sent and not received!)");
            this.type = ChannelType.Invalid;
        }

        if (this.type != ChannelType.Invalid) {
            this.debugMessage("Channel has type : " + this.type + " with " + this.inChannels.size()
                    + " input ports and " + this.outChannels.size() + " output ports");
        }
    }

    public ChannelOutPort<T> getOutChannel(LightweightProcess lightweightProcess, PortType outPortType) {
        ChannelOutPort<T> channel = new ChannelOutPort<T>(this.messageType);

        this.outChannels.put(channel, new Pair<LightweightProcess, PortType>(lightweightProcess, outPortType));

        return channel;
    }

    public ChannelInPort<T> getInChannel(LightweightProcess lightweightProcess, PortType inPortType) {
        ChannelInPort<T> inPort = null;

        if ((inPortType == PortType.InputShared) && (!this.inChannels.isEmpty())) {
            // if it is a shared - use the channel from the
            ChannelInPort<T> sourceInPort = this.inChannels.keySet().iterator().next();

            inPort = new ChannelInPort<T>(this.messageType, sourceInPort);
        } else {
            inPort = new ChannelInPort<T>(this.messageType);
        }

        this.inChannels.put(inPort, new Pair<LightweightProcess, PortType>(lightweightProcess, inPortType));

        return inPort;
    }

    public void forwardMessages() {
        // get all messages
        Queue<T> messages = new LinkedList<T>();
        for (ChannelOutPort<T> outPort : this.outChannels.keySet()) {
            outPort.drainTo(messages);
        }

        // distribute the messages
        if (this.type.InPort == PortType.InputMultiplex) {
            for (ChannelInPort<T> inPort : this.inChannels.keySet()) {
                inPort.messages.addAll(messages);
            }
        } else {
            // for shared and exclusive there is only one effective inPort
            // (shared!)
            if (!this.inChannels.keySet().isEmpty()) {
                ChannelInPort<T> inPort = this.inChannels.keySet().iterator().next();
    
                inPort.messages.addAll(messages);
            }
        }

        if (this.inChannels.keySet().isEmpty() && !messages.isEmpty()) {
            throw new NullPointerException("No InPorts for channel '" + this.id + "'");
        }
    }

    public boolean allChannelsEmpty() {
        for (ChannelInPort<T> inPort : this.inChannels.keySet()) {
            if (inPort.peek() != null) {
                return false;
            }
        }

        return true;
    }
}
