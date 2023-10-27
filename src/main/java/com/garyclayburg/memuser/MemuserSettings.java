package com.garyclayburg.memuser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <br><br>
 * Created 2023-10-26 14:15
 *
 * @author Gary Clayburg
 */
@Component
@ConfigurationProperties(prefix = "memuser")
public class MemuserSettings {
    // The reason this class is Java and not Groovy like everything else in memuser, is to be able to use the
    // spring-boot-configuration-processor to create META-INF/spring-configuration-metadata.json
    // This will not work if MemuserSettings is written in Groovy.  It seems the Java annotation
    // step is part of the gradle JavaCompile, and not GroovyCompile
    /**
     * when true, show the complete HTTP headers for all requests
     */
    boolean showHeaders = false;

    /**
     * If > 0, load this many simple users on startup.  Attributes include userName, firstname, lastname
     */
    int userCount = 0;
    /**
     * when true, PATCH will return a 204 response instead of 200 if possible
     */
    boolean patchRequestsReturn204 = false;

    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = LoggerFactory.getLogger(MemuserSettings.class);

    public boolean isShowHeaders() {
        return showHeaders;
    }

    public MemuserSettings setShowHeaders(boolean showHeaders) {
        this.showHeaders = showHeaders;
        return this;
    }

    public int getUserCount() {
        return userCount;
    }

    public MemuserSettings setUserCount(int userCount) {
        this.userCount = userCount;
        return this;
    }

    public boolean isPatchRequestsReturn204() {
        return patchRequestsReturn204;
    }

    public MemuserSettings setPatchRequestsReturn204(boolean patchRequestsReturn204) {
        this.patchRequestsReturn204 = patchRequestsReturn204;
        return this;
    }
}
