package com.sample.city.springboot.component;

import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.generator.mapping.common.InputValueDeserializer;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

@Slf4j
public class RequestHeaderMvcInjector extends InputValueDeserializer {

    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        final var headerName =
                params.getArgument().getTypedElement().getAnnotation(RequestHeader.class).value();
        final var headerValue =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest()
                        .getHeader(headerName);

        log.debug(
                "Injecting '{}' header value '{}' as '{} parameter value",
                headerName,
                headerValue,
                params.getParameter().getName());
        return headerValue;
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return parameter != null && parameter.isAnnotationPresent(RequestHeader.class);
    }
}
