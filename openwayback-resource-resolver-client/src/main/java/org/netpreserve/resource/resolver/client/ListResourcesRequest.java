/*
 * Copyright 2016 The International Internet Preservation Consortium.
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
package org.netpreserve.resource.resolver.client;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.ws.rs.client.WebTarget;
import org.netpreserve.commons.cdx.CdxRecord;

/**
 *
 */
public final class ListResourcesRequest {

    private static final String DEFAULT_CONTENT_TYPE = "application/vnd.org.netpreserve.cdxj";

    private final String uri;

    private String dateExpression;

    private String contentType = DEFAULT_CONTENT_TYPE;

    private String recordType;

    private String matchType;

    private boolean reverseSort = false;

    private int limit = -1;

    private final ResourceResolverClient client;

    ListResourcesRequest(final ResourceResolverClient client, final String uri) {
        this.client = client;
        this.uri = uri;
    }

    public ListResourcesRequest dateExpression(final String value) {
        this.dateExpression = value;
        return this;
    }

    public ListResourcesRequest contentType(final String value) {
        this.contentType = value;
        return this;
    }

    public ListResourcesRequest recordType(final String value) {
        this.recordType = value;
        return this;
    }

    public ListResourcesRequest matchType(final String value) {
        this.matchType = value;
        return this;
    }

    public ListResourcesRequest reverseSort(final boolean value) {
        this.reverseSort = value;
        return this;
    }
    public ListResourcesRequest limit(final int value) {
        this.limit = value;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public String getDateExpression() {
        return dateExpression;
    }

    public String getContentType() {
        return contentType;
    }

    public String getRecordType() {
        return recordType;
    }

    public int getLimit() {
        return limit;
    }

    public String getMatchType() {
        return matchType;
    }

    public boolean isReverseSort() {
        return reverseSort;
    }

    public List<CdxRecord> execute() {
        try {
            WebTarget target = client.resourceList.path(URLEncoder.encode(getUri(), "UTF-8"));
            if (getDateExpression() != null) {
                target = target.queryParam("date", getDateExpression());
            }
            if (getRecordType() != null) {
                target = target.queryParam("recordType", getRecordType());
            }
            if (getMatchType()!= null) {
                target = target.queryParam("matchType", getMatchType());
            }
            if (getLimit() > -1) {
                target = target.queryParam("limit", getLimit());
            }
            if (isReverseSort()) {
                target = target.queryParam("sort", "desc");
            }

            return new ResourceResolverClient.ResolveResourceCommand(target, getContentType()).execute();
        } catch (UnsupportedEncodingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
