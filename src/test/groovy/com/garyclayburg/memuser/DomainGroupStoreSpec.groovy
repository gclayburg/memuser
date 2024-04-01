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

    def "warning messages"() {
        when:
        def logmsg = """
Mar 31, 2024 4:34:34 PM asciidoctor
WARNING: Configuration property 'memuser.showHeaders' not found.
Watching 409 directories to track changes
WARNING: Configuration property 'memuser.whatisthis' not found.
:asciidoctor (Thread[Execution worker for ':',5,main]) completed. Took 6.399 secs.
"""
        def warningFound = logmsg =~ /WARNING/
        then:
        warningFound

        and: 'we can show the WARNING line'
        def warningPattern = ~/.*WARNING.*/
        warningPattern.matcher('WARNING morestuff3').matches()

        and: 'matcher works'
        "WARNING more of the line" ==~ /WARNING.*/

        and: 'multiline fails'
        !("""
not a match
WARNING more here"""  ==~ /WARNING.*/)

        and: 'we can find all WARNING lines'
        def linesWithWarning = logmsg.findAll(/.*WARNING.*/)
        String foundlines = new String()
        if (linesWithWarning.size() >0) {
            linesWithWarning.each {
                foundlines += it + '\n'
            }
            println "found WARNING messages\n$foundlines"
        }
        foundlines.size() >0
    }

    def "true"(){
        expect:
        true
    }
}
