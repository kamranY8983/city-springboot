package com.sample.city.springboot.factory;

import graphql.GraphqlErrorBuilder;
import graphql.schema.GraphQLSchema;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.GraphQlSource;

import java.util.List;

/**
 * Factory to construct instance of (@link GraphQlSource) with custom exception resolver for Grapl
 * data fetcher
 */
public class GraphQlSourceFactory implements FactoryBean<GraphQlSource> {

    private static final DataFetcherExceptionResolver GRAPHQL_EXCEPTION_RESOLVER =
            DataFetcherExceptionResolver.forSingleError(
                    (ex, env) ->
                            GraphqlErrorBuilder.newError(env)
                                    .errorType(ErrorType.BAD_REQUEST)
                                    .message(
                                            "[%s] %s"
                                                    .formatted(
                                                            ex.getClass().getSimpleName(),
                                                            ex.getMessage()))
                                    .build());

    private final GraphQLSchema graphQLSchema;

    /**
     * Creates instance of (@link GraphQLSourceFactory) with provided GraphQL schema
     *
     * <p>BS
     *
     * @param graphQLSchema graphql schema to be used to build (@link GraphQlSource] instance
     */
    public GraphQlSourceFactory(GraphQLSchema graphQLSchema) {
        this.graphQLSchema = graphQLSchema;
    }

    @Override
    public GraphQlSource getObject() {
        return GraphQlSource.builder(graphQLSchema)
                .exceptionResolvers(List.of(GRAPHQL_EXCEPTION_RESOLVER))
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return GraphQlSource.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
