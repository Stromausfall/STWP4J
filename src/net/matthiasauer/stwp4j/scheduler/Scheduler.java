package net.matthiasauer.stwp4j.scheduler;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

    public void addProcess(LightweightProcess lightweightProcess) {
        if (this.processes.contains(lightweightProcess)) {
            logger.error(this.id + " | already contained the lightweightProcess : " + lightweightProcess);
            throw new IllegalArgumentException("already contained the lightweight process");
        }

        logger.debug(this.id + " | added lightweightProcess to scheduler : " + lightweightProcess);;
        this.processes.add(lightweightProcess);
    }

    public void performIteration() {
        Queue<LightweightProcess> queue =
                new LinkedList<LightweightProcess>(this.processes);
        
        while (!queue.isEmpty()) {
            // get process
            LightweightProcess process = queue.poll();
            
            // execute it
            ExecutionState newState = process.execute();
            
            // if the state is not finished execute it again !
            if (newState != ExecutionState.Finished) {
                // add it at the end of the queue
                queue.add(process);
            } 
        }
    }
}
