package com.garyclayburg.memuser

import org.springframework.stereotype.Service

/**
 * <br><br>
 * Created 2021-06-27 12:45
 *
 * @author Gary Clayburg
 */
@Service
class DomainGroupStore {
    private DomainUserStore domainUserStore
    Map<String, Map<String, MemGroup>> domain_id_groupMap = [:]

    DomainGroupStore(DomainUserStore domainUserStore) {
        this.domainUserStore = domainUserStore
    }

    void put(String domain, MemGroup memGroup) {
        def id_groupMap = domain_id_groupMap.get(domain)
        if (memGroup.id == null) {
            memGroup.id = UUID.randomUUID().toString()
        }
        if (!id_groupMap) {
            domain_id_groupMap.put(domain, [(memGroup.id): memGroup])
        } else {
            id_groupMap.put(memGroup.id, memGroup)
            domain_id_groupMap.put(domain, id_groupMap)
        }
    }

    int size(String domain) {
        int size = 0
        if (domain_id_groupMap.get(domain)) {
            size = domain_id_groupMap.get(domain).size()
        }
        size
    }

    MemGroup get(String domain, String id) {
        MemGroup foundMemGroup = null
        def id_groupMap = domain_id_groupMap.get(domain)
        if (id_groupMap) {
            foundMemGroup = id_groupMap.get(id)
        }
        foundMemGroup
    }

    Collection<MemGroup> getValues(String domain) {
        Collection<MemGroup> groupCollection = []
        def id_groupMap = domain_id_groupMap.get(domain)
        if (id_groupMap) {
            groupCollection = id_groupMap.values()
        }
        return groupCollection
    }

    void removeById(String domain, String id) {
        def id_userMap = domain_id_groupMap.get(domain)
        if (id_userMap) {
            id_userMap.remove(id)
        }
    }
}
