/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.RequestParameterBuilder;
import springfox.documentation.schema.ScalarType;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Value(value = "${auth0.issuer}")
    private String issuer;

    @Value(value = "${contact.name:NDLA}")
    private String contactName;

    @Value(value = "${contact.email:hjelp+api@ndla.no}")
    private String contactEmail;

    @Value(value = "${contact.url:https://ndla.no}")
    private String contactUrl;

    @Value(value = "${terms.url:https://om.ndla.no/tos}")
    private String termsUrl;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("no.ndla.taxonomy.rest.v1"))
                .paths(PathSelectors.regex("/v1/.*")).build().pathMapping("/").apiInfo(apiInfo())
                .directModelSubstitute(URI.class, String.class).directModelSubstitute(URI[].class, String[].class)
                .securitySchemes(List.of(securitySchema())).securityContexts(List.of(securityContext()))
                .globalRequestParameters(List.of(new RequestParameterBuilder().name("VersionHash")
                        .description("Hash code identifying taxonomy version.").in(ParameterType.HEADER)
                        .query(q -> q.model(m -> m.scalarModel(ScalarType.STRING))).build()))
                .useDefaultResponseMessages(true).produces(newHashSet(APPLICATION_JSON.toString()))
                .consumes(newHashSet(APPLICATION_JSON.toString()));
    }

    private OAuth securitySchema() {
        return new OAuth("oauth", List.of(),
                List.of(new ImplicitGrant(new LoginEndpoint(issuer + "authorize"), "access_token")));
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth()).forPaths(PathSelectors.regex("/v1/.*"))
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        return List.of(new SecurityReference("oauth", new AuthorizationScope[0]));
    }

    private Contact contact() {
        return new Contact(contactName, contactUrl, contactEmail);
    }

    private ApiInfo apiInfo() {
        return new ApiInfo("NDLA Taxonomy API", "Rest service and graph database for organizing content.\n\n"
                + "Unless otherwise specified, all PUT and POST requests must use Content-Type: application/json;charset=UTF-8. If charset is omitted, UTF-8 will be assumed. All GET requests will return data using the same content type.\n\n"
                + "When you remove an entity, its associations are also deleted. E.g., if you remove a subject, its associations to any topics are removed. The topics themselves are not affected.",
                "v1", termsUrl, contact(), "GPL 3.0", "https://www.gnu.org/licenses/gpl-3.0.en.html", emptyList());
    }

    /*
     * Needed to make springfox work. Should rather replace with https://springdoc.org/
     */
    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
            ServletEndpointsSupplier servletEndpointsSupplier, ControllerEndpointsSupplier controllerEndpointsSupplier,
            EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
            WebEndpointProperties webEndpointProperties, Environment environment) {
        List<ExposableEndpoint<?>> allEndpoints = new ArrayList();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);
        boolean shouldRegisterLinksMapping = this.shouldRegisterLinksMapping(webEndpointProperties, environment,
                basePath);
        return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints, endpointMediaTypes,
                corsProperties.toCorsConfiguration(), new EndpointLinksResolver(allEndpoints, basePath),
                shouldRegisterLinksMapping, null);
    }

    private boolean shouldRegisterLinksMapping(WebEndpointProperties webEndpointProperties, Environment environment,
            String basePath) {
        return webEndpointProperties.getDiscovery().isEnabled() && (StringUtils.hasText(basePath)
                || ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
    }
}
