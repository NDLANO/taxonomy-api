package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.UrlCacher;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;


@Component
public class UrlCacherFilter implements Filter {

    private UrlCacher urlCacher;
    String batch = "batch";

    public UrlCacherFilter(UrlCacher urlCacher) {
        this.urlCacher = urlCacher;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        filterChain.doFilter(servletRequest, servletResponse);

        System.out.println(request.getMethod() + ": " + request.getRequestURI() + " : result: " + response.getStatus());
        if (request.getMethod().equals(HttpMethod.GET.toString())) return;
        if (response.getStatus() < 200 || response.getStatus() >= 300) return;

        URI id = null;
        URI lastPathElement = getIdFromPath(request.getRequestURI());

        if (response.getStatus() == HttpServletResponse.SC_CREATED) {
            id = getIdFromPath(response.getHeader("Location"));
        } else if (lastPathElement != null) {
            id = lastPathElement;
        }

        if (id == null) return;
        if (request.getMethod().equals(HttpMethod.DELETE.toString())) {
            urlCacher.remove(id);
        } else {
            String doBatch = request.getHeader(batch);
            if (doBatch == null) {
                urlCacher.add(id);
            } else if (!doBatch.equals("1")) {
                    urlCacher.add(id);
            }
        }
    }

    private URI getIdFromPath(String path) {
        String lastElement = path.substring(path.lastIndexOf('/') + 1);
        if (lastElement.startsWith("urn")) return URI.create(lastElement);
        return null;
    }

    @Override
    public void destroy() {

    }
}
