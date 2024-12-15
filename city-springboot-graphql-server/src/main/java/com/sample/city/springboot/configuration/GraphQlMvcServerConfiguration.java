package com.sample.city.springboot.configuration;

import com.sample.city.springboot.component.GraphiQlMvcHandler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import org.springframework.graphql.server.webmvc.SchemaHandler;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Collections;

/** Configuration with beans used to produce GraphQl server */
@AutoConfiguration(after = GraphQlWebMvcAutoConfiguration.class)
@EnableAutoConfiguration
@Configuration
public class GraphQlMvcServerConfiguration {

    /**
     * Custom graphQlRouterFunction bean to override (QLink supports redirects thought the cloud
     * gateway
     *
     * <p>com.citi.jedi.springboot.component.GraphiQLMvcHandler) with custom implementation, which
     *
     * @param graphQlSource GraphQl source to be used in handler
     * @param httpHandler handler for GraphQL requests
     * @param properties properties for GraphQl server
     * @return configured instance of {@link
     *     org.springframework.web.servlet.function.RouterFunction}
     */
    @Bean
    @ConditionalOnClass(org.springframework.web.servlet.function.ServerResponse.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public org.springframework.web.servlet.function.RouterFunction<
                    org.springframework.web.servlet.function.ServerResponse>
            graphQlServerRouterFunction(
                    GraphQlHttpHandler httpHandler,
                    GraphQlSource graphQlSource,
                    GraphQlProperties properties) {
        final var path = properties.getPath();
        final var routeBuilder = org.springframework.web.servlet.function.RouterFunctions.route();
        routeBuilder.GET(
                path,
                request ->
                        org.springframework.web.servlet.function.ServerResponse.status(
                                        HttpStatus.METHOD_NOT_ALLOWED)
                                .headers(
                                        headers ->
                                                headers.setAllow(
                                                        Collections.singleton(HttpMethod.POST)))
                                .build());

        routeBuilder.POST(
                path,
                org.springframework.web.servlet.function.RequestPredicates.contentType(
                                MediaType.APPLICATION_JSON)
                        .and(
                                org.springframework.web.servlet.function.RequestPredicates.accept(
                                        MediaType.APPLICATION_GRAPHQL_RESPONSE,
                                        MediaType.APPLICATION_JSON)),
                httpHandler::handleRequest);
        if (properties.getGraphiql().isEnabled()) {
            final var graphiQLHandler =
                    new GraphiQlMvcHandler(path, properties.getWebsocket().getPath());
            routeBuilder.GET(properties.getGraphiql().getPath(), graphiQLHandler::handleRequest);
        }

        if (properties.getSchema().getPrinter().isEnabled()) {
            final var schemaHandler = new SchemaHandler(graphQlSource);
            routeBuilder.GET(path + "/schema", schemaHandler::handleRequest);
        }
        return routeBuilder.build();
    }
}
