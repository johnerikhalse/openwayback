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
public final class ResolveResourceRequest {

    private static final String DEFAULT_CONTENT_TYPE = "application/vnd.org.netpreserve.cdxj";

    private final String uri;

    private final String timeStamp;

    private String contentType = DEFAULT_CONTENT_TYPE;

    private String recordType;

    private int limit = -1;

    private final ResourceResolverClient client;

    ResolveResourceRequest(final ResourceResolverClient client, final String uri, final String timeStamp) {
        this.client = client;
        this.uri = uri;
        this.timeStamp = timeStamp;
    }

    public ResolveResourceRequest contentType(final String value) {
        this.contentType = value;
        return this;
    }

    public ResolveResourceRequest recordType(final String value) {
        this.recordType = value;
        return this;
    }

    public ResolveResourceRequest limit(final int value) {
        this.limit = value;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public String getTimeStamp() {
        return timeStamp;
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

    public List<CdxRecord> execute() {
        try {
            WebTarget target = client.resource.path(URLEncoder.encode(getUri(), "UTF-8")).path(getTimeStamp());
            if (getRecordType() != null) {
                target = target.queryParam("recordType", getRecordType());
            }
            if (getLimit() > -1) {
                target = target.queryParam("limit", getLimit());
            }

            return new ResourceResolverClient.ResolveResourceCommand(target, getContentType()).execute();
        } catch (UnsupportedEncodingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
