package no.ndla.taxonomy.domain;

public class RequestQueueStatus {

    TaxonomyApiRequest currentRequest;
    long currentAttempts;
    int queuedItems;

    public RequestQueueStatus(TaxonomyApiRequest currentRequest, long currentAttempts, int queuedItems) {
        this.currentRequest = currentRequest;
        this.currentAttempts = currentAttempts;
        this.queuedItems = queuedItems;
    }

    public TaxonomyApiRequest getCurrentRequest() {
        return currentRequest;
    }

    public void setCurrentRequest(TaxonomyApiRequest currentRequest) {
        this.currentRequest = currentRequest;
    }

    public long getCurrentAttempts() {
        return currentAttempts;
    }

    public void setCurrentAttempts(long currentAttempts) {
        this.currentAttempts = currentAttempts;
    }

    public int getQueuedItems() {
        return queuedItems;
    }

    public void setQueuedItems(int queuedItems) {
        this.queuedItems = queuedItems;
    }


}
