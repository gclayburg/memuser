package com.garyclayburg.memuser

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions

import java.time.ZonedDateTime

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * <br><br>
 * Created 2017-08-17 09:41
 *
 * @author Gary Clayburg
 */
@SpringBootTest
@Slf4j
class UserDocsSpec extends BaseDocsSpec {

    public static final String SCIM_JSON = 'application/scim+json'
    public static final String APPLICATION_JSON_VALUE = 'application/json'
    public static final String USERS = '/api/multiv2/specdomain1/Users'
    public static final String USERSD = USERS + '/'
    @Autowired
    ObjectMapper objectMapper

    def "hello info"() {
        when:
        ResultActions resultActions = mockMvc.perform(get('/actuator/info')
                .accept(MediaType.APPLICATION_JSON))

        then:
        resultActions.andExpect(status().isOk())
    }

    def "hello health"() {
        when:
        ResultActions resultActions = mockMvc.perform(get('/actuator/health')
                .accept(MediaType.APPLICATION_JSON))

        then:
        resultActions.andExpect(status().isOk())
    }

    def "schemas"() {
        when:
        ResultActions resultActions = mockMvc.perform(get('/api/multiv2/specdomain/Schemas').accept(MediaType.APPLICATION_JSON))
        then:
        resultActions.andExpect(status().isOk())
        def schemaResultParsed = resultToObject(resultActions)
        resultActions.andExpect(status().isOk())
        schemaResultParsed.totalResults == 2

        when: ' get User schema'
        resultActions = mockMvc.perform(get('/api/multiv2/specdomain/Schemas/urn:ietf:params:scim:schemas:core:2.0:User').accept(MediaType.APPLICATION_JSON))
        schemaResultParsed = resultToObject(resultActions)
        then:
        resultActions.andExpect(status().isOk())
        schemaResultParsed.description == 'User Account'

    }
    def "nobody"() {
        when:
        ResultActions resultActions = mockMvc.perform(get(USERSD + 'nobodyhere')
                .accept(SCIM_JSON))

        then:
        resultActions.andExpect(status().isNotFound())
    }

    def "hello users"() {
        when:
        ResultActions resultActions = mockMvc.perform(get(USERS)
                .accept(SCIM_JSON))

        then:
        resultActions.andExpect(status().isOk())
                .andDo(document('emptyusers'))
    }

    def "x-forwarded headers"() {
        given: 'create bob'
        mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content('''
{
  "userName": "bobbysmith",
  "displayName": "Bob Smith"
}
''')
                .accept(SCIM_JSON))

