package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest
import java.time.ZonedDateTime

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
//        def now = ZonedDateTime.now()
//        Meta newmeta = new Meta(location: "http://example.com/Users", created: now, lastModified: now, resourceType: "User")
//        userMap.put("bill", new MemUser(userName: "bjones", id: 1, meta: newmeta))
    }

    @GetMapping("/Users")
    def getUsers() {
        UserFragmentList userFragmentList = new UserFragmentList()
        List<MemUser> memUserList = new ArrayList<>(userMap.values())
        userFragmentList.setResources(memUserList)
        return userFragmentList
    }

    @PostMapping("/Users")
    def createUser(HttpServletRequest request, @RequestBody MemUser memUser) {
        if (!userMap.get(memUser.userName)) {
            memUser.setId(UUID.randomUUID().toString())
            def now = ZonedDateTime.now()
            memUser.setMeta(
                    new Meta(location: request.getRequestURL().append("/").append(memUser.getId()).toString(), created: now, lastModified: now, resourceType: "User"))
            userMap.put(memUser.id, memUser)
            return memUser
        } else {
            return new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/Users/{id}")
    def getUser(@PathVariable("id") String id) {
        println "looking for: $id"
        userMap.get(id) ?: new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
    }
}

@Canonical
class MemUser {
    String userName
    String id
    Meta meta
    protected Map<String, Object> data = new HashMap<>()

    @JsonAnyGetter
    Map<String, Object> getData() {
        return data
    }

    @JsonAnySetter
    void setData(String name, Object value) {
        data.put(name, value)
    }

    void setPassword(String password){
    }
}

@Canonical
class Meta {
    String location
    ZonedDateTime created
    ZonedDateTime lastModified
    String version
    String resourceType
}

@Canonical
class UserFragmentList{
    List<String> schemas
    int totalResults

    @JsonProperty("Resources")
    List<MemUser> Resources

    UserFragmentList() {
        schemas = ["urn:ietf:params:scim:api:messages:2.0:ListResponse"]
    }

    @JsonProperty("Resources")
    void setResources(List<MemUser> resources) {
        Resources = resources
        totalResults = resources ? resources.size() : 0
    }
}