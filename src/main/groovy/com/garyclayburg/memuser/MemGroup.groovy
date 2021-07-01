package com.garyclayburg.memuser

import groovy.transform.Canonical

/**
 * <br><br>
 * Created 2021-06-27 12:47
 *
 * @author Gary Clayburg
 */
@Canonical
class MemGroup extends MemScimResource {
    String displayName
    ArrayList<Members> members
}
