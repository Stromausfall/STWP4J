package net.matthiasauer.stwp4j;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import net.matthiasauer.stwp4j.ChannelInPort;
import net.matthiasauer.stwp4j.ChannelOutPort;
import net.matthiasauer.stwp4j.ChannelPortsCreated;
import net.matthiasauer.stwp4j.ChannelPortsRequest;
import net.matthiasauer.stwp4j.Guard;
import net.matthiasauer.stwp4j.LightweightProcess;
import net.matthiasauer.stwp4j.PortType;
import net.matthiasauer.stwp4j.Scheduler;

public class GuardTest {
    
    @Test
    public void testGuardConstructorEmpty() {
        boolean exceptionThrown = false;
        
        try {
            new Guard();
        } catch(IllegalArgumentException e) {
            assertTrue(
                    "thrown exception is not correct, was : " + e.getMessage(),
                    e.getMessage().contains("passed inPorts can't be empty !"));
            exceptionThrown = true;
        }
        
        assertTrue(
                "no exception thrown !",
                exceptionThrown);
    }
    
    @Test
    public void testGuardConstructorEmptyList() {
        boolean exceptionThrown = false;
        
        try {
            new Guard(new LinkedList<ChannelInPort<?>>());
        } catch(IllegalArgumentException e) {
            assertTrue(
                    "thrown exception is not correct, was : " + e.getMessage(),
                    e.getMessage().contains("passed inPorts can't be empty !"));
            exceptionThrown = true;
        }
        
        assertTrue(
                "no exception thrown !",
                exceptionThrown);
    }

    @Test
    public void testGuardReturnsIfAllAreAvailable() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> request1 =
                new ArrayList<ChannelPortsRequest<?>>();
        request1.add(new ChannelPortsRequest<String>("id#1", PortType.OutputExclusive, String.class));
        request1.add(new ChannelPortsRequest<Integer>("id#2", PortType.OutputExclusive, Integer.class));
        request1.add(new ChannelPortsRequest<Double>("id#3", PortType.OutputExclusive, Double.class));
        Collection<ChannelPortsRequest<?>> request2 =
                new ArrayList<ChannelPortsRequest<?>>();
        request2.add(new ChannelPortsRequest<String>("id#1", PortType.InputExclusive, String.class));
        request2.add(new ChannelPortsRequest<Integer>("id#2", PortType.InputExclusive, Integer.class));
        request2.add(new ChannelPortsRequest<Double>("id#3", PortType.InputExclusive, Double.class));
        
        // the producer
        scheduler.addProcess(
                new LightweightProcess(request1) {
                    int counter = 0;
                    ChannelOutPort<String> channel1;
                    ChannelOutPort<Integer> channel2;
                    ChannelOutPort<Double> channel3;
                    
                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        this.channel1 = createdChannelPorts.getChannelOutPort("id#1", String.class);
                        this.channel2 = createdChannelPorts.getChannelOutPort("id#2", Integer.class);
                        this.channel3 = createdChannelPorts.getChannelOutPort("id#3", Double.class);
                    }
                    
                    @Override
                    public void execute() {
                        this.counter++;
                        
                        switch (this.counter) {
                        case 1:
                            this.channel1.offer("oi!");
                            break;
                        case 2:
                            this.channel2.offer((Integer)1);
                            break;
                        case 3:
                            this.channel3.offer((Double)2.5);
                            break;
                        default:
                        }
                    }
                });
        
        // the consumer using guard
        scheduler.addProcess(
                new LightweightProcess(request2) {
                    int counter = 0;
                    Guard guard;
                    ChannelInPort<String> channel1;
                    ChannelInPort<Integer> channel2;
                    ChannelInPort<Double> channel3;
                    
                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        this.channel1 = createdChannelPorts.getChannelInPort("id#1", String.class);
                        this.channel2 = createdChannelPorts.getChannelInPort("id#2", Integer.class);
                        this.channel3 = createdChannelPorts.getChannelInPort("id#3", Double.class);
                        this.guard = new Guard(this.channel1, this.channel2, this.channel3);
                    }
                    
                    @Override
                    public void execute() {
                        this.counter++;

                        List<Object> result = this.guard.poll();
                        
                        switch (this.counter) {
                        case 1:
                        case 2:
                        case 3:
                        case 5:
                            assertEquals(
                                    "the guard can't return a result now !",
                                    null,
                                    result);
                            break;
                        case 4:
                            assertNotEquals(
                                    "the guard should return a result now !",
                                    null,
                                    result);
                            assertEquals(
                                    "there should be three objects here",
                                    3,
                                    result.size());
                            assertEquals(
                                    "didn't receive the right object at the right position",
                                    "oi!",
                                    result.get(0));
                            assertEquals(
                                    "didn't receive the right object at the right position",
                                    (Integer)1,
                                    result.get(1));
                            assertEquals(
                                    "didn't receive the right object at the right position",
                                    (Double)2.5,
                                    result.get(2));
                            break;
                        default:
                        }
                    }
                });
        
        scheduler.performIteration();
    }
}
