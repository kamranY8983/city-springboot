package com.sample.city.springboot.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriBuilder;

import java.net.URI;

/** Custom Spring MVC handler to serve a GraphiQl UI page with support for Spring Cloud Gat */
public class GraphiQlMvcHandler {
    private static final Logger Log = LoggerFactory.getLogger(GraphiQlMvcHandler.class);

    private static final String X_FORWARDED_PROTO = "x-forwarded-proto";
    private static final String X_FORWARDED_PREFIX = "x-forwarded-prefix";
    private static final String X_FORWARDED_HOST = "x-forwarded-host";
    private static final String X_FORWARDED_PORT = "x-forwarded-port";

    private static final String PATH = "path";
    private static final String WS_PATH = "WsPath";
    private final String graphQlPath;
    private final String graphQlWsPath;
    private final Resource htmlResource;

    /**
     * Constructor that serves the default (@code graphiql/index.html) included in the t@code
     * spring-graphql] module,
     *
     * <p>⁃ @param grapholPath the path to the GraphOL HTTP endpoint
     *
     * @param graphQlWsPath optional path to the GraphQl WebSocket endpoint
     */
    public GraphiQlMvcHandler(String graphQlPath, String graphQlWsPath) {
        this(graphQlPath, graphQlWsPath, new ClassPathResource("graphiql/index.html"));
    }

    /**
     * Constructor with the HTML page to serve.
     *
     * @param graphQlPath the path to the GraphQL HTTP endpoint
     * @param graphQlWsPath optional path to the GraphQl WwebSocket endpoint Qparam htmlResource the
     *     GraphiQl page to serve
     */
    public GraphiQlMvcHandler(String graphQlPath, String graphQlWsPath, Resource htmlResource) {
        Assert.hasText(graphQlPath, "graphQlPath should not be empty");
        this.graphQlPath = graphQlPath;
        this.graphQlWsPath = graphQlWsPath;
        this.htmlResource = htmlResource;
    }

    /**
     * Render the Graphiol page as "text/html" " and redirect back to the same URL
     *
     * @param request the current request Oreturn the response to render
     */
    public ServerResponse handleRequest(ServerRequest request) {
        return request.param("path").isPresent()
                ? ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(this.htmlResource)
                : ServerResponse.temporaryRedirect(getRedirectUrl(request)).build();
    }

    /**
     * or if the "path"
     *
     * <p>query parameter is missi
     */
    private URI getRedirectUrl(ServerRequest request) {
        final var uriBuilder = request.uriBuilder();
        final var pathQueryParameterValue = applyPathPrefix(request, this.graphQlPath);
        final var wsPathQueryParameterValue = applyPathPrefix(request, this.graphQlWsPath);

        // check if we have forwarding headers and if we do, set up the redirect URL accor
        final var xForwardedProto = request.headers().firstHeader(X_FORWARDED_PROTO);
        final var xForwardedPrefix = request.headers().firstHeader(X_FORWARDED_PREFIX);
        final var xForwardedHost = request.headers().firstHeader(X_FORWARDED_HOST);
        final var xForwardedPort = request.headers().firstHeader(X_FORWARDED_PORT);
        if (xForwardedProto != null
                && xForwardedPrefix != null
                && xForwardedHost != null
                && xForwardedPort != null) {
            Log.info("Forwarded request detected to '()", request.requestPath());
            uriBuilder
                    .scheme(xForwardedProto)
                    .host(xForwardedHost.replaceFirst(":\\d+$", ""))
                    .port(xForwardedPort)
                    .replacePath(xForwardedPrefix)
                    .path(request.requestPath().value());
            setQueryParams(
                    uriBuilder,
                    xForwardedPrefix + pathQueryParameterValue,
                    xForwardedPrefix + wsPathQueryParameterValue);
        } else {
            setQueryParams(uriBuilder, pathQueryParameterValue, wsPathQueryParameterValue);
        }
        final var redirectToUri = uriBuilder.build(request.pathVariables());
        Log.info("Redirecting '{}' to '{}'", request.uri(), redirectToUri);
        return redirectToUri;
    }

    private void setQueryParams(UriBuilder uriBuilder, String pathValue, String wsPathValue) {
        uriBuilder.queryParam(PATH, pathValue);
        if (StringUtils.hasText(this.graphQlWsPath)) {
            uriBuilder.queryParam(WS_PATH, wsPathValue);
        }
    }

    private String applyPathPrefix(ServerRequest request, String path) {
        final var fullPath = request.requestPath().value();
        final var pathWithinApplication = request.requestPath().pathWithinApplication().toString();
        final var pathWithinApplicationIndex = fullPath.indexOf(pathWithinApplication);
        return (pathWithinApplicationIndex != -1)
                ? fullPath.substring(0, pathWithinApplicationIndex) + path
                : path;
    }
}
