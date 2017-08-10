package com.garyclayburg.memuser

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

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
        MemUser memUser = new MemUser(userName: "hi",id: 9)
        UserController userController = new UserController()

        when:
        userController.createUser(memUser)
        def users = userController.getUsers()

        then:
        users.contains(memUser)
        userController.getUser("hi") == memUser
    }
}
