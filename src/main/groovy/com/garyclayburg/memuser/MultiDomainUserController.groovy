package com.garyclayburg.memuser

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

import javax.servlet.http.HttpServletRequest
import java.time.ZonedDateTime

/**
 * <br><br>
 * Created 2020-03-23 11:32
 *
 * @author Gary Clayburg
 */

class DomainUserStore {
    Map<String, Map<String, MemUser>> domain_id_userMap = [:]
    Map<String, Map<String, MemUser>> domain_userName_userMap = [:]

    int size(String domain) {
        int size = 0
        if (domain_id_userMap.get(domain)) {
            size = domain_id_userMap.get(domain).size()
        }
        return size
    }

    MemUser putId(String domain, String id, MemUser memUser) {
        def id_userMap = domain_id_userMap.get(domain)
        MemUser previousMemuser = null
        if (!id_userMap) {
            domain_id_userMap.put(domain, [(id): memUser])
        } else {
            previousMemuser = id_userMap.put(id, memUser)
            domain_id_userMap.put(domain, id_userMap)
        }
        return previousMemuser
    }

    MemUser putUserName(String domain, String userName, MemUser memUser) {
        def userName_userMap = domain_userName_userMap.get(domain)
        MemUser previousMemuser = null
        if (!userName_userMap) {
            domain_userName_userMap.put(domain, [(userName): memUser])
        } else {
            previousMemuser = userName_userMap.put(userName, memUser)
            domain_userName_userMap.put(domain, userName_userMap)
        }
        return previousMemuser
    }

    MemUser getById(String domain, String id) {
        MemUser foundMemUser = null
        def id_userMap = domain_id_userMap.get(domain)
        if (id_userMap) {
            foundMemUser = id_userMap.get(id)
        }
        return foundMemUser
    }

    MemUser getByUserName(String domain, String userName) {
        MemUser foundMemUser = null
        def userName_userMap = domain_userName_userMap.get(domain)
        if (userName_userMap) {
            foundMemUser = userName_userMap.get(userName)
        }
        return foundMemUser
    }

    MemUser removeByUserName(String domain, String userName) {
        MemUser previousMemuser = null
        def userName_userMap = domain_userName_userMap.get(domain)
        if (userName_userMap) {
            previousMemuser = userName_userMap.remove(userName)
        }
        return previousMemuser
    }

    MemUser removeById(String domain, String id) {
        MemUser previousMemuser = null
        def id_userMap = domain_id_userMap.get(domain)
        if (id_userMap) {
            previousMemuser = id_userMap.remove(id)
        }
        return previousMemuser
    }

    void wipeClean(String domain) {
        def id_userMap = domain_id_userMap.get(domain)
        if (id_userMap) {
            Map<String, MemUser> emptyMap = [:]
            domain_id_userMap.put(domain, emptyMap)
        }
        def userName_userMap = domain_userName_userMap.get(domain)
        if (userName_userMap) {
            Map<String, MemUser> emptyMap = [:]
            domain_userName_userMap.put(domain, emptyMap)
        }
    }

    Collection<MemUser> getValues(String domain) {
        Collection<MemUser> userCollection = []
        def id_userMap = domain_id_userMap.get(domain)
        if (id_userMap) {
            userCollection = id_userMap.values()
        }
        return userCollection
    }
}

@Slf4j
@RestController
@RequestMapping('/api/multiv2')
class MultiDomainUserController {
    public static final String X_FORWARDED_PROTO = 'X-Forwarded-Proto'
    public static final String X_FORWARDED_HOST = 'X-Forwarded-Host'
    public static final String START_INDEX = 'startIndex'
    public static final String COUNT = 'count'

    MemuserSettings memuserSettings
    DomainUserStore domainUserStore

    @Autowired
    MultiDomainUserController(MemuserSettings memuserSettings) {
        this.memuserSettings = memuserSettings
        this.domainUserStore = new DomainUserStore()
    }

    @GetMapping(value = '/{domain}/ServiceProviderConfig', produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = '*')
    def getServiceProviderConfig() {
        new File(getClass().getResource('/scim/serviceprovider.json').toURI()).text
    }

