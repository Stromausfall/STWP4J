package net.matthiasauer.stwp4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import net.matthiasauer.stwp4j.ChannelInPort;
import net.matthiasauer.stwp4j.ChannelOutPort;
import net.matthiasauer.stwp4j.ChannelPortsCreated;
import net.matthiasauer.stwp4j.ChannelPortsRequest;
import net.matthiasauer.stwp4j.LightweightProcess;
import net.matthiasauer.stwp4j.PortType;
import net.matthiasauer.stwp4j.Scheduler;
import net.matthiasauer.stwp4j.TestUtils.TestUtilsExecutable;

public class SchedulerTest {
    private Collection<ChannelPortsRequest<?>> testChannelRequests;
    private Collection<ChannelPortsRequest<?>> testConsumerChannelRequests;

    @Before
    public void setUp() throws Exception {
        this.testChannelRequests = TestUtils
                .asList(new ChannelPortsRequest<String>("foo", PortType.Output, String.class));
        this.testConsumerChannelRequests = TestUtils
                .asList(new ChannelPortsRequest<String>("foo", PortType.InputExclusive, String.class));
    }

    @Test
    public void testSchedulerAddProcessNoDuplicates() {
        final Scheduler scheduler = new Scheduler();
        final LightweightProcess lightweightProcess = new LightweightProcess(testChannelRequests) {
            public void execute() {
            }

            @Override
            public void initialize(ChannelPortsCreated createdChannelPorts) {
            }
        };

        scheduler.addProcess(lightweightProcess);

        TestUtils.expectInterruptedExceptionToContain(new TestUtilsExecutable() {
            public void execute() {
                scheduler.addProcess(lightweightProcess);
            }
        }, "process already added to the scheduler");
    }

    private LightweightProcess createTestProcess(final AtomicReference<String> data, final int upTo) {
        return new LightweightProcess(testChannelRequests) {
            int counter = 0;
            ChannelOutPort<String> channel;

            public void execute() {
                if (counter <= upTo) {
                    data.set(data.get() + counter);
                    counter++;

                    this.channel.offer(data.get());
                }
            }

            @Override
            public void initialize(ChannelPortsCreated createdChannelPorts) {
                this.channel = createdChannelPorts.getChannelOutPort("foo", String.class);
            }
        };
    }

    @Test
    public void testSchedulerExecutesUntilInStateFinished() {
        final AtomicReference<String> data = new AtomicReference<String>("");
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(this.createTestProcess(data, 4));
        scheduler.addProcess(this.createConsumer());
        scheduler.performIteration();

        assertEquals("incorrect execution of a single lightweight process", "01234", data.get());
    }

    private LightweightProcess createConsumer() {
        return new LightweightProcess(testConsumerChannelRequests) {

            private ChannelInPort<String> channel;

            @Override
            protected void initialize(ChannelPortsCreated createdChannelPorts) {
                this.channel = createdChannelPorts.getChannelInPort("foo", String.class);
            }

            @Override
            protected void execute() {
                while (channel.poll() != null)
                    ;
            }
        };
    }

    private LightweightProcess createCorruptConsumer() {
        return new LightweightProcess(testConsumerChannelRequests) {
            @Override
            protected void initialize(ChannelPortsCreated createdChannelPorts) {
            }

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
        scheduler.addProcess(this.createTestProcess(data, 4));
        scheduler.addProcess(this.createTestProcess(data, 5));
        scheduler.addProcess(this.createTestProcess(data, 3));
        scheduler.addProcess(this.createTestProcess(data, 4));
        scheduler.addProcess(this.createTestProcess(data, 2));
        scheduler.addProcess(this.createConsumer());
        scheduler.performIteration();

        assertEquals("incorrect execution of a single lightweight process", "00000111112222233334445", data.get());
    }

    @Test
    public void testAddProcessCallsInitializeMethod() {
        final AtomicBoolean initializeMethodCalled = new AtomicBoolean(false);
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(new LightweightProcess(testChannelRequests) {
            @Override
            public void execute() {
            }

            @Override
            public void initialize(ChannelPortsCreated createdChannelPorts) {
                initializeMethodCalled.set(true);
            }
        });

        assertTrue("initialize method was not called after adding process", initializeMethodCalled.get());
    }

    @Test
    public void testAddProcessCallsInitializeMethodAndChannelsAreCreated() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests = TestUtils.asList(
                new ChannelPortsRequest<String>("foo1", PortType.Output, String.class),
                new ChannelPortsRequest<String>("foo2", PortType.Output, String.class),
                new ChannelPortsRequest<String>("foo3", PortType.InputMultiplex, String.class),
                new ChannelPortsRequest<String>("foo4", PortType.InputMultiplex, String.class),
                new ChannelPortsRequest<String>("foo5", PortType.InputShared, String.class));
        scheduler.addProcess(new LightweightProcess(customChannelRequests) {
            @Override
            public void execute() {
            }

            @Override
            public void initialize(ChannelPortsCreated createdChannelPorts) {
                assertEquals("number of output channels not correct", 2,
                        createdChannelPorts.getChannelOutPorts().size());
                assertEquals("number of input channels not correct", 3, createdChannelPorts.getChannelInPorts().size());
            }
        });
    }

    private LightweightProcess createProducer(final String channelName, final int elementsToProduceInIteration) {
        return new LightweightProcess(
                TestUtils.asList(new ChannelPortsRequest<String>(channelName, PortType.Output, String.class))) {
            int current = 0;
            ChannelOutPort<String> outPort;

            @Override
            public void initialize(ChannelPortsCreated createdChannelPorts) {
                this.outPort = createdChannelPorts.getChannelOutPort(channelName, String.class);
            }

            @Override
            public void execute() {
                this.current++;

                if (this.current <= elementsToProduceInIteration) {
                    this.outPort.offer(this.current + "");
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
        Scheduler scheduler = new Scheduler();
        scheduler.addProcess(this.createProducer(channelName, 5));
        scheduler.addProcess(this.createProducer(channelName, 5));
        scheduler.addProcess(this.createProducer(channelName, 4));
        scheduler.addProcess(new LightweightProcess(
                TestUtils.asList(new ChannelPortsRequest<String>(channelName, PortType.InputExclusive, String.class))) {
            ChannelInPort<String> inPort;
            int counter = 0;

            @Override
            public void initialize(ChannelPortsCreated createdChannelPorts) {
                this.inPort = createdChannelPorts.getChannelInPort(channelName, String.class);
            }

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
        scheduler.addProcess(new LightweightProcess(testChannelRequests) {
            @Override
            protected void preIteration() {
                preIteration.incrementAndGet();
            }

            @Override
            protected void postIteration() {
                postIteration.incrementAndGet();
            }

            @Override
            protected void initialize(ChannelPortsCreated createdChannelPorts) {
            }

            @Override
            protected void execute() {
                assertTrue(
                        "the pre iteration method should be called and the post iteration method should not be called by now !",
                        (preIteration.get() == 2) && (postIteration.get() == 0));
            }
        });
        scheduler.addProcess(new LightweightProcess(testChannelRequests) {
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
            protected void initialize(ChannelPortsCreated createdChannelPorts) {
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

        scheduler.addProcess(this.createProducer("foo2", 100));

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

        scheduler.addProcess(this.createProducer(channelName, 100));
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
