package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import com.unboundid.scim2.common.utils.JsonUtils
import com.unboundid.scim2.common.utils.MapperFactory
import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
@EnableConfigurationProperties(MemuserSettings)
class MemuserApplication {
    static void main(String[] args) {
        MapperFactory mf = new MapperFactory()
        def map1 = [:]
        map1[MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES] = true
        map1[MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES] = true
        mf.setMapperCustomFeatures(map1)
        JsonUtils.setCustomMapperFactory(mf)
        SpringApplication.run(MemuserApplication, args)
    }
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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
        JavaTimeModule javaTimeModule = new JavaTimeModule()
        objectMapper.setTimeZone(TimeZone.getDefault())
        javaTimeModule.addSerializer(ZonedDateTime.class,
                new ZonedDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")))
        objectMapper.registerModule(javaTimeModule)
        objectMapper.configure(SerializationFeature.
                WRITE_DATES_AS_TIMESTAMPS, false)
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, true)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false) //
        /*
microsoft likes to send a patch request with extra fields like "name"
"name" exists neither in the spec nor PatchOperation.class, e.g.
{
    "schemas": [
        "urn:ietf:params:scim:api:messages:2.0:PatchOp"
    ],
    "Operations": [
        {
            "name": "addMember",
            "op": "add",
            "path": "members",
            "value": [
            	{
            		"displayName":"new User",
            		"value":"{{id4}}"
            	}
            	]

        }
    ]
}
         */
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
                if (i % 1000 == 0) {
                    log.info("loading users: ${i}/${memuserSettings.userCount}")
                }
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

    @GetMapping(value = ['/ServiceProviderConfig', '/serviceConfiguration'], produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = '*')
    def getServiceProviderConfig() {
        return multiDomainUserController.getServiceProviderConfig()
    }

    @GetMapping('/Users')
    @CrossOrigin(origins = '*', exposedHeaders = ['Link', 'x-total-count'])
    ResponseEntity<ResourcesList> getUsers(HttpServletRequest request, Pageable pageable) {
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
class MemUser extends MemScimResource {
    String userName
    boolean active
    HashSet<UserGroup> groups
    protected Map<String, Object> data = [:]

    @JsonAnyGetter
    Map<String, Object> getData() {
        data
    }

    @JsonAnySetter
    void setData(String name, Object value) {
        data.put(name, value)
    }

    /**
     * This is needed to enable shorthand access to unknown data fields.  Because of this
     * we can use filters containing data fields as if they were fields of MemUser itself, e.g.
     * <pre>
     *{it.dogtagid == '7'} </pre>
     * instead of the client needing to know the internals of MemUser and that it was actually stored in 'data':
     * <pre>
     *{it.data.dogtagid == '7'} </pre>
     * @param name the field stored in data
     * @return the value of the field
     */
    Object get(String name) {
        return data.get(name)
    }

    void setPassword(String password) {}  //well, its secure anyway
    void addGroup(UserGroup userGroup) {
        if (groups == null) {
            groups = []
        }
        groups.add(userGroup)
    }
}

@Canonical
class Meta {
    String location, version, resourceType
    ZonedDateTime created, lastModified
}

