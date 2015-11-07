package test.net.matthiasauer.stwp4j;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import net.matthiasauer.stwp4j.ChannelType;
import net.matthiasauer.stwp4j.ExecutionState;
import net.matthiasauer.stwp4j.InChannel;
import net.matthiasauer.stwp4j.LightweightProcess;
import net.matthiasauer.stwp4j.OutChannel;
import net.matthiasauer.stwp4j.Scheduler;
import net.matthiasauer.stwp4j.utils.Pair;

public class SchedulerTest {
    private Collection<Pair<String, ChannelType>> testChannelRequests;
    
    @Before
    public void setUp() throws Exception {
        this.testChannelRequests =
                Arrays.asList(
                        new Pair<String, ChannelType>("foo", ChannelType.Input));
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
                    public void initialize(Collection<Pair<String, InChannel>> inputChannels,
                            Collection<Pair<String, OutChannel>> outputChannels) {
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
                            return ExecutionState.Working;
                        } else {
                            return ExecutionState.Finished;
                        }
                    }

                    @Override
                    public void initialize(Collection<Pair<String, InChannel>> inputChannels,
                            Collection<Pair<String, OutChannel>> outputChannels) {
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
                    public void initialize(Collection<Pair<String, InChannel>> inputChannels,
                            Collection<Pair<String, OutChannel>> outputChannels) {
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
                    public void initialize(Collection<Pair<String, InChannel>> inputChannels,
                            Collection<Pair<String, OutChannel>> outputChannels) {
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
        Collection<Pair<String, ChannelType>> customChannelRequests =
                Arrays.asList(
                        new Pair<String, ChannelType>("foo1", ChannelType.Input),
                        new Pair<String, ChannelType>("foo2", ChannelType.Input),
                        new Pair<String, ChannelType>("foo3", ChannelType.OutputMultiplex),
                        new Pair<String, ChannelType>("foo4", ChannelType.OutputMultiplex),
                        new Pair<String, ChannelType>("foo5", ChannelType.OutputShared));
        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return null;
                    }

                    @Override
                    public void initialize(Collection<Pair<String, InChannel>> inputChannels,
                            Collection<Pair<String, OutChannel>> outputChannels) {
                        assertEquals(
                                "number of input input channels not correct",
                                2,
                                inputChannels.size());
                        assertEquals(
                                "number of output input channels not correct",
                                3,
                                outputChannels.size());
                    }
                });
    }
    
    private void addDummyLightWeightProcessToScheduler(
            Scheduler scheduler,
            Collection<Pair<String, ChannelType>> customChannelRequests) {
        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return ExecutionState.Finished;
                    }

                    @Override
                    public void initialize(Collection<Pair<String, InChannel>> inputChannels,
                            Collection<Pair<String, OutChannel>> outputChannels) {
                    }
                });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPerformIterationChecksThatChannelConnectionsAreValid1() {
        Scheduler scheduler = new Scheduler();
        Collection<Pair<String, ChannelType>> customChannelRequests1=
                Arrays.asList(
                        new Pair<String, ChannelType>("foo1", ChannelType.Input));
        Collection<Pair<String, ChannelType>> customChannelRequests2=
                Arrays.asList(
                        new Pair<String, ChannelType>("foo1", ChannelType.OutputMultiplex));
        Collection<Pair<String, ChannelType>> customChannelRequests3=
                Arrays.asList(
                        new Pair<String, ChannelType>("foo1", ChannelType.OutputShared));
        
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests1);
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests2);
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests3);
        
        scheduler.performIteration();
    }
}
