package net.matthiasauer.stwp4j;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Scheduler {
    private static final Logger logger = Logger.getLogger(Scheduler.class.getName());
    private static int instanceCounter = 0;
    private final int id = (instanceCounter++);
    private Set<LightweightProcess> processes = new HashSet<LightweightProcess>();
    private Map<String, SchedulerChannel<?>> channels = new HashMap<String, SchedulerChannel<?>>();

    private void throwError(String message) {
        logger.severe(message);
        throw new IllegalArgumentException(message);
    }

    public void addProcess(LightweightProcess lightweightProcess) {
        if (this.processes.contains(lightweightProcess)) {
            throwError(this.id + " | process already added to the scheduler, process : " + lightweightProcess);
        }

        logger.info(this.id + " | added lightweightProcess to scheduler : " + lightweightProcess);
        ;
        this.processes.add(lightweightProcess);

        this.createOutputChannels(lightweightProcess);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private SchedulerChannel<?> getSchedulerChannelEntry(String channelId, Class<?> channelType) {
        SchedulerChannel<?> entry = this.channels.get(channelId);

        if (entry == null) {
            entry = new SchedulerChannel(channelId, channelType);

            logger.info(this.id + " | created a channel : " + channelId);

            this.channels.put(channelId, entry);
        }

        return entry;
    }

    private void createOutputChannels(LightweightProcess lightweightProcess) {
        List<ChannelPortsRequest<?>> channelRequests = lightweightProcess.getChannelRequests();
        Map<String, ChannelOutPort<?>> outputChannels = new HashMap<String, ChannelOutPort<?>>();
        Map<String, ChannelInPort<?>> inputChannels = new HashMap<String, ChannelInPort<?>>();

        for (ChannelPortsRequest<?> request : channelRequests) {
            final String channelId = request.getChannelId();
            final PortType channelPortType = request.getPortType();
            final Class<?> channelType = request.getType();
            SchedulerChannel<?> entry = this.getSchedulerChannelEntry(channelId, channelType);

            if (channelPortType.IsInput) {
                inputChannels.put(channelId, entry.getInChannel(lightweightProcess, channelPortType));
            } else {
                outputChannels.put(channelId, entry.getOutChannel(lightweightProcess, channelPortType));
            }
        }

        lightweightProcess.initialize(new ChannelPortsCreated(inputChannels, outputChannels));
    }

    public void performIteration() {
        // make sure all channels are built
        for (SchedulerChannel<?> entry : this.channels.values()) {
            entry.build();
        }

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
            for (SchedulerChannel<?> channel : this.channels.values()) {
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
