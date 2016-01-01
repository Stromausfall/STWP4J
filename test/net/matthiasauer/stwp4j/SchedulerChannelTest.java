package net.matthiasauer.stwp4j;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class SchedulerChannelTest {
    private final String testMessage = "Hello World !";

    private void addDummyLightWeightSendProcessToScheduler(final Channel<String> channel, Scheduler scheduler) {
        scheduler.addProcess(new LightweightProcess() {
            final ChannelOutPort<String> outport = channel.createOutPort();
            boolean executeOnce = false;

            @Override
            protected void preIteration() {
                executeOnce = true;
            }

            @Override
            public void execute() {
                if (executeOnce) {
                    outport.offer(testMessage);
                }

                executeOnce = false;
            }
        });
    }

    private void addDummyLightWeightReceiveProcessToScheduler(final Channel<String> channel, Scheduler scheduler,
            final AtomicReference<String> output) {
        scheduler.addProcess(new LightweightProcess() {
            ChannelInPort<String> inport = channel.createInPort();

            @Override
            public void execute() {
                String result = null;

                // process all input
                while ((result = inport.poll()) != null) {
                    if (result != null) {
                        output.set(output.get() + result);
                    }
                }
            }
        });
    }

    private void addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(final Channel<String> channel,
            Scheduler scheduler, final AtomicReference<String> output) {
        scheduler.addProcess(new LightweightProcess() {
            ChannelInPort<String> inport = channel.createInPort();

            @Override
            public void execute() {
                String result = inport.poll();

                if (result != null) {
                    output.set(output.get() + result);
                }
            }
        });
    }

    @Test
    public void testOneToOneChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        AtomicReference<String> output = new AtomicReference<String>("");
        Channel<String> channel = scheduler.createMultiplexChannel(testChannelId, String.class, true, false);
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightReceiveProcessToScheduler(channel, scheduler, output);

        scheduler.performIteration();

        assertEquals("received message was not corrent", testMessage, output.get());
    }

    @Test
    public void testOneToManyMultiplexChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        Channel<String> channel = scheduler.createMultiplexChannel(testChannelId, String.class, true, false);
        AtomicReference<String> output1 = new AtomicReference<String>("");
        AtomicReference<String> output2 = new AtomicReference<String>("");
        AtomicReference<String> output3 = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightReceiveProcessToScheduler(channel, scheduler, output1);
        addDummyLightWeightReceiveProcessToScheduler(channel, scheduler, output2);
        addDummyLightWeightReceiveProcessToScheduler(channel, scheduler, output3);

        scheduler.performIteration();

        assertEquals("received message1 was not corrent", testMessage, output1.get());
        assertEquals("received message1 was not corrent", testMessage, output2.get());
        assertEquals("received message1 was not corrent", testMessage, output3.get());
    }

    @Test
    public void testOneToManySharedChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        Channel<String> channel = scheduler.createSharedChannel(testChannelId, String.class, true, false);
        AtomicReference<String> output1 = new AtomicReference<String>("");
        AtomicReference<String> output2 = new AtomicReference<String>("");
        AtomicReference<String> output3 = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(channel, scheduler, output1);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(channel, scheduler, output2);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(channel, scheduler, output3);

        scheduler.performIteration();

        assertEquals("the message was not received exactly once !", testMessage,
                output1.get() + output2.get() + output3.get());
    }

    @Test
    public void testManyToOneChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        Channel<String> channel = scheduler.createMultiplexChannel(testChannelId, String.class, true, false);
        AtomicReference<String> output = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightReceiveProcessToScheduler(channel, scheduler, output);

        scheduler.performIteration();

        assertEquals("received message was not corrent", testMessage + testMessage + testMessage, output.get());
    }

    @Test
    public void testManyToManySharedChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        Channel<String> channel = scheduler.createSharedChannel(testChannelId, String.class, true, false);
        AtomicReference<String> output1 = new AtomicReference<String>("");
        AtomicReference<String> output2 = new AtomicReference<String>("");
        AtomicReference<String> output3 = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(channel, scheduler, output1);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(channel, scheduler, output2);
        addDummyLightWeightReceiveUntilCyclesPassedProcessToScheduler(channel, scheduler, output3);

        scheduler.performIteration();

        assertEquals("the message was not received exactly twice !", testMessage + testMessage,
                output1.get() + output2.get() + output3.get());
    }

    @Test
    public void testManyToManyMultiplexChannel() {
        final String testChannelId = "test1223";
        Scheduler scheduler = new Scheduler();
        Channel<String> channel = scheduler.createMultiplexChannel(testChannelId, String.class, true, false);
        AtomicReference<String> output1 = new AtomicReference<String>("");
        AtomicReference<String> output2 = new AtomicReference<String>("");
        AtomicReference<String> output3 = new AtomicReference<String>("");
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightSendProcessToScheduler(channel, scheduler);
        addDummyLightWeightReceiveProcessToScheduler(channel, scheduler, output1);
        addDummyLightWeightReceiveProcessToScheduler(channel, scheduler, output2);
        addDummyLightWeightReceiveProcessToScheduler(channel, scheduler, output3);

        scheduler.performIteration();

        assertEquals("received message1 was not correct", testMessage + testMessage, output1.get());
        assertEquals("received message1 was not correct", testMessage + testMessage, output2.get());
        assertEquals("received message1 was not corrent", testMessage + testMessage, output3.get());
    }
}
