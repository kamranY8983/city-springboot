package com.sample.city.springboot.factory;

import com.sample.city.springboot.component.RequestHeaderMvcInjector;

import graphql.schema.GraphQLSchema;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.execution.Executable;
import io.leangen.graphql.metadata.execution.FixedMethodInvoker;
import io.leangen.graphql.metadata.execution.MethodInvoker;
import io.leangen.graphql.metadata.strategy.query.AbstractResolverBuilder;
import io.leangen.graphql.metadata.strategy.query.MethodInvokerFactory;
import io.leangen.graphql.metadata.strategy.query.ResolverBuilder;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import io.leangen.graphql.spqr.spring.annotations.WithResolverBuilder;
import io.leangen.graphql.util.Utils;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.StandardMethodMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This class configures GraphQL schema factory with dynamic schema defined in a way
 * graphql-spqr-spring-boot-starter library suggests. However, instead of using GraphQl server
 * capabilities of the <code>graphql-spqr-spring-boot-starter</code>, only dynamic schema resolution
 * and creation functionality is used and server is running using spring-graphql instead.
 */
public class GraphQlSchemaFactory implements FactoryBean<GraphQLSchema> {

    private final ConfigurableApplicationContext context;
    private final MethodInvokerFactory aopAwareFactory = new AopAwareMethodInvokerFactory();

