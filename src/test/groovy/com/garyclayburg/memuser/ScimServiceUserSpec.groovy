package com.garyclayburg.memuser

import com.unboundid.scim2.client.ScimService
import com.unboundid.scim2.common.exceptions.ResourceNotFoundException
import com.unboundid.scim2.common.types.Email
import com.unboundid.scim2.common.types.Name
import com.unboundid.scim2.common.types.PhoneNumber
import com.unboundid.scim2.common.types.UserResource
import groovy.util.logging.Slf4j
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.glassfish.jersey.logging.LoggingFeature
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import spock.lang.Specification

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Feature
import java.util.logging.Level
import java.util.logging.Logger

/**
 * <br><br>
 * Created 2021-07-06 10:48
 *
 * @author Gary Clayburg
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class ScimServiceUserSpec extends Specification {

    @LocalServerPort
    private int randomServerPort

    def "retrieve non-existing user"() {
        given:
        ScimService scimService = getScimService(true)

        when:
        String userName = "usershouldnotexist"
        then:
        try {
            scimService.retrieveRequest("Users", userName).invoke(UserResource.class)
        } catch (ResourceNotFoundException ignored) {
            //we should only get a 404 when the user does not exist
        }

    }

    def "retrieve non-existing user spec"() {
        given:
        String userName = "usershouldnotexist"

        when:
        ScimService scimService = getScimService(false)
        scimService.retrieveRequest("Users", userName).invoke(UserResource.class)
        then: ' we should only get a 404 when the user does not exist'
        thrown ResourceNotFoundException
    }

    def "test patch"() {
        given:
        ScimService scimService = getScimService(true)

        UserResource newUser = new UserResource().setUserName("patchUser")
        newUser.setDisplayName("removeMe")
        newUser.setName(new Name().setGivenName("Bob").setFamilyName("Tester"))
        newUser.setEmails(Collections.singletonList(
                new Email().setValue("bob@tester.com").setType("work")))

        when: 'Create a new user.'
        UserResource createdUser =
                scimService.create("Users", newUser)
        then:
        createdUser.id != null
        createdUser.userName == 'patchUser'

        when: 'update user'
        PhoneNumber phone1 = new PhoneNumber().
                setValue("1234567890").setType("home")
        PhoneNumber phone2 = new PhoneNumber().
                setValue("123123123").setType("work").setPrimary(true)

        UserResource updatedUser = scimService.modifyRequest(createdUser).
                removeValues("displayName").
                replaceValue("name.middleName", "the").
                replaceValue("emails[type eq \"work\"].value", "bobNew@tester.com").
                addValues("phoneNumbers", phone1, phone2).invoke()

        then:
        updatedUser.displayName == null
        updatedUser.name.middleName == 'the'
        updatedUser.emails[0].value == 'bobNew@tester.com'
        updatedUser.phoneNumbers.size() == 2
        updatedUser.phoneNumbers.contains(phone1)
        updatedUser.phoneNumbers.contains(phone2)

        when: 'lookup old username before we change it'
        def oldUserNameUser = scimService.searchRequest("Users").filter("userName eq \"patchUser\"").invoke(UserResource.class)
        then:
        oldUserNameUser.totalResults == 1

        when: 'count users'
        def listResponse = scimService.searchRequest("Users").invoke(UserResource.class)
        then:
        listResponse.getTotalResults() == 1

        when: 'change username'
        def userResource = scimService.modifyRequest(updatedUser).replaceValue("userName", "marriedlady").invoke()

        then:
        userResource.userName == 'marriedlady'
        userResource.id == updatedUser.id

        when: 'lookup old username'
        def oldUserNameUserResource = scimService.searchRequest("Users").filter("userName eq \"patchUser\"").invoke(UserResource.class)
        then: 'we cannnot get the user by the old username'
        oldUserNameUserResource.totalResults == 0

        when: 'lookup new username'
        def newUserListed = scimService.searchRequest("Users").filter("userName eq \"marriedlady\"").invoke(UserResource.class)
        then:
        newUserListed.totalResults == 1

        when: 'count users'
        listResponse = scimService.searchRequest("Users").invoke(UserResource.class)
        then:
        listResponse.getTotalResults() == 1

    }

    ScimService getScimService(boolean logrequests) {
        Client client
        if (logrequests) {
            //log Jersey request/response
            //JUL logging.  bleh.  whatever.
            Logger logger = Logger.getLogger(getClass().getName())
            Feature feature = new LoggingFeature(logger, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, null)
            client = ClientBuilder.newBuilder().register(feature).property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).build()
            // ugly, ugly hack to get PATCH to sorta work
            // https://stackoverflow.com/questions/22355235/patch-request-using-jersey-client
            // https://github.com/payara/Payara/issues/5097
        } else {
            client = ClientBuilder.newClient()
        }
        log.info("try port: " + randomServerPort)
        WebTarget target = client.target("http://localhost:" + randomServerPort + "/api/multiv2/spectestdomain")
        return new ScimService(target)
    }
}
