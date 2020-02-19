package com.garyclayburg.memuser

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import java.time.ZonedDateTime

/**
 * <br><br>
 * Created 2017-08-10 07:57
 *
 * @author Gary Clayburg
 */
@SpringBootTest
class MemuserSpec extends Specification {

    def "contextok"() {
        expect:
        true
    }

    def "adduser with simulated proxy"() {
        given: 'setup one user'
        MemUser memUser = new MemUser(userName: 'hi')
        UserController userController = new UserController(new MemuserSettings())

        HttpServletRequest mockGetProxy = setupProxiedMockRequest(
                'http://www.example.com/Users',
                'https',
                'www.examplesecure.com:443')
        Pageable pageable = new PageRequest(0, 8)

        when: 'add user'
        Object createdUser = userController.addUser(mockGetProxy, memUser)

        then: 'meta.location is correct for proxy'
        createdUser.body.meta.location == 'https://www.examplesecure.com:443/Users/' + createdUser.body.id
        when: 'get the user we just added'
        mockGetProxy = setupProxiedMockRequest(
                'http://www.example.com/Users/' + createdUser.body.id,
                'https',
                'www.examplesecure.com:443')
        def getUser = userController.getUser(mockGetProxy, ((MemUser) createdUser.body).id)

        then: 'returned user matches added user'
        getUser == memUser
        getUser.meta.location == 'https://www.examplesecure.com:443/Users/' + getUser.id

        when: 'get the user list via simulated proxy'
        mockGetProxy = setupProxiedMockRequest(
                'http://www.example.com/Users',
                'https',
                'www.examplesecure.com:443')
        UserFragmentList users = userController.getUsers(mockGetProxy, pageable).body
        then:
        users.resources.contains(memUser)
        users.resources[0].meta.location == 'https://www.examplesecure.com:443/Users/' + getUser.id

        when: 'get user again'
        mockGetProxy = setupProxiedMockRequest(
                'http://www.example.com/Users/' + getUser.id,
                'https',
                'www.examplesecure.com:443')
        getUser = userController.getUser(mockGetProxy, ((MemUser) createdUser.body).id)
        then:
        getUser == memUser
        getUser.meta.location == 'https://www.examplesecure.com:443/Users/' + getUser.id
    }

    private HttpServletRequest setupProxiedMockRequest(String requestURL, String proto, String forwardedHost) {
        HttpServletRequest mockGetProxy = Mock()
        mockGetProxy.getHeader(UserController.X_FORWARDED_PROTO) >> proto
        mockGetProxy.getHeader(UserController.X_FORWARDED_HOST) >> forwardedHost
        mockGetProxy.requestURL >> new StringBuffer(requestURL)
        mockGetProxy
    }

    def "adduser"() {
        given: 'setup one user'
        ZonedDateTime testStart = ZonedDateTime.now()
        Thread.sleep(1L) // yes this is needed. the test can run so fast that now() will be the same millisecond throughout the test method
        MemUser memUser = new MemUser(userName: 'hi')
        UserController userController = new UserController(new MemuserSettings())

        HttpServletRequest mockRequest = Mock()
        mockRequest.requestURL >> new StringBuffer('http://www.example.com/Users')
        Pageable pageable = new PageRequest(0, 8)

        when: 'add user'
        Object createdUser = userController.addUser(mockRequest, memUser)
        UserFragmentList users = userController.getUsers(mockRequest, pageable).body

        then: 'returned user matches added user'
        users.resources.contains(memUser)
        def getUser = userController.getUser(mockRequest, ((MemUser) createdUser.body).id)
        getUser == memUser

        when: 'request user from url'
        HttpServletRequest mockGet = Mock()
        def mockgetid = ((MemUser) createdUser.body).id
        mockGet.requestURL >> new StringBuffer("http://localhost:1234/Users/${mockgetid}")
        getUser = userController.getUser(mockGet, mockgetid)

        then: 'returned user has correct meta.location'
        getUser.meta.location == "http://localhost:1234/Users/${memUser.id}"
        testStart.isBefore(getUser.meta.created)

        when: 'request user from proxyurl'
        def mockgetidProxy = ((MemUser) createdUser.body).id
        HttpServletRequest mockGetProxy = setupProxiedMockRequest(
                "http://localhost:1234/Users/${mockgetidProxy}",
                'https',
                'www.example44.com:443')

        getUser = userController.getUser(mockGetProxy, mockgetidProxy)

        then: 'returned user has correct meta.location'
        getUser.meta.location == "https://www.example44.com:443/Users/${memUser.id}"
        testStart.isBefore(getUser.meta.created)

        when: 'put modified user'
        MemUser mUser = (getUser as MemUser)
        mUser.setData('extrastuff', 'somenewvalue')

        mockGetProxy = setupProxiedMockRequest(
                "http://localhost:1234/Users/${mockgetidProxy}",
                'https',
                'www.example2.com:443')
        def returnedUser = userController.putUser(mockGetProxy, mUser, mUser.id).body

        then: 'location header is created from proxy headers'
        true
        returnedUser.meta.location == "https://www.example2.com:443/Users/${memUser.id}"

        when: 'get list of all users'
        HttpServletRequest mockGet2 = Mock()
        mockGet2.requestURL >> new StringBuffer('http://nowherespecial:1234/Users')
        def userList = userController.getUsers(mockGet2, pageable).body

        then: 'first user in list has meta.location'
        userList.Resources[0].meta.location == "http://nowherespecial:1234/Users/${memUser.id}"

        and: 'SCIM page size correct'
        userList.totalResults == 1
        userList.itemsPerPage == 1

        when: 'add a user'
        MemUser memUser2 = new MemUser(userName: 'justanewguy')
        userController.addUser(mockRequest, memUser2)
        UserFragmentList userList2 = userController.getUsers(mockRequest, pageable).body

        then: '2 users fit on bigger page'
        userList2.totalResults == 2
        userList2.Resources.size() == 2
        userList2.itemsPerPage == 2
        userList2.startIndex == 1

        when: 'request page of 1'
        UserFragmentList page1of2 = userController.getUsers(mockRequest, new PageRequest(0, 1)).body

        then: '1 user on first page'
        page1of2.totalResults == 2
        page1of2.startIndex == 1
        page1of2.itemsPerPage == 1
        page1of2.resources[0].userName == 'hi'
        page1of2.resources.size() == 1

        when: 'request page 2'
        UserFragmentList page2of2 = userController.getUsers(mockRequest, new PageRequest(1, 1)).body

        then: 'last user on this page'
        page2of2.totalResults == 2
        page2of2.startIndex == 2
        page2of2.itemsPerPage == 1
        page1of2.resources.size() == 1
        page2of2.resources[0].userName == 'justanewguy'

        when: 'request page out of bounds'
        UserFragmentList pageInvalid = userController.getUsers(mockRequest, new PageRequest(2, 1)).body

        then: 'return empty list'
        pageInvalid.totalResults == 0
        pageInvalid.startIndex == 0
        pageInvalid.itemsPerPage == 0
        pageInvalid.resources.size() == 0

    }
}
