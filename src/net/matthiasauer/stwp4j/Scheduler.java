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
            throw new IllegalArgumentException(this.id + " | process already added to the scheduler, process : " + lightweightProcess);
        }

        this.processes.add(lightweightProcess);
    }

    public <T> Channel<T> createSharedChannel(String id, Class<T> messageType) {
        Channel<T> channel = new Channel<T>(InputPortType.Shared, id, messageType);

        this.channels.add(channel);

        return channel;
    }

    public <T> Channel<T> createMultiplexChannel(String id, Class<T> messageType) {
        Channel<T> channel = new Channel<T>(InputPortType.Multiplex, id, messageType);

        this.channels.add(channel);

        return channel;
    }

    public void performIteration() {
        // PRE-ITERATION
        for (LightweightProcess process : this.processes) {
            // and execute the preIteration methods
            process.preIteration();
        }

        // repeat until the iteration has ended
        boolean performSubIteration = true;

        while (performSubIteration) {
            performSubIteration = false;

            // execute the LightweightProcesses
            for (LightweightProcess process : this.processes) {

                // EXECUTE
                process.execute();
            }

            boolean allChannelsEmpty = true;

            // forward stuff immediately... implement something better later !
            for (Channel<?> channel : this.channels) {
                // if a channel was not emptied !
                if (!channel.allChannelsEmpty()) {
                    throw new IllegalStateException("channel '" + channel.getId() + "' (of type "
                            + channel.getMessageType()
                            + ") was NOT empty at the end of the subIteration - each channel has to be always emptied !");
                }

                channel.forwardMessages();

                allChannelsEmpty = allChannelsEmpty && channel.allChannelsEmpty();
            }

            performSubIteration = !allChannelsEmpty;
        }

        // POST-ITERATION
        for (LightweightProcess process : this.processes) {
            process.postIteration();
        }
    }
}
