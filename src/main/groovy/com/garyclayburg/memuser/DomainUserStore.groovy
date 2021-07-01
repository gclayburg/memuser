package com.garyclayburg.memuser

import org.springframework.stereotype.Service

/**
 * <br><br>
 * Created 2020-03-23 11:32
 *
 * @author Gary Clayburg
 */

@Service
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

    List<MemUser> getValues(String domain, UserFragmentList userFragmentList) {
        Collection<MemUser> userCollection = []
        def id_userMap = domain_id_userMap.get(domain)
        if (id_userMap) {
            userCollection = id_userMap.values()
        }
        userCollection.toList().subList(userFragmentList.springStartIndex, userFragmentList.endIndex)
    }
}
