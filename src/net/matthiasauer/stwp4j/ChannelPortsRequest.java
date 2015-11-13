package net.matthiasauer.stwp4j;

public final class ChannelPortsRequest<T> {
    private final String channelId;
    private final PortType portType;
    private final Class<T> type;
    
    public ChannelPortsRequest(String channelId, PortType portType, Class<T> type) {
        this.channelId = channelId;
        this.portType = portType;
        this.type = type;
    }

    public String getChannelId() {
        return this.channelId;
    }

    public PortType getPortType() {
        return this.portType;
    }

    public Class<T> getType() {
        return this.type;
    }
}
