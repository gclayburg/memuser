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
    DomainGroupStore domainGroupStore

    @Autowired
    MultiDomainUserController(MemuserSettings memuserSettings,
                              DomainUserStore domainUserStore,
                              DomainGroupStore domainGroupStore) {
        this.domainGroupStore = domainGroupStore
        this.memuserSettings = memuserSettings
        this.domainUserStore = domainUserStore
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

    @GetMapping('/{domain}/Groups')
    @CrossOrigin(origins = '*', exposedHeaders = ['Link', 'x-total-count'])
    ResponseEntity<ResourcesList> getGroups(HttpServletRequest request, Pageable pageable,
                                                @PathVariable(value = 'domain', required = true) String domain) {
        Pageable overriddenPageable = overrideScimPageable(request, pageable)

        ResourcesList resourcesList = new ResourcesList(overriddenPageable,domainGroupStore.size(domain))
        List<MemGroup> listPage = domainGroupStore.getValues(domain,resourcesList)
        resourcesList.resources = overrideLocation(listPage, request)
        generatePage(listPage, overriddenPageable, resourcesList, domain, domainGroupStore.size(domain))
    }

    @GetMapping('/{domain}/Users')
    @CrossOrigin(origins = '*', exposedHeaders = ['Link', 'x-total-count'])
    ResponseEntity<ResourcesList> getUsers(HttpServletRequest request, Pageable pageable,
                                           @PathVariable(value = 'domain', required = true) String domain) {
        showHeaders(request)
        Pageable overriddenPageable = overrideScimPageable(request, pageable)

        ResourcesList userFragmentList = new ResourcesList(overriddenPageable,domainUserStore.size(domain))
        def listPage = domainUserStore.getValues(domain,userFragmentList)
        userFragmentList.resources = overideLocation(listPage, request)
        generatePage(listPage, overriddenPageable, userFragmentList, domain, domainUserStore.size(domain))
    }

    private static ResponseEntity<ResourcesList> generatePage(List<MemScimResource> listPage,
                                                              Pageable pageable,
                                                              ResourcesList resourcesList,
                                                              String domain, int totalSize) {
        def pageImpl = new PageImpl<>(listPage, pageable, totalSize)
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                ServletUriComponentsBuilder.fromCurrentRequest(), pageImpl)
        ResponseEntity.ok().headers(headers).body(resourcesList)
    }

    @PostMapping('/{domain}/Groups')
    @CrossOrigin(origins = '*')
    def addGroup(HttpServletRequest request, @RequestBody MemGroup memGroup,
                 @PathVariable(value = 'domain', required = true) String domain) {
        memGroup.setId(UUID.randomUUID().toString())
        def now = ZonedDateTime.now()
        memGroup.setMeta(
                new Meta(location: request != null ? filterProxiedURL(request, request.requestURL.append('/')
                        .append(memGroup.id).toString()) : 'http://example.com/Groups/' + memGroup.id,
                        created: now,
                        lastModified: now,
                        resourceType: 'Group'))
        memGroup.schemas ?: memGroup.setSchemas('urn:ietf:params:scim:schemas:core:2.0:Group')
        try {
            domainGroupStore.put(domain, memGroup)
            return new ResponseEntity<>((MemGroup) memGroup, HttpStatus.CREATED)
        } catch (InvalidGroupChangeException invalidGroupChangeException) {
            return createError(invalidGroupChangeException.message,HttpStatus.BAD_REQUEST)
        }
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
            return createError("userName ${memUser.userName} already exists",HttpStatus.CONFLICT)
        }
        return createError("userName must be specified",HttpStatus.BAD_REQUEST)
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
                def existingUserByUsername = domainUserStore.getByUserName(domain, memUser.userName)
                if (existingUserByUsername != null && existingUserByUsername.id != id) {
                    return createError("Cannot replace User with a userName that already exists with a different id (id=${existingUserByUsername.id})",HttpStatus.CONFLICT)
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
            return createError("User cannot be replaced because id ${id} does not exist in domain {$domain}",HttpStatus.CONFLICT)
        }
        return createError("userName must be specified",HttpStatus.BAD_REQUEST)
    }

    @PutMapping('/{domain}/Groups/{id}')
    @CrossOrigin(origins = '*')
    def putGroup(HttpServletRequest request, @RequestBody MemGroup memGroup, @PathVariable('id') String id,
                 @PathVariable(value = 'domain', required = true) String domain) {
        showHeaders(request)
        if (domainGroupStore.get(domain, id) != null) {
            def meta = domainGroupStore.get(domain, id).meta
            meta.lastModified = ZonedDateTime.now()
            memGroup.meta = meta
            memGroup.meta.location = filterProxiedURL(request, request.requestURL.toString())
            domainGroupStore.removeById(domain, domainGroupStore.get(domain, id).id)

            memGroup.setId(id) //preserve original id
            try {
                domainGroupStore.put(domain, memGroup)
                return new ResponseEntity<>((MemGroup) memGroup, HttpStatus.OK)
            } catch (InvalidGroupChangeException invalidGroupChangeException) {
                return createError(invalidGroupChangeException.message, HttpStatus.BAD_REQUEST)
            }
        }
        return createError("Group with id ${id} does not exist in domain ${domain}", HttpStatus.CONFLICT)
    }

    @GetMapping('/{domain}/Users/{id}')
    @CrossOrigin(origins = '*')
    def getUser(HttpServletRequest request, @PathVariable('id') String id,
                @PathVariable(value = 'domain', required = true) String domain) {
        showHeaders(request)
        def memUser = domainUserStore.getById(domain, id)
        if (memUser != null) {
            memUser.meta.location = filterProxiedURL(request, request.requestURL.toString())
            return new ResponseEntity<>((MemUser) memUser,HttpStatus.OK)
        } else {
            return createError("User with id ${id} does not exist in domain ${domain}", HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping('/{domain}/Groups/{id}')
    @CrossOrigin(origins = '*')
    def getGroup(HttpServletRequest request, @PathVariable('id') String id,
                 @PathVariable(value = 'domain', required = true) String domain) {
        showHeaders(request)
        def memGroup = domainGroupStore.get(domain, id)
        if (memGroup != null) {
            memGroup.meta.location = filterProxiedURL(request, request.requestURL.toString())
            memGroup
        } else {
            return createError("Group with id ${id} does not exist in domain ${domain}",HttpStatus.NOT_FOUND)
        }
    }

    def overideLocation(Collection<MemUser> memUsers, HttpServletRequest request) {
        def locationFixedUsers = []
        for (MemUser memUser1 : memUsers) {
            locationFixedUsers += overrideLocation(memUser1, request)
        }
        memUsers
    }

    def overrideLocation(Collection<MemGroup> memGroups, HttpServletRequest request) {
        def locationFixedUsers = []
        for (MemGroup memGroup : memGroups) {
            locationFixedUsers += overrideLocation(memGroup, request)
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

    MemGroup overrideLocation(MemGroup memGroup, HttpServletRequest request) {
        showHeaders(request)
        if (memGroup != null) {
            def location = request.requestURL.append('/')
                    .append(memGroup.id).toString()
                    .replaceFirst('Groups//', 'Groups/')
            memGroup.meta.location = filterProxiedURL(request, location)
        }
        memGroup
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
        return createError("User with id ${id} does not exist in domain ${domain}",HttpStatus.NOT_FOUND)
    }

    @DeleteMapping('/{domain}/Groups/{id}')
    @CrossOrigin(origins = '*')
    def deleteGroup(@PathVariable('id') String id,
                    @PathVariable(value = 'domain', required = true) String domain) {
        def group = domainGroupStore.get(domain, id)
        if (group != null) {
            log.info("delete: $id userName: ${group.displayName}")
            domainGroupStore.removeById(domain, id)
            return new ResponseEntity<>((MemUser) null, HttpStatus.NO_CONTENT)
        }
        return createError("Group with id ${id} does not exist in domain ${domain}",HttpStatus.NOT_FOUND)
    }

    private static ResponseEntity<ErrorResponse> createError(String detail, HttpStatus httpStatus) {
        new ResponseEntity<>(new ErrorResponse(
                detail: detail,
                status: httpStatus.value().toString()), httpStatus)
    }

    static boolean isForwardedRequest(HttpServletRequest request) {
        request.getHeader(X_FORWARDED_PROTO) != null && request.getHeader(X_FORWARDED_HOST) != null
    }
}