    private static Pageable overrideScimPageable(HttpServletRequest request, Pageable pageable) {
        Pageable modifiedPageable = pageable
        try {
            Map<String, String[]> parameterMap = request.parameterMap
            if (parameterMap != null && parameterMap.get(START_INDEX) != null && parameterMap.get(COUNT) != null) {
                int startIndex = Integer.parseInt(parameterMap.get(START_INDEX)[0]) - 1
                //SCIM RFC7644 uses 1 based pages, while spring data uses 0 based
                int itemsPerPage = Integer.parseInt(parameterMap.get(COUNT)[0])
                int pageNumber = (int) (startIndex / itemsPerPage)
                modifiedPageable = new PageRequest(pageNumber, itemsPerPage)
            }
        } catch (NumberFormatException ignored) {
            log.warn('invalid SCIM page parameters {}', request.queryString)
        }
        modifiedPageable
    }

    @GetMapping('/{domain}/Users')
    @CrossOrigin(origins = '*', exposedHeaders = ['Link', 'x-total-count'])
    ResponseEntity<UserFragmentList> getUsers(HttpServletRequest request, Pageable pageable,
                                              @PathVariable(value = 'domain', required = true) String domain) {
        showHeaders(request)
        Pageable overriddenPageable = overrideScimPageable(request, pageable)
        def startIndex = (overriddenPageable.pageNumber) * overriddenPageable.pageSize
        UserFragmentList userFragmentList
        domainUserStore.size(domain)
        if (startIndex < domainUserStore.size(domain)) {
            def endIndex = startIndex + overriddenPageable.pageSize
            def adjustedPageSize = overriddenPageable.pageSize
            if (endIndex >= domainUserStore.size(domain)) {
                endIndex = domainUserStore.size(domain)
                adjustedPageSize = endIndex - startIndex
            }
            def listPage = domainUserStore.getValues(domain).toList().subList(startIndex, endIndex)
            userFragmentList = new UserFragmentList(
                    totalResults: domainUserStore.size(domain),
                    itemsPerPage: adjustedPageSize,
                    startIndex: startIndex + 1)
            userFragmentList.resources = overideLocation(listPage, request)
            generatePage(listPage, overriddenPageable, userFragmentList, domain)
        } else {
            userFragmentList = new UserFragmentList(
                    totalResults: 0,
                    itemsPerPage: 0,
                    startIndex: 0,
                    resources: [])
            generatePage([], overriddenPageable, userFragmentList, domain)
        }
    }

