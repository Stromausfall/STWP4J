package net.matthiasauer.bwp4j.hub;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The standard implementation of the Hub interface
 */
public final class HubImplementation implements Hub {
    private static final Logger logger = LoggerFactory.getLogger(HubImplementation.class);
    private static final AtomicInteger idCounter = new AtomicInteger(0);
    private final Map<String, Credentials> credentialsStorage;
    private final String id;
    
    public HubImplementation() {
        this.id = "#" + idCounter.incrementAndGet() + " - ";
        this.credentialsStorage = new HashMap<String, Credentials>();
    }

    @Override
    public synchronized Credentials createCredentials(String credentialsId) {
        if (credentialsId == null) {
            throw new IllegalArgumentException("credentialsId was null");
        }
        
        if (credentialsId.isEmpty()) {
            throw new IllegalArgumentException("credentialsId was empty");
        }
        
        if (credentialsStorage.containsKey(credentialsId)) {
            throw new IllegalArgumentException(
                    "a valid credentials instance with this id already exists");
        }
        
        Credentials createdCredentials = new Credentials(credentialsId);
        
        this.credentialsStorage.put(credentialsId, createdCredentials);
        
        logger.debug(this.id + "Created credentials with id : " + credentialsId);
        
        return createdCredentials;
    }

    @Override
    public synchronized void invalidateCredentials(Credentials credentials) {
        final String credentialsId = credentials.getId(); 
        
        this.credentialsStorage.remove(credentialsId);
        
        logger.debug(this.id + "Invalidated credentials with id : " + credentialsId);
    }
    
}
