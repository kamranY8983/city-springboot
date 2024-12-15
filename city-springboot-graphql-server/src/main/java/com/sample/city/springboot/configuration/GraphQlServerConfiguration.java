package com.sample.city.springboot.configuration;

import com.sample.city.springboot.factory.GraphQlSchemaFactory;
import com.sample.city.springboot.factory.GraphQlSourceFactory;

import graphql.schema.GraphQLSchema;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.DefaultBatchLoaderRegistry;
import org.springframework.graphql.execution.DefaultExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;

/** Configuration with beans used to produce GraphQl server */
@EnableAutoConfiguration
@Configuration
@ConditionalOnProperty(name = "graphql.server.enabled", havingValue = "true")
@EnableConfigurationProperties(GraphQlProperties.class)
public class GraphQlServerConfiguration {

    /**
     * GraphQL schema factory bean based on injected application context
     *
     * @param context application context to be used for schema creation
     * @return configured instance of {@link GraphQlSchemaFactory}
     */
    @Bean
    public GraphQlSchemaFactory graphQlSchema(ConfigurableApplicationContext context) {
        return new GraphQlSchemaFactory(context);
    }

    /**
     * GraphQL source factory bean based on GraphQL schema
     *
     * <p>Qparam schema GraphQl schema to derive source from
     *
     * @return configured instance of (@link GraphQlSourceFactory]
     */
    @Bean
    public GraphQlSourceFactory graphQlSource(GraphQLSchema schema) {
        return new GraphQlSourceFactory(schema);
    }

    @Bean
    @ConditionalOnMissingBean
    public BatchLoaderRegistry batchLoaderRegistry() {
        return new DefaultBatchLoaderRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutionGraphQlService executionGraphQlService(
            GraphQlSource graphQlSource, BatchLoaderRegistry batchLoaderRegistry) {
        final var service = new DefaultExecutionGraphQlService(graphQlSource);
        service.addDataLoaderRegistrar(batchLoaderRegistry);
        return service;
    }
}
