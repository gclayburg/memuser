package com.garyclayburg.memuser

/**
 * <br><br>
 * Created 2021-06-28 00:06
 *
 * @author Gary Clayburg
 */
class ErrorResponse {
    String scimType, detail, status
    String[] schemas = ["urn:ietf:params:scim:api:messages:2.0:Error"]
}
