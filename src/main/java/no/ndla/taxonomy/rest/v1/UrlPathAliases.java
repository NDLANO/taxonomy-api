package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.PathAlias;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.service.PathAliasService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/v1/url/alias")
public class UrlPathAliases {
    private PathAliasService pathAliasService;

    public UrlPathAliases(PathAliasService pathAliasService) {
        this.pathAliasService = pathAliasService;
    }

    @GetMapping
    public PathAlias getOrCreatePathAlias(@RequestParam String path) {
        return pathAliasService.pathAliasForPath(path)
                .orElseThrow(() -> new NotFoundHttpRequestException("Path not found or currently unable to create alias"));
    }

    @GetMapping(value = "/resolve")
    public PathAlias resolvePathAlias(@RequestParam String alias) throws PathAliasService.PathAliasReplacedException {
        return pathAliasService.resolvePath(alias)
                .orElseThrow(() -> new NotFoundHttpRequestException("Path alias not found"));
    }

    @ExceptionHandler(PathAliasService.PathAliasReplacedException.class)
    @ResponseStatus(HttpStatus.MOVED_PERMANENTLY)
    public PathAlias pathAliasReplaced(HttpServletResponse response, PathAliasService.PathAliasReplacedException e) {
        var pathAlias = e.getReplacedBy();
        if (pathAlias == null || pathAlias.getAlias() == null) {
            throw new NullPointerException("Expected non nulls for a valid replace exception");
        }
        response.setHeader("Location", pathAlias.getAlias());
        return pathAlias;
    }
}
