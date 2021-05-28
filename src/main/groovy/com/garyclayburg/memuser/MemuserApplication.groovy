package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Slf4j
@SpringBootApplication
class MemuserApplication {
    static void main(String[] args) {
        SpringApplication.run(MemuserApplication, args)
    }
}

@Component
@ConfigurationProperties(prefix = 'memuser')
class MemuserSettings {
    boolean showHeaders = false
    int userCount = 0
}

// thank you microsoft for encouraging case-insensitive URI path matching
@Configuration
class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    void configurePathMatch(PathMatchConfigurer configurer) {
        AntPathMatcher matcher = new AntPathMatcher()
        matcher.setCaseSensitive(false)
        configurer.setPathMatcher(matcher)
    }
}

@Configuration
class ConfigMe {
    @Bean
    @Primary
    ObjectMapper serializingObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper()
        JavaTimeModule javaTimeModule = new JavaTimeModule()
        javaTimeModule.addSerializer(ZonedDateTime.class,
                new ZonedDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")))
        objectMapper.registerModule(javaTimeModule)
        objectMapper.configure(SerializationFeature.
                WRITE_DATES_AS_TIMESTAMPS, false)
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        objectMapper
    }
}

@Component
@Slf4j
class PreloadUsers {

    MemuserSettings memuserSettings
    UserController userController

    @Autowired
    PreloadUsers(MemuserSettings memuserSettings, UserController userController) {
        this.memuserSettings = memuserSettings
        this.userController = userController
    }

    @PostConstruct
    void load() {
        if (memuserSettings.userCount != 0) {
            log.info("loading users: $memuserSettings.userCount")
            for (int i = 0; i < memuserSettings.userCount; i++) {
                def newuser = new MemUser(userName: 'george' + (i + 1))
                newuser.setData('firstname', 'George')
                newuser.setData('lastname', 'Washington')
                userController.addUser(null, newuser)
            }
            log.info("loading users: $memuserSettings.userCount DONE")
        }
    }
}

@Slf4j
@RestController
@RequestMapping('/api/v2')
class UserController {
    public static final String X_FORWARDED_PROTO = 'X-Forwarded-Proto'
    public static final String X_FORWARDED_HOST = 'X-Forwarded-Host'
    public static final String DEFAULT_DOMAIN1 = 'defaultDomain1'

    MemuserSettings memuserSettings
    MultiDomainUserController multiDomainUserController

    @Autowired
    UserController(MemuserSettings memuserSettings, MultiDomainUserController multiDomainUserController) {
        this.memuserSettings = memuserSettings
        this.multiDomainUserController = multiDomainUserController
    }

    @GetMapping(value = '/ServiceProviderConfig', produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = '*')
    def getServiceProviderConfig() {
        return multiDomainUserController.getServiceProviderConfig()
    }

    @GetMapping('/Users')
    @CrossOrigin(origins = '*', exposedHeaders = ['Link', 'x-total-count'])
    ResponseEntity<UserFragmentList> getUsers(HttpServletRequest request, Pageable pageable) {
        return multiDomainUserController.getUsers(request, pageable, DEFAULT_DOMAIN1)
    }

    @PostMapping('/Users')
    @CrossOrigin(origins = '*')
    def addUser(HttpServletRequest request, @RequestBody MemUser memUser) {
        return multiDomainUserController.addUser(request, memUser, DEFAULT_DOMAIN1)
    }

    @DeleteMapping('/Users')
    @CrossOrigin(origins = '*')
    def deleteAllUsers() {
        return multiDomainUserController.deleteAllUsers(DEFAULT_DOMAIN1)
    }

    @PutMapping('/Users/{id}')
    @CrossOrigin(origins = '*')
    def putUser(HttpServletRequest request, @RequestBody MemUser memUser, @PathVariable('id') String id) {
        return multiDomainUserController.putUser(request, memUser, id, DEFAULT_DOMAIN1)
    }

    @GetMapping('/Users/{id}')
    @CrossOrigin(origins = '*')
    def getUser(HttpServletRequest request, @PathVariable('id') String id) {
        return multiDomainUserController.getUser(request, id, DEFAULT_DOMAIN1)
    }

    @DeleteMapping('/Users/{id}')
    @CrossOrigin(origins = '*')
    def deleteUser(@PathVariable('id') String id) {
        return multiDomainUserController.deleteUser(id, DEFAULT_DOMAIN1)
    }
}

@Canonical
class MemUser {
    String id, userName
    String[] schemas
    Meta meta
    boolean active
    protected Map<String, Object> data = [:]

    @JsonAnyGetter
    Map<String, Object> getData() {
        data
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
    int itemsPerPage
    int startIndex

    @JsonProperty('Resources')
    List<MemUser> Resources

    @JsonProperty('Resources')
    void setResources(resources) {
        Resources = resources
    }
}
