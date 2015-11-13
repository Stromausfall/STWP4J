package net.matthiasauer.stwp4j;

import java.util.ArrayList;
import java.util.List;

public class Guard {
    private final List<ChannelInPort<?>> inPorts;

    public Guard(List<ChannelInPort<?>> inPorts) {
        if (inPorts == null) {
            throw new IllegalArgumentException("passed inPorts can't be null !");
        }
        
        this.inPorts = inPorts;
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