    private ResponseEntity<UserFragmentList> generatePage(List<MemUser> listPage,
                                                          Pageable pageable,
                                                          UserFragmentList userFragmentList,
                                                          String domain) {
        def pageImpl = new PageImpl<>(listPage, pageable, domainUserStore.size(domain))
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(), pageImpl)
        ResponseEntity.ok().headers(headers).body(userFragmentList)
    }

    @PostMapping('/{domain}/Users')
    @CrossOrigin(origins = '*')
    def addUser(HttpServletRequest request, @RequestBody MemUser memUser,
                @PathVariable(value = 'domain', required = true) String domain) {
        showHeaders(request)
        if (memUser.userName != null) {

            if (!domainUserStore.getByUserName(domain, memUser.userName)) {
                memUser.setId(UUID.randomUUID().toString())
                def now = ZonedDateTime.now()
                memUser.setMeta(
                        new Meta(location: request != null ? filterProxiedURL(request, request.requestURL.append('/')
                                .append(memUser.id).toString()) : 'http://example.com/Users/' + memUser.id,
                                created: now,
                                lastModified: now,
                                resourceType: 'User',))
                memUser.schemas ?: memUser.setSchemas('urn:ietf:params:scim:schemas:core:2.0:User')
                domainUserStore.putId(domain, memUser.id, memUser)
                domainUserStore.putUserName(domain, memUser.userName, memUser)
                return new ResponseEntity<>((MemUser) memUser, HttpStatus.CREATED)
            }
            return new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT)
        }
        return new ResponseEntity<>((MemUser) null, HttpStatus.BAD_REQUEST)
    }

    @DeleteMapping('/{domain}/Users')
    @CrossOrigin(origins = '*')
    def deleteAllUsers(@PathVariable(value = 'domain', required = true) String domain) {
        domainUserStore.wipeClean(domain)
        new ResponseEntity<>((MemUser) null, HttpStatus.NO_CONTENT)
    }

    @PutMapping('/{domain}/Users/{id}')
    @CrossOrigin(origins = '*')
    def putUser(HttpServletRequest request, @RequestBody MemUser memUser, @PathVariable('id') String id,
                @PathVariable(value = 'domain', required = true) String domain) {
        showHeaders(request)
        if (memUser.userName != null) {

            if (domainUserStore.getById(domain, id) != null) {
                def existingUserUsername = domainUserStore.getByUserName(domain, memUser.userName)
                if (existingUserUsername != null && existingUserUsername.id != id) {
                    return new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT) //tried to duplicate userName
                }
                def meta = domainUserStore.getById(domain, id).meta
                meta.lastModified = ZonedDateTime.now()
                memUser.meta = meta
                memUser.meta.location = filterProxiedURL(request, request.requestURL.toString())
                domainUserStore.removeByUserName(domain, domainUserStore.getById(domain, id).userName)
                //userName for id may have changed
                memUser.setId(id) //preserve original id
                domainUserStore.putId(domain, id, memUser)
                domainUserStore.putUserName(domain, memUser.userName, memUser)
                return new ResponseEntity<>((MemUser) memUser, HttpStatus.OK)
            }
            new ResponseEntity<>((MemUser) null, HttpStatus.CONFLICT)
        }
        return new ResponseEntity<>((MemUser) null, HttpStatus.BAD_REQUEST)
    }

    @GetMapping('/{domain}/Users/{id}')
    @CrossOrigin(origins = '*')
    def getUser(HttpServletRequest request, @PathVariable('id') String id,
                @PathVariable(value = 'domain', required = true) String domain) {
        showHeaders(request)
        def memUser = domainUserStore.getById(domain, id)
        if (memUser != null) {
            memUser.meta.location = filterProxiedURL(request, request.requestURL.toString())
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
        showHeaders(request)
        if (memUser != null) {
            def location = request.requestURL.append('/')
                    .append(memUser.id).toString()
                    .replaceFirst('Users//', 'Users/')
            memUser.meta.location = filterProxiedURL(request, location)
        }
        memUser
    }

    static String filterProxiedURL(HttpServletRequest request, String locationRaw) {
        String location = locationRaw
        if (isForwardedRequest(request)) {
            location = request.getHeader(X_FORWARDED_PROTO) + '://' +
                    request.getHeader(X_FORWARDED_HOST) + extractURI(locationRaw)
        }
        location
    }

    static String extractURI(String locationRaw) {
        locationRaw.replaceFirst('^http.*//[^/]*', '')
    }

    private void showHeaders(HttpServletRequest request) {
        if (memuserSettings.showHeaders) {
            Enumeration<String> headerNames = request.headerNames
            log.info(request.method + ' ' + request.requestURL + '   headers:')
            while (headerNames?.hasMoreElements()) {
                String headerName = headerNames.nextElement()
                log.info(headerName + ': ' + request.getHeader(headerName))
            }
        }
    }

    @DeleteMapping('/{domain}/Users/{id}')
    @CrossOrigin(origins = '*')
    def deleteUser(@PathVariable('id') String id,
                   @PathVariable(value = 'domain', required = true) String domain) {
        def user = domainUserStore.getById(domain, id)
        if (user != null) {
            log.info("delete: $id userName: ${user.userName}")
            domainUserStore.removeByUserName(domain, user.userName)
            domainUserStore.removeById(domain, id)
            return new ResponseEntity<>((MemUser) null, HttpStatus.NO_CONTENT)
        }
        return new ResponseEntity<>((MemUser) null, HttpStatus.NOT_FOUND)
    }

    static boolean isForwardedRequest(HttpServletRequest request) {
        request.getHeader(X_FORWARDED_PROTO) != null && request.getHeader(X_FORWARDED_HOST) != null
    }
}
