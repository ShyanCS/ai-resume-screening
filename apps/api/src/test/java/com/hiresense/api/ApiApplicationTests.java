package com.hiresense.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIf(value = "com.hiresense.api.testsupport.DatabaseAvailability#isReachable")
class ApiApplicationTests {

    @Test
    void contextLoads() {}
}
