package no.ndla.taxonomy.services;

import no.ndla.taxonomy.configurations.RequestQueueConfig;
import no.ndla.taxonomy.domain.TaxonomyApiRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TaxonomyApiRequestPoster {

    private String syncEndpoint;
    private RestTemplate restTemplate;

    public TaxonomyApiRequestPoster(RequestQueueConfig config){
        restTemplate = new RestTemplate();
        syncEndpoint = config.getTargetHost() + "/api/requests/queue";
    }


    public ResponseEntity<String> postTaxonomyRequestToSync(TaxonomyApiRequest request) {
        return restTemplate.postForEntity(syncEndpoint, request, String.class);
    }

}
