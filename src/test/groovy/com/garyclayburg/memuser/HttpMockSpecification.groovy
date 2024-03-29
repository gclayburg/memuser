package com.garyclayburg.memuser

import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * <br><br>
 * Created 2021-07-03 14:32
 *
 * @author Gary Clayburg
 */
class HttpMockSpecification extends Specification {

    HttpServletRequest setupMockRequest(String requestURL) {
        return setupProxiedMockRequest(requestURL,null,null)
    }
    HttpServletRequest setupProxiedMockRequest(String requestURL, String proto, String forwardedHost) {
        HttpServletRequest mockGetProxy = Mock()
        if (proto) mockGetProxy.getHeader(UserController.X_FORWARDED_PROTO) >> proto
        if (forwardedHost) mockGetProxy.getHeader(UserController.X_FORWARDED_HOST) >> forwardedHost
        mockGetProxy.requestURL >> new StringBuffer(requestURL)

        //UriInfo needs these
        String decodedURL = UriUtils.decode(requestURL,'UTF-8')
        MultiValueMap<String,String> parameters = UriComponentsBuilder.fromUriString(decodedURL).build().getQueryParams()

        mockGetProxy.parameterMap >> parameters
        URI uri = new URL(requestURL).toURI()
        //sadly, the URI returned here is NOT the same as the URI returned from request.requestURI
        // request.requestURI has all the hostname ane proto stripped out, so we need to mimic that here too

        def uriStrippedOfProtoAndHost = MultiDomainUserController.extractURI(uri.toString())

        def uriStrippedOfQueryString = MultiDomainUserController.stripURLofQueryString(uriStrippedOfProtoAndHost)
        mockGetProxy.requestURI >> uriStrippedOfQueryString
        mockGetProxy
    }


}
