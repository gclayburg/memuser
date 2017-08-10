package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import groovy.transform.Canonical
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@SpringBootApplication
class MemuserApplication {

    static void main(String[] args) {
        SpringApplication.run MemuserApplication, args
    }
}

@RestController
class UserController {

    Map<String, MemUser> userMap

    UserController() {
        userMap = new HashMap<>()
        userMap.put("bill", new MemUser(userName: "bjones", id: 1))
    }

    @GetMapping("/Users")
    def getUsers() {
        return userMap.values()
    }

    @PostMapping("/Users")
    def createUser(@RequestBody MemUser memUser) {
        if (!userMap.get(memUser.userName)) {
            userMap.put(memUser.userName, memUser)
            return memUser
        } else {
            return new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/Users/{id}")
    def getUser(@PathVariable("id") String id) {
        println "get $id"
        return userMap.get(id)
    }
}

@Canonical
class MemUser {
    String userName
    String id
    protected Map<String, Object> data = new HashMap<>()

    @JsonAnyGetter
    Map<String, Object> getData() {
        return data
    }

    @JsonAnySetter
    void setData(String name, Object value) {
        data.put(name, value)
    }

}
