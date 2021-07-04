package com.garyclayburg.memuser

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap
import org.glassfish.jersey.uri.internal.JerseyUriBuilder

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.PathSegment
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo

/**
 * <br><br>
 * Created 2021-07-02 14:48
 *
 * @author Gary Clayburg
 */
class UriInfoShim implements UriInfo {

    private HttpServletRequest httpServletRequest
    private MultivaluedStringMap queryParametersMap

    UriInfoShim(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest
        this.queryParametersMap = createQueryParametersMap()
    }

    @Override
    String getPath() {
        return null
    }

    @Override
    String getPath(boolean decode) {
        return null
    }

    @Override
    List<PathSegment> getPathSegments() {
        return null
    }

    @Override
    List<PathSegment> getPathSegments(boolean decode) {
        return null
    }

    @Override
    URI getRequestUri() {
        return null
    }

    @Override
    UriBuilder getRequestUriBuilder() {
        return null
    }

    @Override
    URI getAbsolutePath() {
        return null
    }

    @Override
    UriBuilder getAbsolutePathBuilder() {
        return null
    }

    @Override
    URI getBaseUri() {
        return new URI(httpServletRequest.getRequestURI())
    }

    @Override
    UriBuilder getBaseUriBuilder() {
        return new JerseyUriBuilder().uri(getBaseUri())
    }

    @Override
    MultivaluedMap<String, String> getPathParameters() {
        return new MultivaluedStringMap() //todo is this always good enough?
//        return httpServletRequest.getParameterMap()
//        return null
    }

    @Override
    MultivaluedMap<String, String> getPathParameters(boolean decode) {
        return null
    }

    @Override
    MultivaluedMap<String, String> getQueryParameters() {
        return queryParametersMap
    }

    private MultivaluedStringMap createQueryParametersMap() {
        MultivaluedStringMap queryParametersMap = new MultivaluedStringMap()
        Map<String, String[]> parameterMap = httpServletRequest.getParameterMap()
        parameterMap.each { key, value ->
            value.each {
                queryParametersMap.add(key, it)
            }
        }
        queryParametersMap
    }

    @Override
    MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return null
    }

    @Override
    List<String> getMatchedURIs() {
        return null
    }

    @Override
    List<String> getMatchedURIs(boolean decode) {
        return null
    }

    @Override
    List<Object> getMatchedResources() {
        return null
    }

    @Override
    URI resolve(URI uri) {
        return null
    }

    @Override
    URI relativize(URI uri) {
        return null
    }
}
