package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest
import java.time.LocalDate
import java.time.ZonedDateTime

@Slf4j
@SpringBootApplication
class MemuserApplication {
    static void main(String[] args) {
        SpringApplication.run(MemuserApplication, args)
    }
}

@Configuration
class ConfigMe {
    @Bean
    @Primary
    public ObjectMapper serializingObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
        JavaTimeModule javaTimeModule = new JavaTimeModule()
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer())
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer())
        objectMapper.registerModule(javaTimeModule)
        objectMapper.configure(SerializationFeature.
                WRITE_DATES_AS_TIMESTAMPS, false)
        return objectMapper
    }
}

@Slf4j
@RestController
@RequestMapping('/api/v2')
class UserController {
    Map<String, MemUser> userMap = [:]
    Map<String, MemUser> userNameMap = [:]

    @GetMapping(value = '/ServiceProviderConfig', produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    def getServiceProviderConfig() {
        new File(getClass().getResource('/scim/serviceprovider.json').toURI()).text
    }

    @GetMapping('/Users')
    @CrossOrigin(origins = "*")
    def getUsers(HttpServletRequest request) {
        UserFragmentList userFragmentList = new UserFragmentList()
        userFragmentList.resources = overideLocation(userMap.values(), request)
        return userFragmentList
    }

    @PostMapping('/Users')
    @CrossOrigin(origins = "*")
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

    @DeleteMapping('/Users')
    @CrossOrigin(origins = "*")
    def deleteAllUsers() {
        userMap = [:]
        userNameMap = [:]
        return new ResponseEntity<>((MemUser) null, HttpStatus.NO_CONTENT)
    }

    @PutMapping('/Users/{id}')
    @CrossOrigin(origins = "*")
    def putUser(@RequestBody MemUser memUser, @PathVariable('id') String id) {
        if (userMap.get(id) != null && memUser.userName != null) {
            def existingUserUsername = userNameMap.get(memUser.userName)
            if (existingUserUsername != null && existingUserUsername.id != id) {
                return new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT) //tried to duplicate userName
            }
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
    @CrossOrigin(origins = "*")
    def getUser(HttpServletRequest request, @PathVariable('id') String id) {
        def memUser = userMap.get(id)
        if (memUser != null) {
            memUser.meta.location = request.requestURL
            memUser
        } else {
            return new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
        }
    }

    def overideLocation(Collection<MemUser> memUsers, HttpServletRequest request) {
        def locationFixedUsers = []
        for (MemUser memUser1 : memUsers) {
            locationFixedUsers += overrideLocation(memUser1, request)
        }
        locationFixedUsers
    }

    MemUser overrideLocation(MemUser memUser, HttpServletRequest request) {
        if (memUser != null) {
            def string = request.requestURL.append('/').append(memUser.id).toString()
            memUser.meta.location = string.replaceFirst('Users//', 'Users/')
        }
        memUser
    }

    @DeleteMapping('/Users/{id}')
    @CrossOrigin(origins = "*")
    def deleteUser(@PathVariable('id') String id) {
        def user = userMap.get(id)
        if (user != null) {
            log.info("delete: $id userName: ${user.getUserName()}")
            userMap.remove(id)
            userNameMap.remove(user.getUserName())
            return new ResponseEntity<>((MemUser) null, HttpStatus.NO_CONTENT)
        } else {
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

    void setPassword(String password) {}  //well, its secure anyway
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
    void setResources(resources) {
        Resources = resources
        totalResults = resources ? resources.size() : 0
    }
}
