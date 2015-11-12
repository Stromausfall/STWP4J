package test.net.matthiasauer.stwp4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import net.matthiasauer.stwp4j.ChannelInPort;
import net.matthiasauer.stwp4j.ChannelOutPort;
import net.matthiasauer.stwp4j.ChannelPortsCreated;
import net.matthiasauer.stwp4j.ChannelPortsRequest;
import net.matthiasauer.stwp4j.ExecutionState;
import net.matthiasauer.stwp4j.LightweightProcess;
import net.matthiasauer.stwp4j.PortType;
import net.matthiasauer.stwp4j.Scheduler;
import test.net.matthiasauer.stwp4j.TestUtils.TestUtilsExecutable;

public class SchedulerTest {
    private Collection<ChannelPortsRequest<?>> testChannelRequests;
    
    @Before
    public void setUp() throws Exception {
        this.testChannelRequests =
                Arrays.asList(
                        new ChannelPortsRequest<String>("foo", PortType.Output, String.class));
    }
    
    @Test
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
        
        TestUtils.expectInterruptedExceptionToContain(
                new TestUtilsExecutable() {
                    public void execute() {
                        scheduler.addProcess(lightweightProcess);
                    }
                },
                "process already added to the scheduler");
    }
    
    private LightweightProcess createTestProcess(AtomicReference<String> data, final int upTo) {
        return
                new LightweightProcess(testChannelRequests) {
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
    
    private LightweightProcess createProducer(final String channelName, final int elementsToProduceInIteration) {
        return new LightweightProcess(
                Arrays.asList(new ChannelPortsRequest<String>(channelName, PortType.Output, String.class))) {
            int current = 0;
            ChannelOutPort<String> outPort;
            
            @Override
            public void initialize(ChannelPortsCreated createdChannelPorts) {
                this.outPort = createdChannelPorts.getChannelOutPort(channelName, String.class);
            }
            
            @Override
            public ExecutionState execute() {
                this.current++;
                
                this.outPort.offer(this.current + "");
                
                if (this.current < elementsToProduceInIteration) {
                    return ExecutionState.Waiting;
                } else {
                    return ExecutionState.Finished;
                }
            }
        };
    }
    
    private void expectedChannelIn(ChannelInPort<String> inPort, String ... expected) {
        for (String element : expected) {
            assertEquals("output not as expected", element, inPort.poll());
        }
        assertEquals("after expected we expect to get null", null, inPort.poll());
    }

    @Test
    public void testChannelsForwardOnlyAfterEachTaskHasBeenCalled() {
        final String channelName = "foo foo fo :)";
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(this.createProducer(channelName, 5));
        scheduler.addProcess(this.createProducer(channelName, 5));
        scheduler.addProcess(this.createProducer(channelName, 4));
        scheduler.addProcess(
                new LightweightProcess(
                        Arrays.asList(
                                new ChannelPortsRequest<String>(
                                        channelName,
                                        PortType.InputExclusive,
                                        String.class)
                                )) {
                    ChannelInPort<String> inPort;
                    int counter = 0;
                    
                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        this.inPort = createdChannelPorts.getChannelInPort(channelName, String.class);
                    }
                    
                    @Override
                    public ExecutionState execute() {
                        this.counter++;
                        switch (counter) {
                        case 1:
                            expectedChannelIn(inPort);
                            break;
                        case 2:
                            expectedChannelIn(inPort, "1", "1", "1");
                            break;
                        case 3:
                            expectedChannelIn(inPort, "2", "2", "2");
                            break;
                        case 4:
                            expectedChannelIn(inPort, "3", "3", "3");
                            break;
                        case 5:
                            expectedChannelIn(inPort, "4", "4", "4");
                            break;
                        case 6:
                            expectedChannelIn(inPort, "5", "5");
                            break;
                        case 7:
                            expectedChannelIn(inPort);
                            break;
                        default:
                            return ExecutionState.Finished;
                        }
                        
                        return ExecutionState.Waiting;
                    }
                });
        
        scheduler.performIteration();
    }

    @Test
    public void testSchedulerExecutesUntilInStateFinishedIntroduceWorking() {
        final AtomicReference<String> data = new AtomicReference<String>("");
        Scheduler scheduler = new Scheduler();
        // this thread is used to keep the whole thing alive for 4 sub iterations
        scheduler.addProcess(
                new LightweightProcess(testChannelRequests) {
                    int counter = 0;
                    
                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                    }
                    
                    @Override
                    public ExecutionState execute() {
                        this.counter++;
                        
                        if (this.counter <= 4) {
                            return ExecutionState.Working;
                        } else {
                            return ExecutionState.Finished;
                        }
                    }
                });
        scheduler.addProcess(
                new LightweightProcess(testChannelRequests) {
                    int counter = 0;
                    
                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                    }
                    
                    @Override
                    public ExecutionState execute() {
                        this.counter++;
                        data.set(data.get()+this.counter);
                        
                        switch (this.counter) {
                        case 1:
                            return ExecutionState.Waiting;
                        case 2:
                            return ExecutionState.Working;
                        case 3:
                            return ExecutionState.Finished;
                        default:
                            break;
                        }
                        
                        return null;
                    }
                });

        scheduler.performIteration();
        assertEquals(
                "execution states behavior regarding performIteration() is NOT correct !",
                "123",
                data.get());
    }

    @Test
    public void testSchedulerExecutesExitsPerformIterationIfAllRemainingProcessesAreWaiting() {
        final AtomicReference<String> data = new AtomicReference<String>("");
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(
                new LightweightProcess(testChannelRequests) {
                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                    }
                    
                    @Override
                    public ExecutionState execute() {
                        return ExecutionState.Waiting;
                    }
                });
        
        scheduler.addProcess(
                new LightweightProcess(testChannelRequests) {
                    int counter = 0;
                    
                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                    }
                    
                    @Override
                    public ExecutionState execute() {
                        this.counter++;
                        data.set(data.get()+this.counter);
                        
                        switch (this.counter) {
                        case 1:
                            return ExecutionState.Working;
                        case 2:
                            return ExecutionState.Waiting;
                        case 3:
                            return ExecutionState.Finished;
                        default:
                            break;
                        }
                        
                        return null;
                    }
                });

        scheduler.performIteration();
        assertEquals(
                "after all processes are in waiting we don't want to continue !",
                "12",
                data.get());
    }
}
