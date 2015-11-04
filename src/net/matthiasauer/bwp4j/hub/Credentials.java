package net.matthiasauer.bwp4j.hub;

/**
 * Used to identify the caller of methods
 */
public final class Credentials {
    private final String id;
    
    Credentials(String id) {
        this.id = id;
    }
    
    /**
     * @return the identifier of the Credentials instance
     */
    public String getId() {
        return this.id;
    }
}
