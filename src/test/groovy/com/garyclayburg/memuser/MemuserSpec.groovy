package com.garyclayburg.memuser

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

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
        given:
        MemUser memUser = new MemUser(userName: 'hi')
        UserController userController = new UserController()
        HttpServletRequest mockRequest = Mock()
        mockRequest.requestURL >> new StringBuffer('http://www.example.com/Users')

        when:
        Object createdUser = userController.addUser(mockRequest, memUser)
        UserFragmentList users = userController.getUsers(mockRequest)

        then:
        users.resources.contains(memUser)
        def getUser = userController.getUser(mockRequest, ((MemUser) createdUser.getBody()).id)
        getUser == memUser

        when:
        HttpServletRequest mockGet = Mock()
        def mockgetid = ((MemUser) createdUser.getBody()).id
        mockGet.requestURL >> new StringBuffer("http://localhost:1234/Users/${mockgetid}")
        getUser = userController.getUser(mockGet, mockgetid)

        then:
        getUser.meta.location == "http://localhost:1234/Users/${memUser.id}"

        when:
        HttpServletRequest mockGet2 = Mock()
        mockGet2.requestURL >> new StringBuffer('http://nowherespecial:1234/Users')
        def userList = userController.getUsers(mockGet2)

        then:
        userList.Resources[0].meta.location == "http://nowherespecial:1234/Users/${memUser.id}"
    }
}