    public GraphQlSchemaFactory(final ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Override
    public GraphQLSchema getObject() throws Exception {
        var schemaGenerator = new GraphQLSchemaGenerator();
        findGraphQLApiServices(context)
                .forEach(
                        spqrBean ->
                                schemaGenerator.withOperationsFromBean(
                                        spqrBean.beanSupplier,
                                        spqrBean.type,
                                        spqrBean.exposedType,
                                        spqrBean.resolverBuilders.stream()
                                                .map(
                                                        criteria ->
                                                                findQualifiedBeanByType(
                                                                        criteria.resolverType(),
                                                                        criteria.value(),
                                                                        criteria.qualifierType()))
                                                .peek(
                                                        resolverBuilder -> {
                                                            if (resolverBuilder
                                                                    instanceof
                                                                    AbstractResolverBuilder) {
                                                                ((AbstractResolverBuilder)
                                                                                resolverBuilder)
                                                                        .withMethodInvokerFactory(
                                                                                aopAwareFactory);
                                                            }
                                                        })
                                                .toArray(ResolverBuilder[]::new)));

        // add RequestHeaderMvcInjector to schema generator
        schemaGenerator.withArgumentInjectors(new RequestHeaderMvcInjector());

        return schemaGenerator.generate();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    private List<SpqrBean> findGraphQLApiServices(ConfigurableApplicationContext context) {
        final String[] apiBeanNames = context.getBeanNamesForAnnotation(GraphQLApi.class);
        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

        final var result = new ArrayList<SpqrBean>();
        for (String beanName : apiBeanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            AnnotatedType beanType;
            Set<WithResolverBuilder> resolverBuilders;
            if (beanDefinition.getSource() instanceof StandardMethodMetadata) {
                StandardMethodMetadata metadata =
                        (StandardMethodMetadata) beanDefinition.getSource();
                beanType = metadata.getIntrospectedMethod().getAnnotatedReturnType();
                resolverBuilders =
                        AnnotatedElementUtils.findMergedRepeatableAnnotations(
                                metadata.getIntrospectedMethod(), WithResolverBuilder.class);
            } else {
                BeanDefinition current = beanDefinition;
                BeanDefinition originatingBeanDefinition = current;
                while (current != null) {
                    originatingBeanDefinition = current;
                    current = current.getOriginatingBeanDefinition();
                }
                ResolvableType resolvableType = originatingBeanDefinition.getResolvableType();
                if (resolvableType != ResolvableType.NONE
                        && Utils.isNotEmpty(originatingBeanDefinition.getBeanClassName())) {
                    beanType = GenericTypeReflector.annotate(resolvableType.getType());
                } else {
                    beanType =
                            GenericTypeReflector.annotate(
                                    AopUtils.getTargetClass(context.getBean(beanName)));
                }
                resolverBuilders =
                        AnnotatedElementUtils.findMergedRepeatableAnnotations(
                                beanType, WithResolverBuilder.class);
            }
            List<ResolverBuilderBeanCriteria> builders =
                    resolverBuilders.stream()
                            .map(
                                    builder ->
                                            new ResolverBuilderBeanCriteria(
                                                    builder.value(),
                                                    builder.qualifierValue(),
                                                    builder.qualifierType()))
                            .collect(Collectors.toList());
            result.add(SpqrBean.create(context, beanName, beanType, builders));
        }
        return result;
    }

    /** finds qualified beans used for schema creation by scanning application context */
    private <T> T findQualifiedBeanByType(
            Class<? extends T> type,
            String qualifierValue,
            Class<? extends Annotation> qualifierType) {
        final NoSuchBeanDefinitionException noSuchBeanDefinitionException =
                new NoSuchBeanDefinitionException(
                        qualifierValue,
                        "No matching "
                                + type.getSimpleName()
                                + " bean found for qualifier "
                                + qualifierValue
                                + " of type "
                                + qualifierType.getSimpleName()
                                + " !");
        try {
            if (Utils.isEmpty(qualifierValue)) {
                if (qualifierType.equals(Qualifier.class)) {
                    return Optional.of(context.getBean(type))
                            .orElseThrow(() -> noSuchBeanDefinitionException);
                }
                return context.getBean(
                        Arrays.stream(context.getBeanNamesForAnnotation(qualifierType))
                                .filter(beanName -> type.isInstance(context.getBean(beanName)))
                                .findFirst()
                                .orElseThrow(() -> noSuchBeanDefinitionException),
                        type);
            }

            return BeanFactoryAnnotationUtils.qualifiedBeanOfType(
                    context.getBeanFactory(), type, qualifierValue);
        } catch (NoSuchBeanDefinitionException noBeanException) {
            return handleNoBeanDefinitionException(
                    type, qualifierValue, qualifierType, noBeanException);
        }
    }

    // handles NoSuchBeanDefinitionException during findQualifiedBeanByType
    private <T> T handleNoBeanDefinitionException(
            Class<? extends T> type,
            String qualifierValue,
            Class<? extends Annotation> qualifierType,
            NoSuchBeanDefinitionException exception) {
        final var factory = context.getBeanFactory();

        for (String name : factory.getBeanDefinitionNames()) {
            final var bd = factory.getBeanDefinition(name);
            if (bd.getSource() instanceof StandardMethodMetadata metadata
                    && metadata.getReturnTypeName().equals(type.getName())) {
                final var attributes = metadata.getAnnotationAttributes(qualifierType.getName());
                if (attributes != null) {
                    if (qualifierType.equals(Qualifier.class)
                            && qualifierValue.equals(attributes.get("value"))) {
                        return context.getBean(name, type);
                    }
                    return context.getBean(name, type);
                }
            }
        }

        throw exception;
    }

    @Override
    public Class<?> getObjectType() {
        return GraphQLSchema.class;
    }

    private record SpqrBean(
            BeanScope scope,
            Supplier<Object> beanSupplier,
            AnnotatedType type,
            Class<?> exposedType,
            List<ResolverBuilderBeanCriteria> resolverBuilders) {

        public static SpqrBean create(
                ApplicationContext context,
                String beanName,
                AnnotatedType type,
                List<ResolverBuilderBeanCriteria> resolverBuilders) {
            final var beanScope = BeanScope.findBeanScope(context, beanName);
            Supplier<Object> beanSupplier;
            if (beanScope == BeanScope.SINGLETON) {
                final var singletonBean = context.getBean(beanName);
                beanSupplier = () -> singletonBean;
            } else {
                beanSupplier = () -> context.getBean(beanName);
            }
            return new SpqrBean(
                    beanScope,
                    beanSupplier,
                    type,
                    context.getType(beanName),
                    Collections.unmodifiableList(resolverBuilders));
        }
    }

    private record ResolverBuilderBeanCriteria(
            Class<? extends ResolverBuilder> resolverType,
            String value,
            Class<? extends Annotation> qualifierType) {}

    private enum BeanScope {
        SINGLETON,
        PROTOTYPE,
        UNKNOWN;

        static BeanScope findBeanScope(ApplicationContext context, String beanName) {
            if (context.isSingleton(beanName)) {
                return SINGLETON;
            } else if (context.isPrototype(beanName)) {
                return PROTOTYPE;
            } else {
                return UNKNOWN;
            }
        }
    }

    public static class AopAwareMethodInvokerFactory implements MethodInvokerFactory {

        @Override
        public Executable<Method> create(
                Supplier<Object> targetSupplier,
                Method resolverMethod,
                AnnotatedType enclosingType,
                Class<?> exposedType) {
            resolverMethod = AopUtils.selectInvocableMethod(resolverMethod, exposedType);
            return targetSupplier == null
                    ? new MethodInvoker(resolverMethod, enclosingType)
                    : new FixedMethodInvoker(targetSupplier, resolverMethod, enclosingType);
        }
    }
}
