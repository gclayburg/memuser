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

    def "adduser"() {
        given: "setup one user"
        MemUser memUser = new MemUser(userName: 'hi')
        UserController userController = new UserController()
        HttpServletRequest mockRequest = Mock()
        mockRequest.requestURL >> new StringBuffer('http://www.example.com/Users')
        ZonedDateTime testStart = ZonedDateTime.now()
        Pageable pageable = new PageRequest(0, 8)


        when: "add user"
        Object createdUser = userController.addUser(mockRequest, memUser)
        UserFragmentList users = userController.getUsers(mockRequest,pageable)

        then: "returned user matches added user"
        users.resources.contains(memUser)
        def getUser = userController.getUser(mockRequest, ((MemUser) createdUser.getBody()).id)
        getUser == memUser

        when: "request user from url"
        HttpServletRequest mockGet = Mock()
        def mockgetid = ((MemUser) createdUser.getBody()).id
        mockGet.requestURL >> new StringBuffer("http://localhost:1234/Users/${mockgetid}")
        getUser = userController.getUser(mockGet, mockgetid)

        then: "returned user has correct meta.location"
        getUser.meta.location == "http://localhost:1234/Users/${memUser.id}"
        testStart.isBefore(getUser.meta.created)

        when: "get list of all users"
        HttpServletRequest mockGet2 = Mock()
        mockGet2.requestURL >> new StringBuffer('http://nowherespecial:1234/Users')
        def userList = userController.getUsers(mockGet2,pageable)

        then: "first user in list has meta.location"
        userList.Resources[0].meta.location == "http://nowherespecial:1234/Users/${memUser.id}"

        and: "SCIM page size correct"
        userList.totalResults == 1
        userList.itemsPerPage == 1

        when: "add a user"
        MemUser memUser2 = new MemUser(userName: 'justanewguy')
        userController.addUser(mockRequest, memUser2)
        UserFragmentList userList2 = userController.getUsers(mockRequest,pageable)

        then: "2 users fit on bigger page"
        userList2.totalResults == 2
        userList2.Resources.size() == 2
        userList2.itemsPerPage == 2
        userList2.startIndex == 1

        when: "request page of 1"
        UserFragmentList page1of2 = userController.getUsers(mockRequest, new PageRequest(0, 1))

        then: "1 user on first page"
        page1of2.totalResults == 2
        page1of2.startIndex == 1
        page1of2.itemsPerPage == 1
        page1of2.resources[0].userName == 'hi'
        page1of2.resources.size() ==1

        when: "request page 2"
        UserFragmentList page2of2 = userController.getUsers(mockRequest, new PageRequest(1, 1))

        then: "last user on this page"
        page2of2.totalResults ==2
        page2of2.startIndex == 2
        page2of2.itemsPerPage ==1
        page1of2.resources.size() ==1
        page2of2.resources[0].userName == 'justanewguy'

        when: "request page out of bounds"
        UserFragmentList pageInvalid = userController.getUsers(mockRequest, new PageRequest(2, 1))

        then: "return empty list"
        pageInvalid.totalResults == 0
        pageInvalid.startIndex == 0
        pageInvalid.itemsPerPage ==0
        pageInvalid.resources.size() == 0

    }
}
