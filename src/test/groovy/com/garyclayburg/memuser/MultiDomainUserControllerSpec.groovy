package com.garyclayburg.memuser

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import java.time.ZonedDateTime

/**
 * <br><br>
 * Created 2021-07-01 10:05
 *
 * @author Gary Clayburg
 */
class MultiDomainUserControllerSpec extends Specification {
    def "OverrideLocation"() {
        given:
        HttpServletRequest mockRequest = Mock()

        def requestURL = 'https://www.realserver.com/Users/'
        mockRequest.requestURL >> new StringBuffer(requestURL)
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

    def "override location for memuser collection"() {
        given:
        HttpServletRequest mockRequest = Mock()

        def requestURL = 'https://www.realserver.com/Users'
        mockRequest.requestURL >> new StringBuffer(requestURL)
        MemUser memUser = new MemUser(id: '999', userName: 'nines')
        def now = ZonedDateTime.now()
        memUser.setMeta(new Meta(location: 'http://example.com/Users/999',
                created: now,
                lastModified: now,
                resourceType: 'User'))

        def users = [memUser]
        when:
        users = MultiDomainUserController.overrideLocation(users,mockRequest)

        then:
        users[0].meta.location == 'https://www.realserver.com/Users/999'

    }

    def "override location with id"() {
        given:
        HttpServletRequest mockRequest = Mock()
        mockRequest.requestURL >> new StringBuffer(requestURL)
        expect: 'meta.location has location with id'
        MultiDomainUserController.overrideLocation(memScimResource,mockRequest).meta.location == expectedLocation
        where:
        requestURL | memScimResource | expectedLocation
        'https://www.hi.com/Users' | new MemUser(id: 12345,meta: new Meta()) | 'https://www.hi.com/Users/12345'
        'https://www.hi.com/Users/' | new MemUser(id: 12345,meta: new Meta()) | 'https://www.hi.com/Users/12345'
        'https://www.hi.com/Groups' | new MemGroup(id: 12345,meta: new Meta()) | 'https://www.hi.com/Groups/12345'
        'https://www.hi.com/Groups/' | new MemGroup(id: 12345,meta: new Meta()) | 'https://www.hi.com/Groups/12345'
    }

    def "filter proxy"(String x_forwarded_proto, String x_forwarded_host, String location, String expectedLocation) {
        given:
        HttpServletRequest mockRequest = Mock()
        if (x_forwarded_proto != null && x_forwarded_host != null) {
            mockRequest.getHeader(UserController.X_FORWARDED_PROTO) >> x_forwarded_proto
            mockRequest.getHeader(UserController.X_FORWARDED_HOST) >> x_forwarded_host
        }
        expect: 'meta.location is correct when using frontend proxy'
        MultiDomainUserController.filterProxiedURL(mockRequest,location) == expectedLocation

        where:
        x_forwarded_proto | x_forwarded_host |  location  |  expectedLocation
        'https' | 'www.a.com' | 'https://www.realserver.com/Users/qwerty' |  'https://www.a.com/Users/qwerty'
        'https' | 'www.a.com' | 'https://www.realserver.com/Users/qwerty/' |  'https://www.a.com/Users/qwerty/'
        'https' | 'www.a.com' | 'https://www.realserver.com/Groups/qwerty/' |  'https://www.a.com/Groups/qwerty/'
        'http' | 'www.a.com'  | 'https://www.realserver.com/stuff/Users/qwerty' |  'http://www.a.com/stuff/Users/qwerty'
        null    | null        | 'https://www.realserver.com/Users/qwerty' |  'https://www.realserver.com/Users/qwerty'
    }
}
