package com.garyclayburg.memuser

import com.fasterxml.jackson.databind.ObjectMapper
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

    def "filter user results with closure"(){
        given:
        DomainUserStore domainUserStore = new DomainUserStore()
        def memUser1 = new MemUser(userName: 'testuser1',externalId: 'dude')
        def memUser2 = new MemUser(userName: 'testuser2',active: true)
        def testdom = 'testdom'

        when: 'add user1 user2'
        domainUserStore.putId(testdom,'1',memUser1)
        domainUserStore.putId(testdom,'2',memUser2)
        domainUserStore.putUserName(testdom,'testuser1',memUser1)
        domainUserStore.putUserName(testdom,'testuser2',memUser2)

        then:
        domainUserStore.size(testdom) == 2

        when: 'get with filter'
        def listUsers = domainUserStore.findFilter('testdom',{it.active})
        then:
        listUsers.size() == 1
        listUsers[0].userName == 'testuser2'

        when: 'get with externalid filter'
        def listUsersExternal = domainUserStore.findFilter('testdom',{it.externalId == 'dude'})
        then:
        listUsersExternal.size() == 1
        listUsersExternal[0].userName == 'testuser1'

        when: ' memuser has arbitrary attribute'
        String unknownSoldier = """
{
  "id": "3",
  "userName": "soldier42",
  "dogtagid": "7",
  "homeaddr": {
    "street": "park ave",
    "city": "springfield"
  }
}
"""
        ObjectMapper mapper = new ObjectMapper()
        def memUserSoldier = mapper.readValue(unknownSoldier, MemUser.class)
        domainUserStore.putId(testdom,'3',memUserSoldier)
        domainUserStore.putUserName(testdom,memUserSoldier.userName,memUserSoldier)

        then:
        def listUserSoldier = domainUserStore.findFilter(testdom,{it.dogtagid == '7'})
        listUserSoldier.size() == 1
        listUserSoldier[0].userName == 'soldier42'
        domainUserStore.findFilter(testdom,{it.homeaddr?.city == 'springfield'}).size() == 1
        domainUserStore.findFilter(testdom,{it.homeaddr?.city?.equals('springfield')}).size() == 1
        domainUserStore.findFilter(testdom,{it.homeaddr?.city == 'springfield'})[0].userName == 'soldier42'

    }
}
