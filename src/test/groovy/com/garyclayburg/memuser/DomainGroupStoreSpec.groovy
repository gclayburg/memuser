package com.garyclayburg.memuser

import groovy.util.logging.Slf4j
import spock.lang.Specification


/**
 * <br><br>
 * Created 2020-03-23 12:36
 *
 * @author Gary Clayburg
 */
@Slf4j
class DomainGroupStoreSpec extends Specification {

    def "setup"() {
        log.info("setup spec")
    }

    def cleanup() {
        log.info("cleaning spec")
    }

    def setupSpec() {}

    def cleanupSpec() {}

    def "contextok"() {
        expect:
        true
    }

    def "simple group"() {
        when: 'create group'
        MemGroup memGroup = new MemGroup(displayName: "group one")

        then:
        memGroup.displayName == 'group one'

    }

    def "store group data"() {
        given:
        DomainUserStore domainUserStore = new DomainUserStore()
        DomainGroupStore domainGroupStore = new DomainGroupStore(domainUserStore)
        def testDomain = 'testDomain'
        def testname = 'testname'
        def memUser = new MemUser(userName: testname,id: '1')
        expect:
        domainUserStore.size(testDomain) == 0

        when: 'add a user'
        domainUserStore.putId(testDomain, memUser.id, memUser)
        domainUserStore.putUserName(testDomain, testname, memUser)
        then:
        domainUserStore.size(testDomain) == 1
        domainUserStore.getById(testDomain, memUser.id) == memUser
        domainUserStore.getByUserName(testDomain, testname) == memUser

        when: 'create empty group (POST)'
        MemGroup memGroup = new MemGroup(displayName: "group one")
        domainGroupStore.put(testDomain, memGroup)
        then:
        memGroup.displayName == 'group one'
        memGroup.id != null
        domainGroupStore.size(testDomain) == 1

        when: 'replace group with one holding 1 user (PUT)'
        MemGroup memGroup1 = new MemGroup(displayName: "group one")
        memGroup1.id = memGroup.id
        memGroup1.setMembers([new Members(
                value: memUser.id,
                display: "someuser")])
        domainGroupStore.put(testDomain, memGroup1)
        then:
        domainGroupStore.size(testDomain) == 1

        when: 'get group (GET id)'
        MemGroup memGroup1Returned = domainGroupStore.get(testDomain, memGroup1.id)
        then:
        memGroup1Returned.displayName == 'group one'

    }

}
