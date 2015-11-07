package net.matthiasauer.stwp4j;

import java.util.Collection;
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

import net.matthiasauer.stwp4j.utils.Pair;

public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private static final AtomicInteger instanceCounter =
            new AtomicInteger(0);
    private final int id = instanceCounter.incrementAndGet();
    private Set<LightweightProcess> processes =
            new HashSet<LightweightProcess>();
    private Map<String, SchedulerChannelEntry> channels =
            new HashMap<String, SchedulerChannelEntry>();
    private boolean newChannelsAdded = false;    

    public void addProcess(LightweightProcess lightweightProcess) {
        if (this.processes.contains(lightweightProcess)) {
            logger.error(this.id + " | already contained the lightweightProcess : " + lightweightProcess);
            throw new IllegalArgumentException("already contained the lightweight process");
        }

        logger.debug(this.id + " | added lightweightProcess to scheduler : " + lightweightProcess);;
        this.processes.add(lightweightProcess);
        
        lightweightProcess.initialize(
                createInputChannels(lightweightProcess),
                createOutputChannels(lightweightProcess));
    }
    
    private SchedulerChannelEntry getSchedulerChannelEntry(String channelId) {
        SchedulerChannelEntry entry = this.channels.get(channelId);
        
        if (entry == null) {
            entry = new SchedulerChannelEntry();
            
            this.channels.put(channelId, entry);
        }
        
        return entry;
    }
    
    private Collection<Pair<String, OutChannel>> createOutputChannels(
            LightweightProcess lightweightProcess) {
        List<Pair<String, ChannelType>> channelRequests =
                lightweightProcess.getChannelRequests();
        Collection<Pair<String, OutChannel>> result =
                new LinkedList<Pair<String, OutChannel>>();
        
        for (Pair<String, ChannelType> request : channelRequests) {
            final String channelId = request.first;
            final ChannelType channelType = request.second;
            
            if ((channelType == ChannelType.OutputMultiplex)
                    || (channelType == ChannelType.OutputShared)) {
                
                SchedulerChannelEntry entry =
                        this.getSchedulerChannelEntry(channelId);
                
                if (channelType == ChannelType.OutputMultiplex) {
                    entry.getOutChannelMultiplex(lightweightProcess);
                } else {
                    entry.getOutChannelShared(lightweightProcess);
                }
                
                result.add(new Pair<String, OutChannel>(null, null));
                
                this.newChannelsAdded = true;
            }
        }
        
        return result;
    }

    private Collection<Pair<String, InChannel>> createInputChannels(
            LightweightProcess lightweightProcess) {
        List<Pair<String, ChannelType>> channelRequests =
                lightweightProcess.getChannelRequests();
        Collection<Pair<String, InChannel>> result =
                new LinkedList<Pair<String, InChannel>>();
        
        for (Pair<String, ChannelType> request : channelRequests) {
            final String channelId = request.first;
            final ChannelType channelType = request.second;
            
            if (channelType == ChannelType.Input) {
                
                SchedulerChannelEntry entry =
                        this.getSchedulerChannelEntry(channelId);
                
                entry.getInChannel(lightweightProcess);
                
                result.add(new Pair<String, InChannel>(null, null));
                
                this.newChannelsAdded = true;
            }
        }
        
        return result;
    }
    

    public void performIteration() {
        Queue<LightweightProcess> queue =
                new LinkedList<LightweightProcess>(this.processes);
        
        for (SchedulerChannelEntry entry : this.channels.values()) {
            entry.build();
        }
        
        while (!queue.isEmpty()) {
            // get process
            LightweightProcess process = queue.poll();
            
            // execute it
            ExecutionState newState = process.execute();
            
            if (newState == null) {
                logger.error("ExecutionState 'null' of process : " + process);
                throw new IllegalStateException("returned ExecutionState was null !");
            }
            
            // if the state is not finished execute it again !
            if (newState != ExecutionState.Finished) {
                // add it at the end of the queue
                queue.add(process);
            } 
        }
    }
}
