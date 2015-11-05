package test.net.matthiasauer.stwp4j.scheduler;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import net.matthiasauer.stwp4j.scheduler.ExecutionState;
import net.matthiasauer.stwp4j.scheduler.LightweightProcess;
import net.matthiasauer.stwp4j.scheduler.Scheduler;

public class SchedulerTest {

    @Test(expected = IllegalArgumentException.class)
    public void testSchedulerAddProcessNoDuplicates() {
        Scheduler scheduler = new Scheduler();
        LightweightProcess lightweightProcess =
                new LightweightProcess() {
                    public ExecutionState execute() {
                        return ExecutionState.Finished;
                    }
                };
        
        scheduler.addProcess(lightweightProcess);
        scheduler.addProcess(lightweightProcess);
    }
    
    private LightweightProcess createTestProcess(AtomicReference<String> data, final int upTo) {
        return
                new LightweightProcess() {
                    int counter = 0;
            
                    public ExecutionState execute() {
                        data.set(data.get() + counter);
                        counter++;
                        
                        if (counter <= upTo) {
                            return ExecutionState.Waiting;
                        } else {
                            return ExecutionState.Finished;
                        }
                    }
                };
    }

    @Test
    public void testSchedulerExecutesUntilInStateFinished() {
        final AtomicReference<String> data = new AtomicReference<String>("");
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(this.createTestProcess(data, 4));
        scheduler.performIteration();
        
        assertEquals(
                "incorrect execution of a single lightweight process",
                "01234",
                data.get());
    }

    @Test
    public void testSchedulerExecutesMultipleProcessesConcurrently() {
        final AtomicReference<String> data = new AtomicReference<String>("");
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(this.createTestProcess(data, 4));
        scheduler.addProcess(this.createTestProcess(data, 5));
        scheduler.addProcess(this.createTestProcess(data, 3));
        scheduler.addProcess(this.createTestProcess(data, 4));
        scheduler.addProcess(this.createTestProcess(data, 2));
        scheduler.performIteration();
        
        assertEquals(
                "incorrect execution of a single lightweight process",
                "00000111112222233334445",
                data.get());
    }
}
