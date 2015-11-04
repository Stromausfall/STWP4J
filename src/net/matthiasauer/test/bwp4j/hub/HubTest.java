package net.matthiasauer.test.bwp4j.hub;

import org.junit.Test;

import static org.junit.Assert.*;

import net.matthiasauer.bwp4j.hub.Credentials;
import net.matthiasauer.bwp4j.hub.Hub;
import net.matthiasauer.bwp4j.hub.HubImplementation;

public class HubTest {
    private static final String credentialName1 = "foo credentials #1";
    private static final String credentialName2 = "foo credentials #2";
    
    @Test
    public void testCreateCredentialsAreNotTheSame() {
        Hub instance = new HubImplementation();

        Credentials instance1 = instance.createCredentials(credentialName1);
        Credentials instance2 = instance.createCredentials(credentialName2);
        
        assertNotEquals(instance1, instance2);
    }
    
    @Test(expected = IllegalArgumentException.class) 
    public void testCreateCredentialsCantUseSameCredentialName() {
        Hub instance = new HubImplementation();

        instance.createCredentials(credentialName1);
        instance.createCredentials(credentialName1);
    }
    
    @Test(expected = IllegalArgumentException.class) 
    public void testCreateCredentialsNoIdSupplied() {
        Hub instance = new HubImplementation();

        instance.createCredentials(null);
    }
    
    @Test(expected = IllegalArgumentException.class) 
    public void testCreateCredentialsEmptyIdSupplied() {
        Hub instance = new HubImplementation();

        instance.createCredentials("");
    }
     
    @Test
    public void testCreateCredentialsInvalidateCredentialsAllowsToCreateThemAgain() {
        Hub instance = new HubImplementation();

        Credentials credentialsInstance =
                instance.createCredentials(credentialName1);
        instance.invalidateCredentials(credentialsInstance);
        instance.createCredentials(credentialName1);
    }
}
