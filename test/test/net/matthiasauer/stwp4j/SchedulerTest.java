package test.net.matthiasauer.stwp4j;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import net.matthiasauer.stwp4j.ExecutionState;
import net.matthiasauer.stwp4j.LightweightProcess;
import net.matthiasauer.stwp4j.Scheduler;

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
                            return ExecutionState.Working;
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

    @Test(expected=IllegalStateException.class)
    public void testSchedulerCausesExceptionIfProcessReturnsNull() {
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(
                new LightweightProcess() {
                    @Override
                    public ExecutionState execute() {
                        return null;
                    }
                });
        
        scheduler.performIteration();
    }

    @Test
    public void testGetMessageHubResultDoesntChange() {
        Scheduler scheduler = new Scheduler();
        assertSame(
                "getMessageHub should always return the same instance",
                scheduler.getConnectionHub(),
                scheduler.getConnectionHub());
    }

}
