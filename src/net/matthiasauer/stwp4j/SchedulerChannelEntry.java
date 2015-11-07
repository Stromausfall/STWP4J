package net.matthiasauer.stwp4j;

import java.util.LinkedList;
import java.util.List;

class SchedulerChannelEntry {
    private enum ChannelMultiplicity {
        Invalid,
        OneToOne,
        OneToMany,
        ManyToOne,
        ManyToMany
    }
    
    private enum ChannelType {
        Unknown,
        Multiplex,
        Shared
    }
    
    private ChannelMultiplicity multiplicity = ChannelMultiplicity.Invalid;
    private ChannelType type = ChannelType.Unknown;
    
    private List<LightweightProcess> inChannel =
            new LinkedList<LightweightProcess>();
    private List<LightweightProcess> outChannelMultiplex =
            new LinkedList<LightweightProcess>();
    private List<LightweightProcess> outChannelShared =
            new LinkedList<LightweightProcess>();

    public void getInChannel(LightweightProcess lightweightProcess) {
        this.inChannel.add(lightweightProcess);
    }

    public void getOutChannelMultiplex(LightweightProcess lightweightProcess) {
        this.outChannelMultiplex.add(lightweightProcess);
    }

    public void getOutChannelShared(LightweightProcess lightweightProcess) {
        this.outChannelShared.add(lightweightProcess);
    }

    public void build() {
        if ((outChannelMultiplex.size() != 0) && (outChannelShared.size() != 0)) {
            throw new IllegalArgumentException("problem with mixing outchannels : multiplex and shared");
        }
        
    }
}
