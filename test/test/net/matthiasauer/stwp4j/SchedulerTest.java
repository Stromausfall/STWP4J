package test.net.matthiasauer.stwp4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import net.matthiasauer.stwp4j.ChannelPortsCreated;
import net.matthiasauer.stwp4j.ChannelPortsRequest;
import net.matthiasauer.stwp4j.ExecutionState;
import net.matthiasauer.stwp4j.LightweightProcess;
import net.matthiasauer.stwp4j.PortType;
import net.matthiasauer.stwp4j.Scheduler;

public class SchedulerTest {
    private Collection<ChannelPortsRequest<?>> testChannelRequests;
    
    @Before
    public void setUp() throws Exception {
        this.testChannelRequests =
                Arrays.asList(
                        new ChannelPortsRequest<String>("foo", PortType.Output, String.class));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSchedulerAddProcessNoDuplicates() {
        Scheduler scheduler = new Scheduler();
        LightweightProcess lightweightProcess =
                new LightweightProcess(testChannelRequests) {
                    public ExecutionState execute() {
                        return ExecutionState.Finished;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                    }
                };
        
        scheduler.addProcess(lightweightProcess);
        scheduler.addProcess(lightweightProcess);
    }
    
    private LightweightProcess createTestProcess(AtomicReference<String> data, final int upTo) {
        return
                new LightweightProcess(testChannelRequests) {
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

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
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
                new LightweightProcess(testChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return null;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                    }
                });
        
        scheduler.performIteration();
    }

    @Test
    public void testAddProcessCallsInitializeMethod() {
        AtomicBoolean initializeMethodCalled =
                new AtomicBoolean(false);
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(
                new LightweightProcess(testChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return null;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        initializeMethodCalled.set(true);
                    }
                });
        
        assertTrue(
                "initialize method was not called after adding process",
                initializeMethodCalled.get());
    }

    @Test
    public void testAddProcessCallsInitializeMethodAndChannelsAreCreated() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests =
                Arrays.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.Output, String.class),
                        new ChannelPortsRequest<String>("foo2", PortType.Output, String.class),
                        new ChannelPortsRequest<String>("foo3", PortType.InputMultiplex, String.class),
                        new ChannelPortsRequest<String>("foo4", PortType.InputMultiplex, String.class),
                        new ChannelPortsRequest<String>("foo5", PortType.InputShared, String.class));
        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return null;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        assertEquals(
                                "number of output channels not correct",
                                2,
                                createdChannelPorts.getChannelOutPorts().size());
                        assertEquals(
                                "number of input channels not correct",
                                3,
                                createdChannelPorts.getChannelInPorts().size());
                    }
                });
    }
}
