package net.matthiasauer.test.bwp4j.hub;

import static org.junit.Assert.*;

import org.junit.Test;

import net.matthiasauer.bwp4j.hub.Credentials;
import net.matthiasauer.bwp4j.hub.Hub;
import net.matthiasauer.bwp4j.hub.HubImplementation;

public class CredentialsTest {
    private static final String credentialName1 = "foo credentials #1";

    @Test
    public void testGetId() {
        Hub instance = new HubImplementation();
        Credentials credentials =
                instance.createCredentials(credentialName1);
        
        assertSame(credentials.getId(), credentialName1);
    }
}
