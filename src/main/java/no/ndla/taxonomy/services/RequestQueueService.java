package no.ndla.taxonomy.services;


import no.ndla.taxonomy.configurations.RequestQueueConfig;
import no.ndla.taxonomy.domain.RequestQueueStatus;
import no.ndla.taxonomy.domain.TaxonomyApiRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


@Service
@Profile("queue-requests")
public class RequestQueueService {


    private static final Logger LOGGER = LoggerFactory.getLogger(RequestQueueService.class);
    private long waitTimeBetweenRetries;
    private boolean autoEnqueueingRunning;
    private final BlockingQueue<TaxonomyApiRequest> requestQueue;
    private TaxonomyApiRequest currentRequest = null;
    private TaxonomyApiRequestPoster requestPoster;
    private int currentAttemptCount = 0;

    public RequestQueueService(RequestQueueConfig config, TaxonomyApiRequestPoster requestPoster) {
        this.requestPoster = requestPoster;
        LOGGER.info("Sync server: "+config.getTargetHost());
        waitTimeBetweenRetries = config.getWaitTimeBetweenRetries();
        requestQueue = new LinkedBlockingQueue<>();
        startAutomaticEnqueuing();
    }


    public void add(TaxonomyApiRequest request) {
        LOGGER.info("Adding taxonomy API request to local queue {}" + request.toString());
        requestQueue.add(request);
    }

    public RequestQueueStatus getStatus() {
        return new RequestQueueStatus(currentRequest, currentAttemptCount, requestQueue.size());
    }


    private void startAutomaticEnqueuing() {
        autoEnqueueingRunning = true;
        new Thread(() -> {
            while (autoEnqueueingRunning) {
                try {
                    if (currentRequest == null) {
                        currentRequest = requestQueue.take();
                    }
                    LOGGER.info("Attempting to enqueue request in sync queue: " + currentRequest.toString() + " (" + requestQueue.size() + " items remaining in local queue");
                    try {
                        ++currentAttemptCount;
                        ResponseEntity<String> response = requestPoster.postTaxonomyRequestToSync(currentRequest);
                        if (response.getStatusCode().is2xxSuccessful()) {
                            LOGGER.info("Sync queue insert success after " + currentAttemptCount + " attempts");
                            currentRequest = null;
                            currentAttemptCount = 0;
                        } else {
                            LOGGER.error("Received non-success HTTP code ({}) when posting a Taxonomy API Request to sync, will retry in {} seconds", response.getStatusCode().value(), waitTimeBetweenRetries / 1000);
                            Thread.sleep(waitTimeBetweenRetries);
                        }
                    } catch (Exception e) {
                        LOGGER.error("An error occurred when posting a Taxonomy API Request to sync, will retry in {} seconds", waitTimeBetweenRetries / 1000, e);
                        Thread.sleep(waitTimeBetweenRetries);
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn("Thread was interrupted, retrying", e);
                }
            }
        }).start();
    }


    @PreDestroy
    private void preDestroy() {
        autoEnqueueingRunning = false;
    }
}
