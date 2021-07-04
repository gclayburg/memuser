package com.garyclayburg.memuser

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * <br><br>
 * Created 2021-06-28 13:05
 *
 * @author Gary Clayburg
 */
@SpringBootTest
@Slf4j
class MemUserGroupSpec extends Specification {

    @Autowired
    ObjectMapper objectMapper

    def "add user to group"() {
        given:
        DomainUserStore domainUserStore = new DomainUserStore()
        MemuserSettings memuserSettings = new MemuserSettings()
        DomainGroupStore domainGroupStore = new DomainGroupStore(domainUserStore)
        MultiDomainUserController multiDomainUserController = new MultiDomainUserController(memuserSettings, domainUserStore, domainGroupStore, objectMapper)
        HttpServletRequest mockGetProxy = setupProxiedMockRequest(
                'http://www.example.com/Users',
                'https',
                'www.examplesecure.com:443')

        def TESTDOMAIN = 'testdomain'

        when: 'add user'
        MemUser memUser = new MemUser(userName: 'testuser1')
        def createdUserResponse = multiDomainUserController.addUser(mockGetProxy, memUser, TESTDOMAIN)
//        multiDomainUserController.getu
        then: 'returned user is in no groups'
        println "response is: ${createdUserResponse.body}"
        def string = objectMapper.writeValueAsString(createdUserResponse.body)
        println "json response is ${string}"
        MemUser memUserReturned = (MemUser) createdUserResponse.body
        memUserReturned.id != null
        createdUserResponse.body.id != null
        createdUserResponse.body.groups == null

        when: 'add group'
        MemGroup memGroup = new MemGroup(displayName: 'group1')
        memGroup.members = [new Members(value: memUser.id,
                display: 'memuser1')]
        HttpServletRequest mockGroupProxy = setupProxiedMockRequest(
                'http://www.example.com/Groups',
                'https',
                'www.examplesecure.com:443')
        def createdGroupResponse = multiDomainUserController.addGroup(mockGroupProxy, memGroup, TESTDOMAIN)

        then:
        createdGroupResponse.body.id != null
        createdGroupResponse.body.members.size() == 1

        when: 'get user again'
        def filledUserResponse = multiDomainUserController.getUser(mockGetProxy, memUser.id, TESTDOMAIN)
        then: 'user is in group'
        filledUserResponse.body.id != null
        filledUserResponse.body.groups != null
        filledUserResponse.body.groups.size() == 1

        when: 'put group again without changes'
        def putGroupResponse = multiDomainUserController.putGroup(mockGroupProxy, memGroup, memGroup.id, TESTDOMAIN)
        then:
        putGroupResponse.statusCode.value() == 200
        putGroupResponse.body.id != null
        putGroupResponse.body.members.size() == 1

        when: 'put group again with invalid member'
        memGroup.members.add(new Members(
                value: 'invaliduserid',
                display: 'invalidusername'))
        def putGroupResponse2 = multiDomainUserController.putGroup(mockGroupProxy, memGroup, memGroup.id, TESTDOMAIN)
        then:
        putGroupResponse2.statusCode.value() == 400
        putGroupResponse2.body.detail != null
        log.info("expected error mesg: ${putGroupResponse2.body.detail}")
    }

    private HttpServletRequest setupProxiedMockRequest(String requestURL, String proto, String forwardedHost) {
        HttpServletRequest mockGetProxy = Mock()
        mockGetProxy.getHeader(UserController.X_FORWARDED_PROTO) >> proto
        mockGetProxy.getHeader(UserController.X_FORWARDED_HOST) >> forwardedHost
        mockGetProxy.requestURL >> new StringBuffer(requestURL)
        mockGetProxy
    }

}
