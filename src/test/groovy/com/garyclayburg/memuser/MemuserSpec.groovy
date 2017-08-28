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
        UserFragmentList users = userController.users

        then:
        users.resources.contains(memUser)
        userController.getUser(((MemUser) createdUser.getBody()).id) == memUser
    }
}
