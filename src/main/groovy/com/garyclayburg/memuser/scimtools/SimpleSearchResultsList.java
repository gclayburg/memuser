package com.garyclayburg.memuser.scimtools;

import static com.unboundid.scim2.common.utils.ApiConstants.*;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyclayburg.memuser.MemScimResource;
import com.garyclayburg.memuser.MemUser;
import com.garyclayburg.memuser.ResourcesList;
import com.unboundid.scim2.common.GenericScimResource;
import com.unboundid.scim2.common.Path;
import com.unboundid.scim2.common.ScimResource;
import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.messages.SortOrder;
import com.unboundid.scim2.server.utils.ResourceComparator;
import com.unboundid.scim2.server.utils.ResourcePreparer;
import com.unboundid.scim2.server.utils.ResourceTypeDefinition;
import com.unboundid.scim2.server.utils.SchemaAwareFilterEvaluator;

/**
 * A utility ListResponseStreamingOutput that will filter, sort, and paginate
 * the search results for simple search implementations that always returns the
 * entire result set.
 * <p>
 * Adapted/forked from com.unboundid.scim2.server.utils.SimpleSearchResults [com.unboundid.product.scim2:scim-2-sdk-server:2.3.6]
 */
public class SimpleSearchResultsList<T extends ScimResource> {

    private final List<ScimResource> resources;
    private final Filter filter;
    private final Integer startIndex;
    private final Integer count;
    private final SchemaAwareFilterEvaluator filterEvaluator;
    private final ObjectMapper mapper;
    private final ResourceComparator<ScimResource> resourceComparator;
    private final ResourcePreparer<ScimResource> responsePreparer;

    /**
     * Create a new SimpleSearchResults for results from a search operation.
     *
     * @param resourceType The resource type definition of result resources.
     * @param uriInfo      The UriInfo from the search operation.
     * @throws BadRequestException if the filter or paths in the search operation
     *                             is invalid.
     */
    public SimpleSearchResultsList(final ResourceTypeDefinition resourceType,
                                   final UriInfo uriInfo, ObjectMapper mapper) throws BadRequestException {
        this.filterEvaluator = new SchemaAwareFilterEvaluator(resourceType);
        this.mapper = mapper;
        this.responsePreparer =
                new ResourcePreparer<>(resourceType, uriInfo);
        this.resources = new LinkedList<>();

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String filterString = queryParams.getFirst(QUERY_PARAMETER_FILTER);
        String startIndexString = queryParams.getFirst(
                QUERY_PARAMETER_PAGE_START_INDEX);
        String countString = queryParams.getFirst(QUERY_PARAMETER_PAGE_SIZE);
        String sortByString = queryParams.getFirst(QUERY_PARAMETER_SORT_BY);
        String sortOrderString = queryParams.getFirst(QUERY_PARAMETER_SORT_ORDER);

        if (filterString != null) {
            this.filter = Filter.fromString(filterString);
        } else {
            this.filter = null;
        }

        if (startIndexString != null) {
            int i = Integer.parseInt(startIndexString);
            // 3.4.2.4: A value less than 1 SHALL be interpreted as 1.
            startIndex = Math.max(i, 1);
        } else {
            startIndex = null;
        }

        if (countString != null) {
            int i = Integer.parseInt(countString);
            // 3.4.2.4: A negative value SHALL be interpreted as 0.
            count = Math.max(i, 0);
        } else {
            count = null;
        }

        Path sortBy;
        try {
            sortBy = sortByString == null ? null : Path.fromString(sortByString);
        } catch (BadRequestException e) {
            throw BadRequestException.invalidValue("'" + sortByString +
                                                   "' is not a valid value for the sortBy parameter: " +
                                                   e.getMessage());
        }
        SortOrder sortOrder = sortOrderString == null ?
                SortOrder.ASCENDING : SortOrder.fromName(sortOrderString);
        if (sortBy != null) {
            this.resourceComparator = new ResourceComparator<>(
                    sortBy, sortOrder, resourceType);
        } else {
            this.resourceComparator = null;
        }
    }

    /**
     * Add a resource to include in the search results.
     *
     * @param resource The resource to add.
     * @return this object.
     * @throws ScimException If an error occurs during filtering or setting the
     *                       meta attributes.
     */
    public SimpleSearchResultsList<T> add(final T resource) throws ScimException {
        // Convert to GenericScimResource
        GenericScimResource genericResource;
        if (resource instanceof GenericScimResource) {
            // Make a copy
            genericResource = new GenericScimResource(
                    ((GenericScimResource) resource).getObjectNode().deepCopy());
        } else {
            genericResource = resource.asGenericScimResource();
        }

        // Set meta attributes so they can be used in the following filter eval
        responsePreparer.setResourceTypeAndLocation(genericResource);

        if (filter == null || filter.visit(filterEvaluator,
                genericResource.getObjectNode())) {
            resources.add(genericResource);
        }

        return this;
    }

    /**
     * Add resources to include in the search results.
     *
     * @param resources The resources to add.
     * @return this object.
     * @throws ScimException If an error occurs during filtering or setting the
     *                       meta attributes.
     */
    public SimpleSearchResultsList<T> addAll(final Collection<T> resources)
            throws ScimException {
        for (T resource : resources) {
            add(resource);
        }
        return this;
    }

    public ResourcesList toResourcesList() throws IOException {
        if (resourceComparator != null) {
            resources.sort(resourceComparator);
        }
        List<ScimResource> resultsToReturn = resources;
        if (startIndex != null) {
            if (startIndex > resources.size()) {
                resultsToReturn = Collections.emptyList();
            } else {
                resultsToReturn = resources.subList(startIndex - 1, resources.size());
            }
        }
        if (count != null && !resultsToReturn.isEmpty()) {
            resultsToReturn = resultsToReturn.subList(0, Math.min(count, resultsToReturn.size()));
        }
        int totalResultsResourcesList = resources.size();
        int startIndexResourcesList = 1;
        int itemsPerPageResourcesList = resultsToReturn.size();
        if (startIndex != null || count != null) {
            startIndexResourcesList = (startIndex == null ? 1 : startIndex);
        }
        List<MemScimResource> filteredMemUsers = new ArrayList<>();
        for (ScimResource resource : resultsToReturn) {
            GenericScimResource genericScimResource = responsePreparer.trimRetrievedResource(resource);
            filteredMemUsers.add(scimResourceToMemUser(genericScimResource));
        }

        return new ResourcesList(totalResultsResourcesList, startIndexResourcesList, itemsPerPageResourcesList, filteredMemUsers);
    }

    private MemUser scimResourceToMemUser(ScimResource resource) throws IOException {
        return this.mapper.reader().forType(MemUser.class).readValue(resource.toString());
    }
}
