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
class DomainUserStoreSpec extends Specification {

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

    def "store it"() {
        given:
        DomainUserStore domainUserStore = new DomainUserStore()
        def testDomain = 'testDomain'
        def testname = 'testname'
        def memUser = new MemUser(userName: testname)
        expect:
        domainUserStore.size(testDomain) == 0

        when: 'add a user'
        domainUserStore.putId(testDomain, '1', memUser)
        domainUserStore.putUserName(testDomain, testname, memUser)
        then:
        domainUserStore.size(testDomain) == 1
        domainUserStore.getById(testDomain, '1') == memUser
        domainUserStore.getByUserName(testDomain, testname) == memUser
        when: 'put again'
        domainUserStore.putId(testDomain, '1', memUser)
        then: 'no change'
        domainUserStore.size(testDomain) == 1
        domainUserStore.getById(testDomain, '1') == memUser
        domainUserStore.getByUserName(testDomain, testname) == memUser

        when: 'remove by id'
        domainUserStore.removeById(testname, '1')
        domainUserStore.removeByUserName(testname, testname)

        then:
        domainUserStore.size() == 0
    }
}
