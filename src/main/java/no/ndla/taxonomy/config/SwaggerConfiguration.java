/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
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
    public OpenAPI API() {
        return new OpenAPI().components(components()).info(new Info().title("NDLA Taxonomy API")
                .description("Rest service and graph database for organizing content.\n\n"
                        + "When you remove an entity, its associations are also deleted. E.g., if you remove a subject, its associations to any topics are removed. The topics themselves are not affected.")
                .version("v1").contact(new Contact().name(contactName).email(contactEmail).url(contactUrl))
                .termsOfService(termsUrl)
                .license(new License().name("GPL 3.0").url("https://www.gnu.org/licenses/gpl-3.0.en.html")));
    }

    private Components components() {
        SecurityScheme implicit = new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().implicit(new OAuthFlow().authorizationUrl(issuer + "authorize")));
        Parameter header = new HeaderParameter().in("header").schema(new StringSchema()).name("versionHash");
        Header version = new Header().description("versionHash").schema(new StringSchema());
        return new Components().securitySchemes(Map.of("oauth", implicit)).addParameters("versionHash", header)
                .addHeaders("versionHash", version);
    }

    @Bean
    public OpenApiCustomiser headerParameterOpenAPICustomiser() {
        return openApi -> openApi.getPaths().values().stream().flatMap(pathItem -> pathItem.readOperations().stream())
                .forEach(operation -> operation
                        .addParametersItem(new HeaderParameter().$ref("#/components/parameters/versionHash")));
    }

}
