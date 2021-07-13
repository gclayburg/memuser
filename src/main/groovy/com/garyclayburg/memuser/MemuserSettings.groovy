package com.garyclayburg.memuser

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * <br><br>
 * Created 2021-06-06 09:34
 *
 * @author Gary Clayburg
 */
@Component
@ConfigurationProperties(prefix = 'memuser')
class MemuserSettings {
    boolean showHeaders = false
    int userCount = 0
    /**
     * when true, PATCH will return a 204 response instead of 200 if possible
     */
    boolean patchRequestsReturn204 = false
}
