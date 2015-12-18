package net.matthiasauer.stwp4j;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import net.matthiasauer.stwp4j.ChannelInPort;
import net.matthiasauer.stwp4j.ChannelOutPort;
import net.matthiasauer.stwp4j.ChannelPortsCreated;
import net.matthiasauer.stwp4j.ChannelPortsRequest;
import net.matthiasauer.stwp4j.LightweightProcess;
import net.matthiasauer.stwp4j.PortType;
import net.matthiasauer.stwp4j.Scheduler;

public class SchedulerChannelTest {
    private void addDummyLightWeightProcessToScheduler(
            Scheduler scheduler,
            Collection<ChannelPortsRequest<?>> customChannelRequests) {
        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public void execute(SubIterationRequest request) {
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                    }
                });
    }

    @Test(expected = IllegalStateException.class)
    public void testIncorrectOutputSharedAndMultiplex() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests1=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.Output, String.class));
        Collection<ChannelPortsRequest<?>> customChannelRequests2=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.InputMultiplex, String.class));
        Collection<ChannelPortsRequest<?>> customChannelRequests3=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.InputShared, String.class));
        
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests1);
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests2);
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests3);
        
        scheduler.performIteration();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncorrectOutputSharedAndExclusive() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests1=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.Output, String.class));
        Collection<ChannelPortsRequest<?>> customChannelRequests2=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.InputExclusive, String.class));
        Collection<ChannelPortsRequest<?>> customChannelRequests3=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.InputShared, String.class));

        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests1);
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests2);
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests3);
        
        scheduler.performIteration();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncorrectInputIncludingExclusive() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests1=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.Output, String.class));
        Collection<ChannelPortsRequest<?>> customChannelRequests2=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.OutputExclusive, String.class));
        Collection<ChannelPortsRequest<?>> customChannelRequests3=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.InputShared, String.class));

        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests1);
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests2);
        this.addDummyLightWeightProcessToScheduler(scheduler, customChannelRequests3);
        
        scheduler.performIteration();
    }
    
    private final String testMessage = "Hello World !";
    
    private void addDummyLightWeightSendProcessToScheduler(
            final String channelId,
            Scheduler scheduler,
            PortType outPortType) {
        Collection<ChannelPortsRequest<?>> customChannelRequests =
                TestUtils.asList(
                        new ChannelPortsRequest<String>(channelId, outPortType, String.class));
        
        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    ChannelOutPort<String> outport;
                    
                    @Override
                    public void execute(SubIterationRequest request) {
                        outport.offer(testMessage);
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        this.outport = createdChannelPorts.getChannelOutPort(channelId, String.class);
                    }
                });
    }
    
    private void addDummyLightWeightReceiveProcessToScheduler(
            final String channelId,
            Scheduler scheduler,
            PortType inPortType,
            final AtomicReference<String> output,
            final int messagesToExpect,
            final int maxCycles) {
        Collection<ChannelPortsRequest<?>> customChannelRequests =
                TestUtils.asList(
                        new ChannelPortsRequest<String>(channelId, inPortType, String.class));
        
        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    ChannelInPort<String> inport;
                    int receivedMessages = 0;
                    int currentCycles = 0;
                    
                    @Override
                    public void execute(SubIterationRequest request) {
                        String result = inport.poll();
                        this.currentCycles++;
                        
                        if (result != null) {
                            output.set(output.get() + result);
                            this.receivedMessages++;
                        }

                        if ((this.receivedMessages < messagesToExpect)
                                && (this.currentCycles < maxCycles)) {
                            request.forceTrigger();
                        }
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        this.inport = createdChannelPorts.getChannelInPort(channelId, String.class);
                    }
                });
    }
    
    private void addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(
            final String channelId,
            Scheduler scheduler,
            PortType inPortType,
            final AtomicReference<String> output,
            final int maxCycles) {
        Collection<ChannelPortsRequest<?>> customChannelRequests =
                TestUtils.asList(
                        new ChannelPortsRequest<String>(channelId, inPortType, String.class));
        
        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    ChannelInPort<String> inport;
                    int cycles = 0;
                    
                    @Override
                    public void execute(SubIterationRequest request) {
                        String result = inport.poll();
                        this.cycles++;
                        
                        if (result != null) {
                            output.set(output.get() + result);
                        }
                        
                        if (this.cycles < maxCycles) {
                            request.forceTrigger();
                        }
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        this.inport = createdChannelPorts.getChannelInPort(channelId, String.class);
                    }
                });
    }

    @Test
    public void testOneToOneChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        AtomicReference<String> output = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.OutputExclusive);
        addDummyLightWeightReceiveProcessToScheduler(testChannelId, scheduler, PortType.InputExclusive, output, 1, 5);
                
        scheduler.performIteration();
        
        assertEquals(
                "received message was not corrent",
                testMessage,
                output.get());
    }

    @Test
    public void testOneToManyMultiplexChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        AtomicReference<String> output1 = new AtomicReference<String>("");
        AtomicReference<String> output2 = new AtomicReference<String>("");
        AtomicReference<String> output3 = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.OutputExclusive);
        addDummyLightWeightReceiveProcessToScheduler(testChannelId, scheduler, PortType.InputMultiplex, output1, 1, 5);
        addDummyLightWeightReceiveProcessToScheduler(testChannelId, scheduler, PortType.InputMultiplex, output2, 1, 5);
        addDummyLightWeightReceiveProcessToScheduler(testChannelId, scheduler, PortType.InputMultiplex, output3, 1, 5);
                
        scheduler.performIteration();
        
        assertEquals(
                "received message1 was not corrent",
                testMessage, output1.get());
        assertEquals(
                "received message1 was not corrent",
                testMessage, output2.get());
        assertEquals(
                "received message1 was not corrent",
                testMessage, output3.get());
    }

    @Test
    public void testOneToManySharedChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        AtomicReference<String> output1 = new AtomicReference<String>("");
        AtomicReference<String> output2 = new AtomicReference<String>("");
        AtomicReference<String> output3 = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.OutputExclusive);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(testChannelId, scheduler, PortType.InputShared, output1, 3);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(testChannelId, scheduler, PortType.InputShared, output2, 3);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(testChannelId, scheduler, PortType.InputShared, output3, 3);
                
        scheduler.performIteration();
        
        assertEquals(
                "the message was not received exactly once !",
                testMessage, output1.get() + output2.get() + output3.get());
    }

    @Test
    public void testManyToOneChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        AtomicReference<String> output = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.Output);
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.Output);
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.Output);
        addDummyLightWeightReceiveProcessToScheduler(testChannelId, scheduler, PortType.InputExclusive, output, 3, 5);
                
        scheduler.performIteration();
        
        assertEquals(
                "received message was not corrent",
                testMessage + testMessage + testMessage,
                output.get());
    }

    @Test
    public void testManyToManySharedChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        AtomicReference<String> output1 = new AtomicReference<String>("");
        AtomicReference<String> output2 = new AtomicReference<String>("");
        AtomicReference<String> output3 = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.Output);
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.Output);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(testChannelId, scheduler, PortType.InputShared, output1, 3);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(testChannelId, scheduler, PortType.InputShared, output2, 3);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(testChannelId, scheduler, PortType.InputShared, output3, 3);
                
        scheduler.performIteration();
        
        assertEquals(
                "the message was not received exactly twice !",
                testMessage + testMessage, output1.get() + output2.get() + output3.get());
    }

    @Test
    public void testManyToManyMultiplexChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        AtomicReference<String> output1 = new AtomicReference<String>("");
        AtomicReference<String> output2 = new AtomicReference<String>("");
        AtomicReference<String> output3 = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.Output);
        addDummyLightWeightSendProcessToScheduler(testChannelId, scheduler, PortType.Output);
        addDummyLightWeightReceiveProcessToScheduler(testChannelId, scheduler, PortType.InputMultiplex, output1, 2, 5);
        addDummyLightWeightReceiveProcessToScheduler(testChannelId, scheduler, PortType.InputMultiplex, output2, 2, 5);
        addDummyLightWeightReceiveProcessToScheduler(testChannelId, scheduler, PortType.InputMultiplex, output3, 2, 5);
                
        scheduler.performIteration();
        
        assertEquals(
                "received message1 was not correct",
                testMessage + testMessage, output1.get());
        assertEquals(
                "received message1 was not correct",
                testMessage + testMessage, output2.get());
        assertEquals(
                "received message1 was not corrent",
                testMessage + testMessage, output3.get());
    }
}
