package net.matthiasauer.stwp4j;

public enum PortType {
    Output(false),
    OutputExclusive(false),
    InputMultiplex(true),
    InputShared(true),
    InputExclusive(true);
    
    public final boolean IsInput;
    
    private PortType(boolean isInput) {
        this.IsInput = isInput;
    }
}
