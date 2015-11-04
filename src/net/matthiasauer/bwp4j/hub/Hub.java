package net.matthiasauer.bwp4j.hub;

public interface Hub {
    /**
     * Creates credentials - they have to be used when interacting with the hub
     * @param credentialName identifies for whom the credentials are created
     * @return the created instance
     */
    Credentials createCredentials(String credentialName);
    
    /**
     * Invalidates the credentials
     * @param credentials the credentials to invalidate
     */
    void invalidateCredentials(Credentials credentials);
}
