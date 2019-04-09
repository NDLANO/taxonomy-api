package no.ndla.taxonomy.services;

import no.ndla.taxonomy.configurations.RequestQueueConfig;
import no.ndla.taxonomy.domain.RequestQueueStatus;
import no.ndla.taxonomy.domain.TaxonomyApiRequest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.internal.verification.VerificationModeFactory.times;


public class RequestQueueServiceTest {


    private static RequestQueueService requestQueueService;
    private RequestQueueConfig config;
    private TaxonomyApiRequestPoster requestPoster;
    private static final long WAIT_TIME = 10L;

    @Before
    public void init() {
        config = Mockito.mock(RequestQueueConfig.class);
        Mockito.when(config.getTargetHost()).thenReturn("dummyhost");
        Mockito.when(config.getWaitTimeBetweenRetries()).thenReturn(WAIT_TIME);
        requestPoster = Mockito.mock(TaxonomyApiRequestPoster.class);
        requestQueueService = new RequestQueueService(config, requestPoster);
    }

    @AfterClass
    public static void cleanUp(){
        requestQueueService.shutdown();
    }


    @Test
    public void thereIsNothingInQueueOnStartup() {
        RequestQueueStatus status = requestQueueService.getStatus();
        assertNull(status.getCurrentRequest());
        assertEquals(0, status.getQueuedItems());
        assertEquals(0, status.getCurrentAttempts());
    }

    @Test
    public void itemAddedIsAttemptedSent() throws InterruptedException {
        Mockito.when(requestPoster.postTaxonomyRequestToSync(Mockito.any(TaxonomyApiRequest.class))).thenReturn(ResponseEntity.noContent().build());
        TaxonomyApiRequest taxonomyApiRequest = new TaxonomyApiRequest("POST", "/v1/dummy", "{}", "2001-01-01T11:22:33:444");
        requestQueueService.add(taxonomyApiRequest);
        Thread.sleep(50L); //give blocking queue a chance to catch up
        Mockito.verify(requestPoster, times(1)).postTaxonomyRequestToSync(taxonomyApiRequest);
    }

    @Test
    public void failedPostingsAreRetried() throws InterruptedException {
        Mockito.when(requestPoster.postTaxonomyRequestToSync(Mockito.any(TaxonomyApiRequest.class))).thenReturn(ResponseEntity.badRequest().build());
        TaxonomyApiRequest taxonomyApiRequest = new TaxonomyApiRequest("POST", "/v1/dummy", "{}", "2001-01-01T11:22:33:444");
        requestQueueService.add(taxonomyApiRequest);
        Thread.sleep( 10 * WAIT_TIME);
        Mockito.verify(requestPoster, atLeast(5)).postTaxonomyRequestToSync(taxonomyApiRequest);
    }


}