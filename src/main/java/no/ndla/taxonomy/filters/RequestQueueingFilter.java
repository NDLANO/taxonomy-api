package no.ndla.taxonomy.filters;

import no.ndla.taxonomy.domain.TaxonomyApiRequest;
import no.ndla.taxonomy.services.RequestQueueService;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Order(1)
@Profile("queue-requests")
public class RequestQueueingFilter extends GenericFilterBean {

    private RequestQueueService requestQueueService;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;

    public RequestQueueingFilter(RequestQueueService requestQueueService) {
        this.requestQueueService = requestQueueService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        if (requestShouldBeEnqueued(httpServletRequest)) {
            //request must be wrapped in order to read from the input stream more than once (to get hold of the body)
            ContentCachingRequestWrapper contentCachingRequestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
            filterChain.doFilter(contentCachingRequestWrapper, servletResponse);
            enqueueRequestIfItWasSuccessful(servletResponse, contentCachingRequestWrapper);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private boolean requestShouldBeEnqueued(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getRequestURI().startsWith("/v1/") &&
                (httpServletRequest.getMethod().equals("PUT") ||
                        httpServletRequest.getMethod().equals("POST") ||
                        httpServletRequest.getMethod().equals("DELETE"));
    }

    private void enqueueRequestIfItWasSuccessful(ServletResponse servletResponse, ContentCachingRequestWrapper request) {
        int statusCode = ((HttpServletResponse) servletResponse).getStatus();
        if (statusCode >= 200 && statusCode <= 299) {
            TaxonomyApiRequest taxonomyApiRequest = createRequestDTO(request);
            requestQueueService.add(taxonomyApiRequest);
        }
    }

    private TaxonomyApiRequest createRequestDTO(ContentCachingRequestWrapper request) {
        return new TaxonomyApiRequest(request.getMethod(),
                request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
                new String(request.getContentAsByteArray(), StandardCharsets.UTF_8),
                LocalDateTime.now().format(dateFormatter));
    }

}
