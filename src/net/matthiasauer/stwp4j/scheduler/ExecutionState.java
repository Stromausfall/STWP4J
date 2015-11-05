package net.matthiasauer.stwp4j.scheduler;

public final class ExecutionState {
    static final ExecutionState New = new ExecutionState("New");
    public static final ExecutionState Working = new ExecutionState("Working");
    public static final ExecutionState Waiting = new ExecutionState("Waiting");
    public static final ExecutionState Finished = new ExecutionState("Finished");
    
    private final String value;
    
    private ExecutionState(String value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return this.value;
    }
}
