package net.matthiasauer.stwp4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private static final AtomicInteger instanceCounter =
            new AtomicInteger(0);
    private final int id = instanceCounter.incrementAndGet();
    private Set<LightweightProcess> processes =
            new HashSet<LightweightProcess>();
    private Map<String, SchedulerChannel<?>> channels =
            new HashMap<String, SchedulerChannel<?>>();
    
    private void throwError(String message) {
        logger.error(message);
        throw new IllegalArgumentException(message);
    }

    public void addProcess(LightweightProcess lightweightProcess) {
        if (this.processes.contains(lightweightProcess)) {
            throwError(this.id + " | process already added to the scheduler, process : " + lightweightProcess);
        }

        logger.debug(this.id + " | added lightweightProcess to scheduler : " + lightweightProcess);;
        this.processes.add(lightweightProcess);
        
        this.createOutputChannels(lightweightProcess);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private SchedulerChannel<?> getSchedulerChannelEntry(String channelId, Class<?> channelType) {
        SchedulerChannel<?> entry = this.channels.get(channelId);
        
        if (entry == null) {
            entry = new SchedulerChannel(channelId, channelType);
            
            logger.debug(this.id + " | created a channel : " + channelId);
            
            this.channels.put(channelId, entry);
        }
        
        return entry;
    }
    
    private void createOutputChannels(LightweightProcess lightweightProcess) {
        List<ChannelPortsRequest<?>> channelRequests =
                lightweightProcess.getChannelRequests();
        Map<String, ChannelOutPort<?>> outputChannels =
                new HashMap<String, ChannelOutPort<?>>();
        Map<String, ChannelInPort<?>> inputChannels =
                new HashMap<String, ChannelInPort<?>>();
        
        for (ChannelPortsRequest<?> request : channelRequests) {
            final String channelId = request.getChannelId();
            final PortType channelPortType = request.getPortType();
            final Class<?> channelType = request.getType();
            SchedulerChannel<?> entry =
                    this.getSchedulerChannelEntry(channelId, channelType);
            
            if (channelPortType.IsInput) {
                inputChannels.put(
                        channelId,
                        entry.getInChannel(lightweightProcess, channelPortType));
            } else {
                outputChannels.put(
                        channelId,
                        entry.getOutChannel(lightweightProcess, channelPortType));
            }
        }
        
        lightweightProcess.initialize(
                new ChannelPortsCreated(inputChannels, outputChannels));
    }

    public void performIteration() {
        Queue<LightweightProcess> queue =
                new LinkedList<LightweightProcess>(this.processes);
        Queue<LightweightProcess> queue2 =
                new LinkedList<LightweightProcess>();
        // make sure all channels are built
        for (SchedulerChannel<?> entry : this.channels.values()) {
            entry.build();
        }
        
        boolean allRemainingProcessesAreWaiting = true;
        
        while (!queue.isEmpty()) {
            // get process
            LightweightProcess process = queue.poll();
            
            // execute it
            ExecutionState newState = process.execute();
            
            if (newState == null) {
                logger.error(this.id + " | ExecutionState 'null' of process : " + process);
                throw new IllegalStateException("returned ExecutionState was null !");
            }
            
            // if the state is not finished execute it again !
            if (newState != ExecutionState.Finished) {
                // add it at the end of the queue
                queue2.add(process);
            }
            
            allRemainingProcessesAreWaiting =
                    allRemainingProcessesAreWaiting
                    && (newState == ExecutionState.Waiting);
            
            if (queue.isEmpty()) {                
                if (allRemainingProcessesAreWaiting) {
                    // all remaining processes are waiting - the iteration has 
                    // therefore finished !
                    break;
                }
                
                // forward stuff immediately... implement something better later !
                for (SchedulerChannel<?> channel : this.channels.values()) {
                    channel.forwardMessages();
                }
                
                // change the channels
                Queue<LightweightProcess> temp = queue;
                
                queue = queue2;
                queue2 = temp;
                
                // reset the variable that determines whether all proceses
                // are waiting (and therefore the iteration should finish)
                allRemainingProcessesAreWaiting = true;
            }
        }
    }
}
