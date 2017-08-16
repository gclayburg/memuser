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
class MemuserSpec extends Specification{

    def "contextok"(){
        expect:
        true
    }

    def "createuser"(){
        given:
        MemUser memUser = new MemUser(userName: "hi")
        UserController userController = new UserController()
        HttpServletRequest mockRequest = Mock()
        mockRequest.getRequestURL() >> new StringBuffer("http://www.example.com/Users")


        when:
        Object createdUser = userController.createUser(mockRequest,memUser)
        UserFragmentList users = userController.getUsers()

        then:
        users.getResources().contains(memUser)
        userController.getUser(((MemUser)createdUser).id) == memUser
    }
}
