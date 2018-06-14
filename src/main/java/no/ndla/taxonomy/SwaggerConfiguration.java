package no.ndla.taxonomy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.URI;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    private AuthorizationScope oauthScope = new AuthorizationScope("taxonomy:all", "taxonomy:all");

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("no.ndla.taxonomy.rest.v1"))
                .paths(PathSelectors.regex("/v1/.*"))
                .build()
                .pathMapping("/")
                .apiInfo(apiInfo())
                .securitySchemes(newArrayList(oauth()))
                .securityContexts(newArrayList(securityContext()))
                .directModelSubstitute(URI.class, String.class)
                .useDefaultResponseMessages(false)
                .produces(newHashSet(APPLICATION_JSON_UTF8.toString()))
                .consumes(newHashSet(APPLICATION_JSON_UTF8.toString()))
                .globalResponseMessage(RequestMethod.GET, newArrayList(
                        new ResponseMessageBuilder()
                                .code(HttpStatus.OK.value())
                                .message(HttpStatus.OK.getReasonPhrase())
                                .build()
                ))
                .globalResponseMessage(RequestMethod.PUT, newArrayList(
                        new ResponseMessageBuilder()
                                .code(NO_CONTENT.value())
                                .message(NO_CONTENT.getReasonPhrase())
                                .build(),
                        new ResponseMessageBuilder()
                                .code(NOT_FOUND.value())
                                .message(NOT_FOUND.getReasonPhrase())
                                .build()
                ))
                .globalResponseMessage(DELETE, newArrayList(
                        new ResponseMessageBuilder()
                                .code(NO_CONTENT.value())
                                .message(NO_CONTENT.getReasonPhrase())
                                .build(),
                        new ResponseMessageBuilder()
                                .code(NOT_FOUND.value())
                                .message(NOT_FOUND.getReasonPhrase())
                                .build()
                ))
                .globalResponseMessage(RequestMethod.POST, newArrayList(
                        new ResponseMessageBuilder()
                                .code(HttpStatus.CREATED.value())
                                .message(CREATED.getReasonPhrase())
                                .headersWithDescription(
                                        singletonMap(LOCATION.toString(), new Header("Location", "Path to created resource", new ModelRef("URI")))
                                )
                                .build(),
                        new ResponseMessageBuilder()
                                .code(HttpStatus.CONFLICT.value())
                                .message(CONFLICT.getReasonPhrase())
                                .build()
                ))
                ;
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(newArrayList(new SecurityReference("oauth2", new AuthorizationScope[]{oauthScope})))
                .forPaths(s -> s.startsWith("/v1/"))
                .build();
    }

    private SecurityScheme oauth() {
        return new OAuth("oauth2", newArrayList(oauthScope), newArrayList(new ClientCredentialsGrant("/auth/tokens"))) {
            @Override
            public String getType() {
                return super.getType();
            }
        };
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "NDLA Taxonomy API",
                "Rest service and graph database for organizing content." +
                        "\n\n" +
                        "Unless otherwise specified, all PUT and POST requests must use Content-Type: application/json;charset=UTF-8. If charset is omitted, UTF-8 will be assumed. All GET requests will return data using the same content type." +
                        "\n\n" +
                        "When you remove an entity, its associations are also deleted. E.g., if you remove a subject, its associations to any topics are removed. The topics themselves are not affected.",
                "v1",
                null,
                null,
                "GPL 3.0",
                "https://www.gnu.org/licenses/gpl-3.0.en.html",
                emptyList()
        );

        return apiInfo;
    }

}
