package net.matthiasauer.stwp4j;

import java.util.HashSet;
import java.util.Set;

public class Scheduler {
    private static int instanceCounter = 0;
    private final int id = (instanceCounter++);
    private final Set<LightweightProcess> processes = new HashSet<LightweightProcess>();
    private final Set<Channel<?>> channels = new HashSet<Channel<?>>();

    public void addProcess(LightweightProcess lightweightProcess) {
        if (this.processes.contains(lightweightProcess)) {
            throw new IllegalArgumentException(
                    this.id + " | process already added to the scheduler, process : " + lightweightProcess);
        }

        this.processes.add(lightweightProcess);
    }

    public <T> Channel<T> createSharedChannel(String id, Class<T> messageType, boolean mustBeEmptyAfterEachIteration,
            boolean allowMessagesWithoutHavingInPorts) {
        Channel<T> channel = new Channel<T>(InputPortType.Shared, id, messageType, mustBeEmptyAfterEachIteration,
                allowMessagesWithoutHavingInPorts);

        this.channels.add(channel);

        return channel;
    }

    public <T> Channel<T> createMultiplexChannel(String id, Class<T> messageType, boolean mustBeEmptyAfterEachIteration,
            boolean allowMessagesWithoutHavingInPorts) {
        Channel<T> channel = new Channel<T>(InputPortType.Multiplex, id, messageType, mustBeEmptyAfterEachIteration,
                allowMessagesWithoutHavingInPorts);

        this.channels.add(channel);

        return channel;
    }
    
    private int forwardMessages() {
        int forwardedMessages = 0;
        
        for (Channel<?> channel : this.channels) {
            forwardedMessages += channel.forwardMessages();
        }
        
        return forwardedMessages;
    }

    public void performIteration() {
        // PRE-ITERATION
        for (LightweightProcess process : this.processes) {
            // and execute the preIteration methods
            process.preIteration();
        }
        
        // forward messages from PRE-iteration
        this.forwardMessages();

        // repeat until the iteration has ended
        boolean performSubIteration = true;

        while (performSubIteration) {
            performSubIteration = false;

            // execute the LightweightProcesses
            for (LightweightProcess process : this.processes) {

                // EXECUTE
                process.execute();
            }

            // forward messages and check whether anything was forwarded
            final int forwardedMessages = forwardMessages();

            performSubIteration = (forwardedMessages != 0);
        }

        // POST-ITERATION
        for (LightweightProcess process : this.processes) {
            process.postIteration();
        }

        // POST-ITERATION channel checks
        for (Channel<?> channel : this.channels) {
            channel.performPostIterationCheck();
        }
        
        this.forwardMessages();
    }
}
