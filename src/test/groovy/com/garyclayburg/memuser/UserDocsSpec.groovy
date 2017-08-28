package com.garyclayburg.memuser

import com.fasterxml.jackson.databind.ObjectMapper
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
    public static final String USERS = '/api/v1/Users'
    public static final String USERSD = USERS + '/'
    @Autowired
    ObjectMapper objectMapper

    def "hello info"() {
        when:
        ResultActions resultActions = mockMvc.perform(get('/info')
                .accept(MediaType.APPLICATION_JSON))

        then:
        resultActions.andExpect(status().isOk())
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

    def "create list"() {
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
        MemUser createdUser = unmarshall(mvcResult)

        when:
        ResultActions resultActionsAlice = mockMvc.perform(get(USERSD + createdUser.id)
                .accept(SCIM_JSON))

        then:
        resultActionsAlice.andExpect(status().isOk())
                .andDo(document('getalice'))
        when:
        ResultActions dupCreateActions = mockMvc.perform(post(USERS)
                .contentType(SCIM_JSON)
                .content('{\n' +
                '  "userName": "alicesmith"\n' +
                '}')
                .accept(SCIM_JSON))

        then: 'no duplicate username allowed'
        dupCreateActions.andExpect(status().is4xxClientError())
                .andDo(document('duplicatealicesmith'))

        when: 'change username'
        createdUser.setUserName('alicejones')
        createdUser.setSchemas(null)
        createdUser.meta = null
        createdUser.data.remove('displayName')
        String strAlice = objectMapper.writeValueAsString(createdUser)
        def startput = ZonedDateTime.now()
        ResultActions changeUserNameACtions = mockMvc.perform(put(USERSD + createdUser.id)
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

        when: 'change alice username with displayname'
        changeUserNameACtions = mockMvc.perform(put(USERSD + createdUser.id)
                .accept(SCIM_JSON)
                .contentType(SCIM_JSON)
                .content('''
{
  "id": "''' + createdUser.id  + '''",
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

    }

    MemUser unmarshall(MvcResult mvcResult) {
        return objectMapper.reader().forType(MemUser).readValue(
                mvcResult.response.contentAsString)
    }

    def "contextok"() {
        expect:
        true
    }
}
