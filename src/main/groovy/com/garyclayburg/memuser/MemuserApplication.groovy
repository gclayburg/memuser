package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest
import java.time.ZonedDateTime

@Slf4j
@SpringBootApplication
class MemuserApplication {
    static void main(String[] args) {
        SpringApplication.run(MemuserApplication, args)
    }
}

@Slf4j
@RestController
@RequestMapping('/api/v2')
class UserController {

    Map<String, MemUser> userMap = [:]
    Map<String, MemUser> userNameMap = [:]

    @GetMapping(value = '/ServiceProviderConfig', produces = MediaType.APPLICATION_JSON_VALUE)
    def getServiceProviderConfig(){
        new File(getClass().getResource('/scim/serviceprovider.json').toURI()).text
    }
    @GetMapping('/Users')
    def getUsers() {
        UserFragmentList userFragmentList = new UserFragmentList()
        List<MemUser> memUserList = new ArrayList<>(userMap.values())
        userFragmentList.setResources(memUserList)
        return userFragmentList
    }

    @PostMapping('/Users')
    def addUser(HttpServletRequest request, @RequestBody MemUser memUser) {
        if (!userNameMap.get(memUser.userName)) {
            memUser.setId(UUID.randomUUID().toString())
            def now = ZonedDateTime.now()
            memUser.setMeta(
                    new Meta(location: request.requestURL.append('/').append(memUser.id).toString(),
                            created: now,
                            lastModified: now,
                            resourceType: 'User',))
            memUser.schemas ?: memUser.setSchemas('urn:ietf:params:scim:schemas:core:2.0:User')
            userMap.put(memUser.id, memUser)
            userNameMap.put(memUser.userName, memUser)
            return new ResponseEntity<>((MemUser) memUser, HttpStatus.CREATED)
        }
        return new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT)
    }

    @PutMapping('/Users/{id}')
    def putUser(@RequestBody MemUser memUser, @PathVariable('id') String id) {
        if (userMap.get(id) != null ) {
            def meta = userMap.get(id).meta
            meta.lastModified = ZonedDateTime.now()
            memUser.meta = meta
            userNameMap.remove(userMap.get(id).userName) //userName for id may have changed
            memUser.setId(id) //preserve original id
            userMap.put(id, memUser)
            userNameMap.put(memUser.userName, memUser)
            return new ResponseEntity<>((MemUser) memUser, HttpStatus.OK)
        }
        return new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT)
    }

    @GetMapping('/Users/{id}')
    def getUser(@PathVariable('id') String id) {
        userMap.get(id) ?: new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
    }

    @DeleteMapping('/Users/{id}')
    def deleteUser(@PathVariable('id') String id){
        def user = userMap.get(id)
        if (user != null){
            log.info("delete: $id userName: ${user.getUserName()}")
            userMap.remove(id)
            userNameMap.remove(user.getUserName())
            return new ResponseEntity<>((MemUser) null, HttpStatus.NO_CONTENT)
        } else{
            return new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
        }
    }
}

@Canonical
class MemUser {
    String id, userName
    String[] schemas
    Meta meta
    protected Map<String, Object> data = [:]

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
    List<String> schemas = ['urn:ietf:params:scim:api:messages:2.0:ListResponse']
    int totalResults

    @JsonProperty('Resources')
    List<MemUser> Resources

    @JsonProperty('Resources')
    void setResources(List<MemUser> resources) {
        Resources = resources
        totalResults = resources ? resources.size() : 0
    }
}
