package com.garyclayburg.memuser


import com.fasterxml.jackson.databind.ObjectMapper
import com.unboundid.scim2.common.types.SchemaResource
import com.unboundid.scim2.common.types.UserResource
import com.unboundid.scim2.common.utils.SchemaUtils
import groovy.json.JsonBuilder

import javax.servlet.http.HttpServletRequest
import java.time.ZonedDateTime

/**
 * <br><br>
 * Created 2021-07-01 10:05
 *
 * @author Gary Clayburg
 */
class MultiDomainUserControllerSpec extends HttpMockSpecification {

    def "OverrideLocation"() {
        given:
        def requestURL = 'https://www.realserver.com/Users/'
        HttpServletRequest mockRequest = setupMockRequest(requestURL)

        MemUser memUser = new MemUser(id: '999', userName: 'nines')
        def now = ZonedDateTime.now()
        memUser.setMeta(new Meta(location: 'http://example.com/Users/999',
                created: now,
                lastModified: now,
                resourceType: 'User'))

        when:
        MultiDomainUserController.overrideLocation(memUser, mockRequest)

        then: 'location is corrected'
        memUser.meta.location == 'https://www.realserver.com/Users/999'
        mockRequest.requestURL.toString() == requestURL
    }

    def "OverrideLocation with incomplete location"() {
        given:
        def requestURL = 'https://www.realserver.com/Users/'
        HttpServletRequest mockRequest = setupMockRequest(requestURL)

        MemUser memUser = new MemUser(id: '999', userName: 'nines')
        def now = ZonedDateTime.now()
        memUser.setMeta(new Meta(location: '/Users/999',
                created: now,
                lastModified: now,
                resourceType: 'User'))

        when:
        MultiDomainUserController.overrideLocation(memUser, mockRequest)

        then: 'location proto and host is corrected to match request, not whatever was stored before'
        memUser.meta.location == 'https://www.realserver.com/Users/999'
        mockRequest.requestURL.toString() == requestURL
    }

    def "override location for memuser collection"() {
        given:
        def requestURL = 'https://www.realserver.com/Users'
        HttpServletRequest mockRequest = setupMockRequest(requestURL)
        MemUser memUser = new MemUser(id: '999', userName: 'nines')
        def now = ZonedDateTime.now()
        memUser.setMeta(new Meta(location: 'http://example.com/Users/999',
                created: now,
                lastModified: now,
                resourceType: 'User'))

        def users = [memUser]
        when:
        users = MultiDomainUserController.overrideLocation(users, mockRequest)

        then:
        users[0].meta.location == 'https://www.realserver.com/Users/999'

    }

    def "override location with id"() {
        given:
        HttpServletRequest mockRequest = setupMockRequest(requestURL)

        expect: 'meta.location has location with id'
        MultiDomainUserController.overrideLocation(memScimResource, mockRequest).meta.location == expectedLocation
        where:
        requestURL                   | memScimResource                           | expectedLocation
        'https://www.hi.com/Users'   | new MemUser(id: 12345, meta: new Meta())  | 'https://www.hi.com/Users/12345'
        'https://www.hi.com/Users/'  | new MemUser(id: 12345, meta: new Meta())  | 'https://www.hi.com/Users/12345'
        'https://www.hi.com/Groups'  | new MemGroup(id: 12345, meta: new Meta()) | 'https://www.hi.com/Groups/12345'
        'https://www.hi.com/Groups/' | new MemGroup(id: 12345, meta: new Meta()) | 'https://www.hi.com/Groups/12345'
    }

    def "showresourcetypes"() {
        when:
        def requestURL = 'https://www.realserver.com/somejunk/ResourceTypes/'
        HttpServletRequest mockRequest = setupMockRequest(requestURL)
        def resourcetypes = MultiDomainUserController.showResourcetypes(mockRequest)
        then:
        String json = new JsonBuilder(resourcetypes).toPrettyString()
        println "real jsonpretty is ${json}"
        resourcetypes.Resources[1].meta.location == 'https://www.realserver.com/somejunk/ResourceTypes/Group'
        resourcetypes.Resources[0].meta.location == 'https://www.realserver.com/somejunk/ResourceTypes/User'

    }

    def "print schema"() {
        given:
        SchemaResource schemaResource = SchemaUtils.getSchema(UserResource.class)
        def schemaResponse = MultiDomainUserController.showSchemas()
        expect:
        schemaResource != null
        schemaResponse.totalResults == 2
        println "pretty schemas: " + prettyJson(schemaResponse)
    }

    static String prettyJson(Object object) {
        ObjectMapper mapper = new ConfigMe().serializingObjectMapper()
        mapper.writeValueAsString(object)
    }

    def "stripslash"() {
        expect:
        MultiDomainUserController.stripAnyTrailingSlash("hi/") == 'hi'
        MultiDomainUserController.stripAnyTrailingSlash("hi/ ") == 'hi'
        MultiDomainUserController.stripAnyTrailingSlash("hi//") == 'hi'
        MultiDomainUserController.stripAnyTrailingSlash("hi") == 'hi'
        MultiDomainUserController.stripAnyTrailingSlash("hi/there") == 'hi/there'
        MultiDomainUserController.stripAnyTrailingSlash("") == ''
        MultiDomainUserController.stripAnyTrailingSlash() == null
    }

    def "filter proxy"(String x_forwarded_proto, String x_forwarded_host, String location, String expectedLocation) {
        given:
        HttpServletRequest mockRequest = Mock()
        if (x_forwarded_proto != null && x_forwarded_host != null) {
            mockRequest.getHeader(UserController.X_FORWARDED_PROTO) >> x_forwarded_proto
            mockRequest.getHeader(UserController.X_FORWARDED_HOST) >> x_forwarded_host
        }
        expect: 'meta.location is correct when using frontend proxy'
        MultiDomainUserController.filterProxiedURL(mockRequest, location) == expectedLocation

        where:
        x_forwarded_proto | x_forwarded_host | location                                        | expectedLocation
        'https'           | 'www.a.com'      | 'https://www.realserver.com/Users/qwerty'       | 'https://www.a.com/Users/qwerty'
        'https'           | 'www.a.com'      | 'https://www.realserver.com/Users/qwerty/'      | 'https://www.a.com/Users/qwerty/'
        'https'           | 'www.a.com'      | 'https://www.realserver.com/Groups/qwerty/'     | 'https://www.a.com/Groups/qwerty/'
        'http'            | 'www.a.com'      | 'https://www.realserver.com/stuff/Users/qwerty' | 'http://www.a.com/stuff/Users/qwerty'
        null              | null             | 'https://www.realserver.com/Users/qwerty'       | 'https://www.realserver.com/Users/qwerty'
    }
}
