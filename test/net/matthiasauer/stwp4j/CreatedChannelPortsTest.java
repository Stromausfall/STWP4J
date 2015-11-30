package net.matthiasauer.stwp4j;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import net.matthiasauer.stwp4j.ChannelPortsCreated;
import net.matthiasauer.stwp4j.ChannelPortsRequest;
import net.matthiasauer.stwp4j.ExecutionState;
import net.matthiasauer.stwp4j.LightweightProcess;
import net.matthiasauer.stwp4j.PortType;
import net.matthiasauer.stwp4j.Scheduler;

public class CreatedChannelPortsTest {
    
    @Test
    public void testGetPorts() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.Output, String.class),
                        new ChannelPortsRequest<String>("foo2", PortType.InputExclusive, String.class),
                        new ChannelPortsRequest<String>("foo3", PortType.InputMultiplex, String.class),
                        new ChannelPortsRequest<String>("foo4", PortType.InputShared, String.class),
                        new ChannelPortsRequest<String>("foo5", PortType.OutputExclusive, String.class));

        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return ExecutionState.Finished;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        Assert.assertNotNull(
                                "foo1 should have a output port",
                                createdChannelPorts.getChannelOutPort("foo1", String.class));
                        Assert.assertNotNull(
                                "foo2 should have a output port",
                                createdChannelPorts.getChannelInPort("foo2", String.class));
                        Assert.assertNotNull(
                                "foo3 should have a output port",
                                createdChannelPorts.getChannelInPort("foo3", String.class));
                        Assert.assertNotNull(
                                "foo4 should have a output port",
                                createdChannelPorts.getChannelInPort("foo4", String.class));
                        Assert.assertNotNull(
                                "foo5 should have a output port",
                                createdChannelPorts.getChannelOutPort("foo5", String.class));
                    }
                });
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetChannelOutPortIncorrectIdentifier() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.Output, String.class));

        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return ExecutionState.Finished;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        createdChannelPorts.getChannelOutPort("foo2", String.class);
                    }
                });
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetChannelInPortIncorrectIdentifier() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.InputExclusive, String.class));

        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return ExecutionState.Finished;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        createdChannelPorts.getChannelInPort("foo2", String.class);
                    }
                });
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetChannelOutPortIncorrectType() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.Output, String.class));

        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return ExecutionState.Finished;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        createdChannelPorts.getChannelOutPort("foo1", Integer.class);
                    }
                });
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetChannelInPortIncorrectType() {
        Scheduler scheduler = new Scheduler();
        Collection<ChannelPortsRequest<?>> customChannelRequests=
                TestUtils.asList(
                        new ChannelPortsRequest<String>("foo1", PortType.InputExclusive, String.class));

        scheduler.addProcess(
                new LightweightProcess(customChannelRequests) {
                    @Override
                    public ExecutionState execute() {
                        return ExecutionState.Finished;
                    }

                    @Override
                    public void initialize(ChannelPortsCreated createdChannelPorts) {
                        createdChannelPorts.getChannelInPort("foo1", Integer.class);
                    }
                });
    }
}
