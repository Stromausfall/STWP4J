package net.matthiasauer.stwp4j;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class Channel<T> {
    private final InputPortType inputType;
    private final Set<ChannelInPort<T>> inPorts;
    private final Set<ChannelOutPort<T>> outPorts;
    private final String id;
    private final Class<T> messageType;

    Class<T> getMessageType() {
        return this.messageType;
    }

    String getId() {
        return this.id;
    }

    Channel(InputPortType inputType, String id, Class<T> messageType) {
        this.inputType = inputType;
        this.id = id;
        this.messageType = messageType;
        this.inPorts = new HashSet<ChannelInPort<T>>();
        this.outPorts = new HashSet<ChannelOutPort<T>>();
    }

    public ChannelOutPort<T> createOutPort() {
        ChannelOutPort<T> outPort = new ChannelOutPort<T>(this.messageType);

        this.outPorts.add(outPort);

        return outPort;
    }
    
    public ChannelInPort<T> createInPort() {
        switch (this.inputType) {
        case Multiplex:
            return this.createMultiplexInPort();
        case Shared:
            return this.createSharedInPort();
        default:
            throw new NullPointerException("Unknown InputPortType : " + this.inputType);
        }
    }
    
    private ChannelInPort<T> createMultiplexInPort() {
        ChannelInPort<T> inPort = new ChannelInPort<T>(this.messageType);
        
        this.inPorts.add(inPort);
        
        return inPort;
    }

    private ChannelInPort<T> createSharedInPort() {
        ChannelInPort<T> inPort = null;

        if (this.inPorts.isEmpty()) {
            inPort = new ChannelInPort<T>(this.messageType);
        } else {
            ChannelInPort<T> alreadyExistingInPort = this.inPorts.iterator().next();
            inPort = new ChannelInPort<T>(alreadyExistingInPort);
        }

        this.inPorts.add(inPort);

        return inPort;
    }
    
    private void distributeMessagesMultiplex(Queue<T> messages) {
        for (ChannelInPort<T> inPort : this.inPorts) {
            inPort.messages.addAll(messages);
        }
    }
    
    private void distributeMessagesShared(Queue<T> messages) {
        if (this.inPorts.isEmpty()) {
            // nothing to do !
            return;
        }
        
        ChannelInPort<T> channelInPort = this.inPorts.iterator().next();
        List<T> uniqueInPortMessageCollections = channelInPort.messages;
        
        uniqueInPortMessageCollections.addAll(messages);
    }

    void forwardMessages() {
        // get all messages
        Queue<T> messages = new LinkedList<T>();
        for (ChannelOutPort<T> outPort : this.outPorts) {
            outPort.drainTo(messages);
        }

        // distribute the messages
        switch (this.inputType) {
        case Multiplex:
            this.distributeMessagesMultiplex(messages);
            break;
        case Shared:
            this.distributeMessagesShared(messages);
            break;
        default:
            throw new NullPointerException("Unknown InputPortType : " + this.inputType);
        }

        if (this.inPorts.isEmpty() && !messages.isEmpty()) {
            throw new IllegalStateException("channel '" + this.id + "' has messages (of type " + this.messageType
                    + ") to forward but no InPorts !");
        }
    }

    boolean allChannelsEmpty() {
        for (ChannelInPort<T> inPort : this.inPorts) {
            if (inPort.peek() != null) {
                return false;
            }
        }

        return true;
    }
}
