package com.garyclayburg.memuser

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical
import org.springframework.data.domain.Pageable

/**
 * <br><br>
 * Created 2021-07-02 12:10
 *
 * @author Gary Clayburg
 */
@Canonical
class ResourcesList {
    List<String> schemas = ['urn:ietf:params:scim:api:messages:2.0:ListResponse']
    int totalResults
    int itemsPerPage
    int startIndex

    @JsonProperty('Resources')
    List<MemScimResource> Resources

    /**
     * needed to return sublist of users from DomainUserController, DomainGroupController
     */
    @JsonIgnore
    int springStartIndex
    @JsonIgnore
    int endIndex

    ResourcesList() {  // used by jackson
    }

    ResourcesList(Pageable pageable, int totalResults) {
        this.startIndex = (pageable.pageNumber) * pageable.pageSize
        this.totalResults = totalResults
        if (startIndex < totalResults) {
            this.endIndex = startIndex + pageable.pageSize
            itemsPerPage = pageable.pageSize
            if ((endIndex >= totalResults)) {
                endIndex = totalResults
                itemsPerPage = totalResults - startIndex
            }
            this.springStartIndex = startIndex
            this.startIndex++ //RFC7644 uses 1 based pages, spring pageable uses 0 based pages
        } else {
            this.startIndex = 0
            this.itemsPerPage = 0
            this.totalResults = 0
            this.endIndex = 0
        }
    }

    ResourcesList(int totalResultsResourcesList, int startIndexResourcesList, int itemsPerPageResourcesList, List<MemScimResource> filteredResources) {
        this.startIndex = startIndexResourcesList
        this.totalResults = totalResultsResourcesList
        this.resources = filteredResources
        this.itemsPerPage = itemsPerPageResourcesList
    }

    @JsonProperty('Resources')
    void setResources(resources) {
        Resources = resources
    }
}
