package no.ndla.taxonomy.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("no.ndla.taxonomy.rest.v1"))
                .paths(PathSelectors.regex("/v1/.*"))
                .build()
                .pathMapping("/")
                .apiInfo(apiInfo())
                .directModelSubstitute(URI.class, String.class)
                .directModelSubstitute(URI[].class, String[].class)
                .securitySchemes(Collections.singletonList(apiKey()))
                .securityContexts(Collections.singletonList(securityContext()))
                .useDefaultResponseMessages(true)
                .produces(newHashSet(APPLICATION_JSON_UTF8.toString()))
                .consumes(newHashSet(APPLICATION_JSON_UTF8.toString()));

    }

    private ApiKey apiKey() {
        return new ApiKey("apiKey", "Authorization", "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.regex("/v1/.*"))
                .build();
    }


    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Arrays.asList(new SecurityReference("apiKey", authorizationScopes));
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "NDLA Taxonomy API",
                "Rest service and graph database for organizing content.\n\n" +
                        "Unless otherwise specified, all PUT and POST requests must use Content-Type: application/json;charset=UTF-8. If charset is omitted, UTF-8 will be assumed. All GET requests will return data using the same content type.\n\n" +
                        "When you remove an entity, its associations are also deleted. E.g., if you remove a subject, its associations to any topics are removed. The topics themselves are not affected.\n\n" +
                        "If you are using Swagger in an environment that requires authentication you will need a valid JWT token to PUT/POST/DELETE. Apply this by typing 'Bearer [YOUR TOKEN]' in the 'Authorize' dialog",
                "v1",
                null,
                null,
                "GPL 3.0",
                "https://www.gnu.org/licenses/gpl-3.0.en.html",
                emptyList()
        );
    }

}
