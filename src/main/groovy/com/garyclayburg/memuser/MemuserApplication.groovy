package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest
import java.time.ZonedDateTime

@Slf4j
@SpringBootApplication
class MemuserApplication {
    static void main(String[] args) {
        def context = SpringApplication.run(MemuserApplication, args)
        def bean = context.getBean("versionPrint")
        bean.printVersion()
    }
}

@Slf4j
@Component
class VersionPrint{
    @Value('${info.app.version}')
    private String version

    @Autowired
    private ApplicationContext context

    public void printVersion(){
        Environment env = context.environment
        log.info('\n----------------------------------------------------------\n\t' +
                '\'{}\' is ready for e-business! \t\t\t version: {}\n\t' +
                'Local: \t\thttp://localhost:{} \t\t\t {}\n\t' +
                'External: \thttp://{}:{}\n----------------------------------------------------------',
                env.getProperty('spring.application.name'),
                env.getProperty('info.app.version'),
                env.getProperty('server.port'),
                env.getProperty('build.org.label-schema.description'),
                InetAddress.localHost.hostAddress,
                env.getProperty('server.port'))
        log.info("  now running versions "+version)
    }

}

@Slf4j
@RestController
@RequestMapping('/api/v1')
class UserController {

    Map<String, MemUser> userMap = [:]
    Map<String, MemUser> userNameMap = [:]

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
        if (userMap.get(memUser?.id) != null && memUser.id == id) {
            def meta = userMap.get(memUser.id).meta
            meta.lastModified = ZonedDateTime.now()
            memUser.meta = meta
            userNameMap.remove(userMap.get(memUser.id).userName) //userName for id may have changed
            userMap.put(memUser.id, memUser)
            userNameMap.put(memUser.userName, memUser)
            return new ResponseEntity<>((MemUser) memUser, HttpStatus.OK)
        }
        return new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT)
    }

    @GetMapping('/Users/{id}')
    def getUser(@PathVariable('id') String id) {
        userMap.get(id) ?: new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
    }
}

@Canonical
class MemUser {
    String id, userName, schemas
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
