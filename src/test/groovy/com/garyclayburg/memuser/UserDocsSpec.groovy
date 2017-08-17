package com.garyclayburg.memuser

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import spock.lang.*
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.notNullValue
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

/**
 * <br><br>
 * Created 2017-08-17 09:41
 *
 * @author Gary Clayburg
 */
@SpringBootTest
@Slf4j
class UserDocsSpec extends BaseDocsSpec {

    @Autowired
    ObjectMapper objectMapper;

    def "hello info"(){
        when:
        ResultActions resultActions = mockMvc.perform(get('/info')
        .accept(MediaType.APPLICATION_JSON))

        then:
        resultActions.andExpect(status().isOk())
    }

    def "hello users"(){
        when:
        ResultActions resultActions = mockMvc.perform(get('/Users')
        .accept(MediaType.APPLICATION_JSON))

        then:
        resultActions.andExpect(status().isOk())
        .andDo(document("emptyusers"))
    }

    def "create one"(){
        when:
        ResultActions resultActions = mockMvc.perform(post('/Users')
        .contentType(MediaType.APPLICATION_JSON)
        .content('{\n' +
                '  "userName": "alice"\n' +
                '}')
                .accept(MediaType.APPLICATION_JSON))

        then:
        def mvcResult = resultActions.andExpect(status().isCreated())
                .andDo(document("createalice"))
                .andReturn()
        MemUser createdUser = unmarshall(mvcResult)

        when:
        ResultActions resultActionsAlice = mockMvc.perform(get('/Users/' +createdUser.id)
                .accept(MediaType.APPLICATION_JSON))

        then:
        resultActionsAlice.andExpect(status().isOk())
                .andDo(document("getalice"))


    }

    MemUser unmarshall(MvcResult mvcResult) {
        return objectMapper.reader().forType(MemUser.class).readValue(
                mvcResult.getResponse().getContentAsString())
    }

    def "contextok"() {
        expect:
        true
    }
}
