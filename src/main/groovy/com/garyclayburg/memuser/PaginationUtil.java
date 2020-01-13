/*
 * Copyright 2016-2019 the original author or authors from the JHipster project.
 *
 * This file is part of the JHipster project, see https://www.jhipster.tech/
 * for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyclayburg.memuser;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for handling pagination.
 *
 * <p>
 * Pagination uses the same principles as the <a href="https://developer.github.com/v3/#pagination">GitHub API</a>,
 * and follow <a href="http://tools.ietf.org/html/rfc5988">RFC 5988 (Link header)</a>.
 *
 * <p>copied from io.github.jhipster.web.util.PaginationUtil
 * <p>also duplicated from visualsync.  perhaps this should be in a library instead.</p>
 */
public final class PaginationUtil {

    private static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";
    private static final String HEADER_LINK_FORMAT = "<{0}>; rel=\"{1}\"";

    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = LoggerFactory.getLogger(PaginationUtil.class);

    private PaginationUtil() {
    }

    /**
     * Generate pagination headers for a Spring Data {@link Page} object.
     *
     * @param uriBuilder The URI builder.
     * @param page       The page.
     * @param <T>        The type of object.
     * @return http header.
     */
    public static <T> HttpHeaders generatePaginationHttpHeaders(UriComponentsBuilder uriBuilder, Page<T> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_X_TOTAL_COUNT, Long.toString(page.getTotalElements()));
        int pageNumber = page.getNumber();
        int pageSize = page.getSize();
        StringBuilder link = new StringBuilder();
        if (pageNumber < page.getTotalPages() - 1) {
            link.append(prepareLink(uriBuilder, pageNumber + 1, pageSize, "next"))
                    .append(",");
        }
        if (pageNumber > 0) {
            link.append(prepareLink(uriBuilder, pageNumber - 1, pageSize, "prev"))
                    .append(",");
        }
        link.append(prepareLink(uriBuilder, page.getTotalPages() - 1, pageSize, "last"))
                .append(",")
                .append(prepareLink(uriBuilder, 0, pageSize, "first"));
        headers.add(HttpHeaders.LINK, link.toString());
        return headers;
    }

    private static String prepareLink(UriComponentsBuilder uriBuilder, int pageNumber, int pageSize, String relType) {
        return MessageFormat.format(HEADER_LINK_FORMAT, preparePageUri(uriBuilder, pageNumber, pageSize), relType);
    }

    private static String preparePageUri(UriComponentsBuilder uriBuilder, int pageNumber, int pageSize) {
        return uriBuilder.replaceQueryParam("page", Integer.toString(pageNumber))
                .replaceQueryParam("size", Integer.toString(pageSize))
                .toUriString()
                .replace(",", "%2C")
                .replace(";", "%3B");
    }
}