        when:
        ResultActions resultActionsAll = mockMvc.perform(get(USERSD)
                .header('Host', 'somewherebehindaproxy')
                .header('X-Forwarded-Proto', 'https')
                .header('X-Forwarded-Host', 'thehttpshost:443')
                .header('X-Forwarded-For', '10.2.3.4')
                .accept(SCIM_JSON))
        def userlist = unmarshaluserList(resultActionsAll.andReturn())
        then:
        userlist.resources[0].meta.location.startsWith('https://thehttpshost:443/api')
    }

    def "java time birthday parse does not truncate milliseconds of timestamps"() {
        given: 'clear out any other test data'
        mockMvc.perform(delete(USERSD).accept(SCIM_JSON))

        when: 'create bob'
        def listOfBobs = []
        for (int i = 0; i < 100; i++) {

            ResultActions createdBob = mockMvc.perform(post(USERS)
                    .contentType(SCIM_JSON)
                    .content("""
{
  "userName": "bobbysmith${Math.random()}",
  "displayName": "Bob Smith",
  "birthday": "2018-08-09T08:18:34.100-06:00"
}
""")
                    .accept(SCIM_JSON))
            listOfBobs.add(createdBob)
        }

        then: 'all bobs have formatted dates with millisecond precision'
        for (createdBob in listOfBobs) {
            def createdBobReturned = createdBob.andExpect(status().isCreated()).andReturn()
            def matcher = createdBobReturned.response.contentAsString =~ /\.\d\d\d/
            log.info("content is: " + createdBobReturned.response.contentAsString)

            assert matcher.find()
            assert matcher.size() == 4 //userName, created, lastModified, birthday
        }
    }

    def "username can be supplied case-insensitive"() {
        given: 'clear out test data'
        mockMvc.perform(delete(USERSD).accept(SCIM_JSON))
        when: 'create microsoft friendly bob'
        ResultActions weirdBob = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content("""
{
  "UserName": "MSbob",
  "displayName": "MS Bob"
}
""")
                .accept(SCIM_JSON))

        then: 'username attribute is returned only once'
        def weirdBobCreateResult = weirdBob.andExpect(status().isCreated()).andReturn()
        log.info("weird bob: " + weirdBobCreateResult.response.contentAsString)
        def bobParsed = new JsonSlurper().parseText(weirdBobCreateResult.response.contentAsString)
        bobParsed.userName == 'MSbob'
        bobParsed.UserName == null // even though we sent UserName, the returned name is userName which is legal

        when: 'change userName with Replace patch operation'
        println 'do the patch already'
        ResultActions changedBobRA = mockMvc.perform(patch(USERSD +bobParsed.id)
        .contentType(APPLICATION_JSON_VALUE).content("""
{ "schemas":
       ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
     "Operations":[
       {
        "op":"Replace",
        "path":"userName",
        "value":"newusername"
       }]
   }
""")
        .accept(APPLICATION_JSON_VALUE))

        then: 'username was changed'
        println 'can we change bob or no?'
        println "content is ${changedBobRA.andReturn().response.getContentAsString()}"
        def changeBobContent = changedBobRA.andExpect(status().isOk()).andReturn().response.getContentAsString()
        def newusernameparsed = new JsonSlurper().parseText(changeBobContent)
        newusernameparsed.userName == 'newusername'
    }

    def "username NOT supplied"() {
        given: 'clear out test data'
        mockMvc.perform(delete(USERSD).accept(SCIM_JSON))
        when: 'create empty user'
        ResultActions emptyBob = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content("""
{
  "displayName": "ghost Bob"
}
""")
                .accept(SCIM_JSON))

        then: '400 returned'
        emptyBob.andExpect(status().isBadRequest()).andReturn()
    }


    def "user create list"() {
        given: 'clear out any other test data'
        mockMvc.perform(delete(USERSD).accept(SCIM_JSON))

        when: 'create alice'
        ResultActions createActions = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content('''
{
  "userName": "alicesmith",
  "displayName": "Alice P Smith"
}
''')
                .accept(SCIM_JSON))

        then:
        def mvcResult = createActions.andExpect(status().isCreated())
                .andDo(document('createalice'))
                .andReturn()
        MemUser createdAliceSmith = unmarshall(mvcResult)

        when: 'also parse result as json'
        def bodyString = mvcResult.response.contentAsString
        def bodyJson = new JsonSlurper().parseText(bodyString)

        then: 'meta.created is ISO 8601 format, not decimal timestamp'
        !BigDecimal.isCase(bodyJson.meta.created) //!instanceof

        when: 'lookup created alice'
        ResultActions resultActionsAlice = mockMvc.perform(get(USERSD + createdAliceSmith.id)
                .accept(SCIM_JSON))

        then:
        resultActionsAlice.andExpect(status().isOk())
                .andDo(document('getalice'))

        when: 'add a new user with same userName'
        ResultActions dupCreateActions = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content('''
{
  "userName": "alicesmith"
}
''')
                .accept(SCIM_JSON))

        then: 'no duplicate username allowed'
        dupCreateActions.andExpect(status().is4xxClientError())
                .andDo(document('duplicatealicesmith'))

        when: 'add a new user with different userName'
        ResultActions bellCreateActions = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content('''
{
  "userName": "alicebell"
}
''')
                .accept(SCIM_JSON))

        then:
        def bellMvcResult = bellCreateActions.andExpect(status().isCreated())
                .andDo(document('createdalicebell')).andReturn()

        when: 'changeusername of alicebell to alicesmith'
        def memUserBell = unmarshall(bellMvcResult)
        ResultActions changeUserNameBell = mockMvc.perform(put(USERSD + memUserBell.id)
                .accept(SCIM_JSON)
                .contentType(SCIM_JSON)
                .content('''
{
  "userName": "alicesmith"
}
'''))
        then: 'should be 409 conflict'
        changeUserNameBell.andExpect(status().is4xxClientError())

        when: 'change username'
        createdAliceSmith.setUserName('alicejones')
        createdAliceSmith.setSchemas(null)
        createdAliceSmith.meta = null
        createdAliceSmith.data.remove('displayName')
        String strAlice = objectMapper.writeValueAsString(createdAliceSmith)
        def startput = ZonedDateTime.now()
        ResultActions changeUserNameACtions = mockMvc.perform(put(USERSD + createdAliceSmith.id)
                .accept(SCIM_JSON)
                .contentType(SCIM_JSON)
                .content(strAlice))
        def aliceModified = unmarshall(changeUserNameACtions.andReturn())

        then:
        changeUserNameACtions.andExpect(status().isOk())
                .andDo(document('changeusername'))
        aliceModified.userName == 'alicejones'
        aliceModified.meta.lastModified.isAfter(startput)
        aliceModified.meta.created.isBefore(startput)
        aliceModified.data.get('displayName') != 'Alice P Smith'

        when: 'get user by returned meta.location'
        log.info('meta.location= ' + aliceModified.meta.location)

        ResultActions resultActionsAliceLocation = mockMvc.perform(get(aliceModified.meta.location.replace('http://localhost:8080', ''))
                .accept(SCIM_JSON))
        def alicelocation = unmarshall(resultActionsAliceLocation.andReturn())

        then: 'same username is returned'
        alicelocation.userName == aliceModified.userName

        when: 'change alice username with displayname'
        changeUserNameACtions = mockMvc.perform(put(USERSD + createdAliceSmith.id)
                .accept(SCIM_JSON)
                .contentType(SCIM_JSON)
                .content('''
{
  "id": "''' + createdAliceSmith.id + '''",
  "userName": "alicejones",
  "displayName": "Alice P Smith"
}
'''))
        aliceModified = unmarshall(changeUserNameACtions.andReturn())

        then:
        changeUserNameACtions.andExpect(status().isOk())
                .andDo(document('changeusernamedisplayname'))
        aliceModified.userName == 'alicejones'
        aliceModified.meta.lastModified.isAfter(startput)
        aliceModified.meta.created.isBefore(startput)
        aliceModified.data.get('displayName') == 'Alice P Smith'

        when: 'create tom'
        createActions = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content('''
{
  "userName": "tomjones",
  "name": {
    "formatted": "Dr. Tom J Jones",
    "familyName": "Jones",
    "givenName": "Tom",
    "middleName": "Jackson",
    "honorificPrefix": "Dr."
  },
  "displayName": "Tom Jones"
}
''')
                .accept(SCIM_JSON))

        then:
        createActions.andExpect(status().isCreated())
                .andDo(document('createtom'))

        when: 'create harry'
        createActions = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content('''
{
  "userName": "harry",
  "displayName": "Tom Jones",
  "mailcode": "CHP",
  "emailaddress": "tomjones@gmail.com"
}
''')
                .accept(SCIM_JSON))

        then:
        createActions.andExpect(status().isCreated())
                .andDo(document('createharry'))

        when:
        ResultActions resultActionsList = mockMvc.perform(get(USERSD)
                .accept(SCIM_JSON))

        then:
        resultActionsList.andExpect(status().isOk())
                .andDo(document('getlist'))
        def userlist = unmarshaluserList(resultActionsList.andReturn())
        userlist.resources.size() == 4
        userlist.resources.size() == userlist.totalResults

        when: 'get first user by returned meta.location'
        ResultActions resultActionsUser0 = mockMvc.perform(get(userlist.resources[0].meta.location.replace('http://localhost:8080', ''))
                .accept(SCIM_JSON))
        def user0location = unmarshall(resultActionsUser0.andReturn())

        then: 'same username is returned'
        alicelocation.userName == user0location.userName

        when: 'create bill password'
        createActions = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content('''
{
  "userName": "billy",
  "password": "qwerty1234",
  "userpassword": "asdfghjkl"
}
''')
                .accept(SCIM_JSON))

        then:
        createActions.andExpect(status().isCreated())
                .andDo(document('createbill'))

        when: 'attempt to return user list with parsable filter'
        ResultActions resultList = mockMvc.perform(get(USERS + '?filter=userName eq "billy"')
                .accept(SCIM_JSON))

        then: 'ok'
        resultList.andExpect(status().isOk())

        when: 'attempt to return user list with unparsable filter'
        ResultActions resultListBad = mockMvc.perform(get(USERS + '?filter=userName eq billy')
                .accept(SCIM_JSON))
        def MVCresult = resultListBad.andReturn()
        JsonSlurper jsonSlurper = new JsonSlurper()
        def scimException = jsonSlurper.parseText(MVCresult.response.contentAsString)

        then: 'response body has scim exception details'
        resultListBad.andExpect(status().isBadRequest())
        scimException.scimType == 'invalidFilter'

        when: 'return filtered user with correct size'
        ResultActions resultListTrimmed = mockMvc.perform(get(USERS + '?filter=userName eq "harry"')
                .accept(SCIM_JSON))
        def harryObj = (ResourcesList) jsonSlurper.parseText(resultListTrimmed.andReturn().response.contentAsString)

        then:
        harryObj.Resources.size() == 1
        harryObj.totalResults == 1
        harryObj.Resources[0].userName == 'harry'

        when: 'return filtered user with excluded attribute'
        def harryFilteredExcluded = (ResourcesList) jsonSlurper.parseText(mockMvc.perform(get(USERS + '?filter=userName eq "harry"&excludedAttributes=mailcode')
                .accept(SCIM_JSON)).andReturn().response.contentAsString)

        then:
        harryFilteredExcluded.Resources[0].userName == 'harry'
        harryFilteredExcluded.Resources[0].mailcode == null

        when: 'return first page of all alices with pagesize 1'
        def alices = (ResourcesList) jsonSlurper.parseText(mockMvc.perform(get(USERS + '?filter=userName sw "alice"&startIndex=1&count=1')
                .accept(SCIM_JSON)).andReturn().response.contentAsString)

        then:
        alices.totalResults == 2
        alices.itemsPerPage == 1
        alices.Resources[0].userName == 'alicejones'

        when: 'return second page of all alices with pagesize 1'
        def alices2ndPage = (ResourcesList) jsonSlurper.parseText(mockMvc.perform(get(USERS + '?filter=userName sw "alice"&startIndex=2&count=1')
                .accept(SCIM_JSON)).andReturn().response.contentAsString)

        then:
        alices2ndPage.totalResults == 2
        alices2ndPage.itemsPerPage == 1
        alices2ndPage.Resources[0].userName == 'alicebell'

    }

    ResourcesList unmarshaluserList(MvcResult mvcResult) {
        objectMapper.reader().forType(ResourcesList).readValue(mvcResult.response.contentAsString)
    }

    MemUser unmarshall(MvcResult mvcResult) {
        return objectMapper.reader().forType(MemUser).readValue(
                mvcResult.response.contentAsString)
    }

    def "contextok"() {
        expect:
        true
    }

    static Object resultToObject(ResultActions resultActions) {
        new JsonSlurper().parseText(resultActions.andReturn().response.contentAsString)
    }
}
