package net.matthiasauer.stwp4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import net.matthiasauer.stwp4j.TestUtils.TestUtilsExecutable;

public class SchedulerTest {

    @Test
    public void testSchedulerAddProcessNoDuplicates() {
        final Scheduler scheduler = new Scheduler();
        final LightweightProcess lightweightProcess = new LightweightProcess() {
            public void execute() {
            }
        };

        scheduler.addProcess(lightweightProcess);

        TestUtils.expectInterruptedExceptionToContain(new TestUtilsExecutable() {
            public void execute() {
                scheduler.addProcess(lightweightProcess);
            }
        }, "process already added to the scheduler");
    }

    private LightweightProcess createTestProcess(final ChannelOutPort<String> channel, final AtomicReference<String> data, final int upTo) {
        return new LightweightProcess() {
            int counter = 0;

            public void execute() {
                if (counter <= upTo) {
                    data.set(data.get() + counter);
                    counter++;

                    channel.offer(data.get());
                }
            }
        };
    }

    @Test
    public void testSchedulerExecutesUntilInStateFinished() {
        final AtomicReference<String> data = new AtomicReference<String>("");
        Scheduler scheduler = new Scheduler();
        Channel<String> channel = scheduler.createMultiplexChannel("foo", String.class);
        scheduler.addProcess(this.createTestProcess(channel.createOutPort(), data, 4));
        scheduler.addProcess(this.createConsumer(channel.createInPort()));
        scheduler.performIteration();

        assertEquals("incorrect execution of a single lightweight process", "01234", data.get());
    }

    private LightweightProcess createConsumer(final ChannelInPort<String> channel) {
        return new LightweightProcess() {
            @Override
            protected void execute() {
                while (channel.poll() != null);
            }
        };
    }

    private LightweightProcess createCorruptConsumer() {
        return new LightweightProcess() {
            @Override
            protected void execute() {
                // doesn't consume !
            }
        };
    }

    @Test
    public void testSchedulerExecutesMultipleProcessesConcurrently() {
        final AtomicReference<String> data = new AtomicReference<String>("");
        Scheduler scheduler = new Scheduler();
        Channel<String> channel = scheduler.createMultiplexChannel("foo", String.class);
        scheduler.addProcess(this.createTestProcess(channel.createOutPort(), data, 4));
        scheduler.addProcess(this.createTestProcess(channel.createOutPort(), data, 5));
        scheduler.addProcess(this.createTestProcess(channel.createOutPort(), data, 3));
        scheduler.addProcess(this.createTestProcess(channel.createOutPort(), data, 4));
        scheduler.addProcess(this.createTestProcess(channel.createOutPort(), data, 2));
        scheduler.addProcess(this.createConsumer(channel.createInPort()));
        scheduler.performIteration();

        assertEquals("incorrect execution of a single lightweight process", "00000111112222233334445", data.get());
    }

    private LightweightProcess createProducer(final ChannelOutPort<String> outPort, final int elementsToProduceInIteration) {
        return new LightweightProcess() {
            int current = 0;

            @Override
            public void execute() {
                this.current++;

                if (this.current <= elementsToProduceInIteration) {
                    outPort.offer(this.current + "");
                }
            }
        };
    }

    private void expectedChannelIn(ChannelInPort<String> inPort, String... expected) {
        for (String element : expected) {
            assertEquals("output not as expected", element, inPort.poll());
        }
        assertEquals("after expected we expect to get null", null, inPort.poll());
    }

    @Test
    public void testChannelsForwardOnlyAfterEachTaskHasBeenCalled() {
        final String channelName = "foo foo fo :)";
        final Scheduler scheduler = new Scheduler();
        final Channel<String> channel = scheduler.createMultiplexChannel(channelName, String.class);
        scheduler.addProcess(this.createProducer(channel.createOutPort(), 5));
        scheduler.addProcess(this.createProducer(channel.createOutPort(), 5));
        scheduler.addProcess(this.createProducer(channel.createOutPort(), 4));
        scheduler.addProcess(new LightweightProcess() {
            final ChannelInPort<String> inPort = channel.createInPort();
            int counter = 0;

            @Override
            public void execute() {
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
                    return;
                }
            }
        });

        scheduler.performIteration();
    }

    @Test
    public void testSchedulerCallsPreAndPostIterationMethods() {
        final AtomicReference<String> data = new AtomicReference<String>("");
        final AtomicInteger preIteration = new AtomicInteger();
        final AtomicInteger postIteration = new AtomicInteger();
        Scheduler scheduler = new Scheduler();
        // this thread is used to keep the whole thing alive for 4 sub
        // iterations
        scheduler.addProcess(new LightweightProcess() {
            @Override
            protected void preIteration() {
                preIteration.incrementAndGet();
            }

            @Override
            protected void postIteration() {
                postIteration.incrementAndGet();
            }

            @Override
            protected void execute() {
                assertTrue(
                        "the pre iteration method should be called and the post iteration method should not be called by now !",
                        (preIteration.get() == 2) && (postIteration.get() == 0));
            }
        });
        scheduler.addProcess(new LightweightProcess() {
            int counter = 0;

            @Override
            protected void preIteration() {
                preIteration.incrementAndGet();
            }

            @Override
            protected void postIteration() {
                postIteration.incrementAndGet();
            }

            @Override
            protected void execute() {
                this.counter++;
                data.set(data.get() + this.counter);

                assertTrue(
                        "the pre iteration method should be called and the post iteration method should not be called by now !",
                        (preIteration.get() == 2) && (postIteration.get() == 0));
            }
        });

        assertTrue("the pre and post iteration methods should not have been called by now",
                (preIteration.get() == 0) && (postIteration.get() == 0));

        scheduler.performIteration();

        assertTrue("the pre and post iteration methods have been called by now",
                (preIteration.get() == 2) && (postIteration.get() == 2));
    }

    @Test
    public void testNoInPortCausesIllegalStateException() {
        Scheduler scheduler = new Scheduler();
        final String channelName = "foo2";
        final Channel<String> channel = scheduler.createMultiplexChannel(channelName, String.class);

        scheduler.addProcess(this.createProducer(channel.createOutPort(), 100));

        try {
            scheduler.performIteration();
        } catch (IllegalStateException exception) {
            assertEquals(
                    "message of the thrown exception is incorrect !", "channel '" + channelName
                            + "' has messages (of type " + String.class + ") to forward but no InPorts !",
                    exception.getMessage());
            return;
        }

        fail("Expected IllegalStateException not thrown !");
    }

    @Test
    public void testChannelNotEmptied() {
        Scheduler scheduler = new Scheduler();
        final String channelName = "foo";
        final Channel<String> channel = scheduler.createMultiplexChannel(channelName, String.class);

        scheduler.addProcess(this.createProducer(channel.createOutPort(), 100));
        
        // create an InPort that is not used !
        channel.createInPort();
        scheduler.addProcess(this.createCorruptConsumer());

        try {
            scheduler.performIteration();
        } catch (IllegalStateException exception) {
            assertEquals(
                    "message of the thrown exception is incorrect !", "channel '" + channelName
                            + "' (of type " + String.class + ") was NOT empty at the end of the subIteration - each channel has to be always emptied !",
                    exception.getMessage());
            return;
        }

        fail("Expected IllegalStateException not thrown !");
    }
}
