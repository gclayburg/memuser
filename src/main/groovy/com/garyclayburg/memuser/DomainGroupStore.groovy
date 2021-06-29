package com.garyclayburg.memuser

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

/**
 * <br><br>
 * Created 2021-06-27 12:45
 *
 * @author Gary Clayburg
 */
@Service
@Slf4j
class DomainGroupStore {
    private DomainUserStore domainUserStore
    Map<String, Map<String, MemGroup>> domain_id_groupMap = [:]

    DomainGroupStore(DomainUserStore domainUserStore) {
        this.domainUserStore = domainUserStore
    }

    void put(String domain, MemGroup memGroup) throws InvalidGroupChangeException {
        def id_groupMap = domain_id_groupMap.get(domain)
        if (memGroup.id == null) {
            memGroup.id = UUID.randomUUID().toString()
        }
        checkGroupMembers(memGroup, domain)
        addGroupMembers(memGroup,domain)
        if (!id_groupMap) {
            domain_id_groupMap.put(domain, [(memGroup.id): memGroup])
        } else {
            id_groupMap.put(memGroup.id, memGroup)
            domain_id_groupMap.put(domain, id_groupMap)
        }
    }

    void checkGroupMembers(MemGroup memGroup,String domain) throws InvalidGroupChangeException {
        for (Members member: memGroup.members) {
            def memUser = domainUserStore.getById(domain, member.value)
            if (memUser == null) {
                throw new InvalidGroupChangeException("Cannot change group ${memGroup.displayName}. User [id=${member.value}, display=${member.display}] does not exist")
            }
        }
    }

    void addGroupMembers(MemGroup memGroup, String domain) {
        for (Members member: memGroup.members) {
            def memUser = domainUserStore.getById(domain,member.value)
            if (memUser == null) {
                log.warn("User id ${member.value} does not exist, but it exists in Group ${memGroup.displayName} anyway")
                //its possible that a user might have  been deleted before we are able to add this
                // group to its "groups" attribute.  Maybe we should do this check and add in a transaction?
            } else {
                memUser.addGroup(new UserGroup(value: memGroup.id,display: memGroup.displayName))
            }
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
