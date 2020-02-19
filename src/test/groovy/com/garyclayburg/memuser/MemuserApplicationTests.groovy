package com.garyclayburg.memuser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

import java.time.ZonedDateTime

import static org.junit.Assert.assertTrue

@RunWith(SpringRunner)
@SpringBootTest
class MemuserApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testSerialization() throws IOException {
        String expectedJson = '{"parseDate":"2018-12-04T18:47:38.927Z"}'
        MyPojo pojo = new MyPojo(ZonedDateTime.parse('2018-12-04T18:47:38.927Z'))
        ObjectMapper objectMapper = new ObjectMapper()
        objectMapper.registerModule(new JavaTimeModule())
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        def string = objectMapper.writeValueAsString(pojo)
        assertTrue(string == expectedJson)
    }
}

class MyPojo {
    ZonedDateTime parseDate

    MyPojo(ZonedDateTime zonedDateTime) {
        this.parseDate = zonedDateTime
    }
}
