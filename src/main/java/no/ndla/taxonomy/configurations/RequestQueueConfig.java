package no.ndla.taxonomy.configurations;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "requestqueue")
public class RequestQueueConfig {

    private boolean enabled;
    private String targetHost;
    private long waitTimeBetweenRetries;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public long getWaitTimeBetweenRetries() {
        return waitTimeBetweenRetries;
    }

    public void setWaitTimeBetweenRetries(long waitTimeBetweenRetries) {
        this.waitTimeBetweenRetries = waitTimeBetweenRetries;
    }
}
