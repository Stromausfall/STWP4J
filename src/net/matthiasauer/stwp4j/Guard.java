package net.matthiasauer.stwp4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Guard {
    private final List<ChannelInPort<?>> inPorts;

    public Guard(Collection<ChannelInPort<?>> inPorts) {
        if (inPorts.isEmpty()) {
            throw new IllegalArgumentException("passed inPorts can't be empty !");
        }
        
        this.inPorts =
                new LinkedList<ChannelInPort<?>>(inPorts);
    }

    public Guard(ChannelInPort<?> ... inPorts) {
        if (inPorts.length == 0) {
            throw new IllegalArgumentException("passed inPorts can't be empty !");
        }
        
        List<ChannelInPort<?>> list =
                new LinkedList<ChannelInPort<?>>();
        
        for (ChannelInPort<?> inPort : inPorts) {
            list.add(inPort);
        }
        
        this.inPorts = list;
    }
    
    private boolean allAreReady() {
        for (ChannelInPort<?> inPort : this.inPorts) {
            if (inPort.peek() == null) {
                // at least one port that offers no element !
                return false;
            }
        }
        
        return true;
    }

    public List<Object> poll() {
        if (!this.allAreReady()) {
            // not all inPorts to guard offer an element !
            return null;
        }
        
        List<Object> result =
                new ArrayList<Object>();
        
        for (ChannelInPort<?> inPort : this.inPorts) {
            result.add(inPort.poll());
        }
        
        return result;
    }
}
