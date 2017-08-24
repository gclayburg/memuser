package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
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

@Slf4j
@RestController
@RequestMapping('/api')
class UserController {

    Map<String, MemUser> userMap = new HashMap<>()
    Map<String, MemUser> userNameMap = new HashMap<>()

    @GetMapping("/Users")
    def getUsers() {
        UserFragmentList userFragmentList = new UserFragmentList()
        List<MemUser> memUserList = new ArrayList<>(userMap.values())
        userFragmentList.setResources(memUserList)
        return userFragmentList
    }

    @PostMapping("/Users")
    def createUser(HttpServletRequest request, @RequestBody MemUser memUser) {
        log.info("create")
        if (!userNameMap.get(memUser.userName)) {
            memUser.setId(UUID.randomUUID().toString())
            def now = ZonedDateTime.now()
            memUser.setMeta(
                    new Meta(location: request.getRequestURL().append("/").append(memUser.getId()).toString(), created: now, lastModified: now, resourceType: "User"))
            memUser.schemas ?: memUser.setSchemas("urn:ietf:params:scim:schemas:core:2.0:User")
            userMap.put(memUser.id, memUser)
            userNameMap.put(memUser.userName, memUser)
            return new ResponseEntity<>((MemUser) memUser, HttpStatus.CREATED)
        } else {
            return new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT)
        }
    }

    @PutMapping("/Users/{id}")
    def putUser(@RequestBody MemUser memUser, @PathVariable("id") String id) {
        if (userMap.get(memUser?.id) != null && memUser.id == id) {
            def meta = userMap.get(memUser.id).getMeta()
            meta.setLastModified(ZonedDateTime.now())
            memUser.setMeta(meta)
            userNameMap.remove(userMap.get(memUser.id).userName) //userName for id may have changed
            userMap.put(memUser.id, memUser)
            userNameMap.put(memUser.userName, memUser)
            return new ResponseEntity<>((MemUser) memUser, HttpStatus.OK)
        } else {
            return new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT)
        }
    }

    @GetMapping("/Users/{id}")
    def getUser(@PathVariable("id") String id) {
        userMap.get(id) ?: new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
    }
}

@Canonical
class MemUser {
    String id, userName, schemas
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

    void setPassword(String password) {}
}

@Canonical
class Meta {
    String location, version, resourceType
    ZonedDateTime created, lastModified
}

@Canonical
class UserFragmentList {
    List<String> schemas = ["urn:ietf:params:scim:api:messages:2.0:ListResponse"]
    int totalResults

    @JsonProperty("Resources")
    List<MemUser> Resources

    @JsonProperty("Resources")
    void setResources(List<MemUser> resources) {
        Resources = resources
        totalResults = resources ? resources.size() : 0
    }
}