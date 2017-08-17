package com.garyclayburg.memuser

import groovy.util.logging.Slf4j
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.JUnitRestDocumentation
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.*
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;


/**
 * <br><br>
 * Created 2017-08-17 09:20
 *
 * @author Gary Clayburg
 */
@SpringBootTest
@Slf4j
class BaseDocsSpec extends Specification {

    @Rule
    JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets")

    protected MockMvc mockMvc

    @Autowired
    private WebApplicationContext context

    def "setup"() {
        log.info("setup spec")
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation)).build()
    }
}
