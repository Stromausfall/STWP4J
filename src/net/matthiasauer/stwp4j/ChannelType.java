package net.matthiasauer.stwp4j;

enum ChannelType {
    Invalid(null, null),
    OneToOne(PortType.OutputExclusive, PortType.InputExclusive),
    OneToManyShared(PortType.OutputExclusive, PortType.InputShared),
    OneToManyMultiplex(PortType.OutputExclusive, PortType.InputMultiplex),
    ManyToOne(PortType.Output, PortType.InputExclusive),
    ManyToManyShared(PortType.Output, PortType.InputShared),
    ManyToManyMultiplex(PortType.Output, PortType.InputMultiplex);

    public final PortType OutPort;
    public final PortType InPort;
    
    private ChannelType(PortType outPort, PortType inPort) {
        this.OutPort = outPort;
        this.InPort = inPort;
    }
}
