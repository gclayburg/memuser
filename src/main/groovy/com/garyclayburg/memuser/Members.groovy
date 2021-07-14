package com.garyclayburg.memuser

import groovy.transform.Canonical

/**
 * <br><br>
 * Created 2021-06-27 12:49
 *
 * @author Gary Clayburg
 */
@Canonical
class Members {
    String value, display, ref
    String displayName
    // this is only here to satisfy microsoft postman test, even though it seems to violate the Scim SPEC
}
