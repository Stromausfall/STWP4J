package net.matthiasauer.stwp4j;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class SubIterationRequestTest {
    private LightweightProcess createConsumer(final AtomicInteger alive, final String ... channelIds) {
        Collection<ChannelPortsRequest<?>> request =
                new ArrayList<ChannelPortsRequest<?>>();
        
        for (String channelId : channelIds) {
            request.add(new ChannelPortsRequest<String>(channelId, PortType.InputExclusive, String.class));
        }
        
        return new LightweightProcess(request) {
            private List<ChannelInPort<String>> inPorts =
                    new LinkedList<ChannelInPort<String>>();

            @Override
            protected void initialize(ChannelPortsCreated createdChannelPorts) {
                for (String channelId : channelIds) {
                    inPorts.add(
                            createdChannelPorts.getChannelInPort(channelId, String.class));
                }
            }
            
            @Override
            protected void execute(SubIterationRequest request) {
                for (ChannelInPort<String> inPort : this.inPorts) {
                    // empty channel
                    while (inPort.poll() != null);
                    
                    // add trigger for the channel
                    request.triggerIfNotEmpty(inPort);
                }
                
                alive.incrementAndGet();
            }
        };
    }
    
    private LightweightProcess createProducer(final int numberToProduce, final String channelId) {
        Collection<ChannelPortsRequest<?>> request =
                new ArrayList<ChannelPortsRequest<?>>();
        request.add(new ChannelPortsRequest<String>(channelId, PortType.Output, String.class));
        
        return new LightweightProcess(request) {
            private ChannelOutPort<String> outport;
            int counter = 0;

            @Override
            protected void initialize(ChannelPortsCreated createdChannelPorts) {
                this.outport = createdChannelPorts.getChannelOutPort(channelId, String.class);
            }
            
            @Override
            protected void execute(SubIterationRequest request) {                
                if (counter < numberToProduce) {
                    this.outport.offer("foo");
                    
                    request.forceTrigger();
                }
                
                counter++;
            }
        };
    }

    @Test
    public void testSubIterationRequestWithEmptyChannel() {
        Scheduler scheduler = new Scheduler();
        AtomicInteger aliveCounter = new AtomicInteger(0);
        
        scheduler.addProcess(this.createProducer(0, "id#1"));
        scheduler.addProcess(this.createConsumer(aliveCounter, "id#1"));
        
        scheduler.performIteration();
        
        assertEquals(
                "the number of sub iterations alive is not correct",
                1,
                aliveCounter.get());
    }

    @Test
    public void testSubIterationRequestWithFilledChannel() {
        for (int i = 0; i < 10; i++) {
            Scheduler scheduler = new Scheduler();
            AtomicInteger aliveCounter = new AtomicInteger(0);
            
            scheduler.addProcess(this.createProducer(i, "id#1"));
            scheduler.addProcess(this.createConsumer(aliveCounter, "id#1"));
            
            scheduler.performIteration();
            
            assertEquals(
                    "the number of sub iterations (we produce " + i + ") alive is not correct",
                    i+1,
                    aliveCounter.get());
        }
    }

    @Test
    public void testSubIterationRequestWithMultipleFilledChannels() {
        for (int i = 0; i < 10; i++) {
            Scheduler scheduler = new Scheduler();
            AtomicInteger aliveCounter = new AtomicInteger(0);
            
            scheduler.addProcess(this.createProducer(i - 5, "id#1"));
            scheduler.addProcess(this.createProducer(i, "id#2"));
            scheduler.addProcess(this.createProducer(i - 3, "id#3"));
            scheduler.addProcess(this.createProducer(i - 2, "id#4"));
            scheduler.addProcess(this.createConsumer(aliveCounter, "id#1", "id#2", "id#3", "id#4"));
            
            scheduler.performIteration();
            
            assertEquals(
                    "the number of sub iterations (we produce " + i + ") alive is not correct",
                    i+1,
                    aliveCounter.get());
        }
    }

}
