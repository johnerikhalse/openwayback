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

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 *
 */
public final class GetResourceRequest {

    private static final String DEFAULT_CONTENT_TYPE = "*/*";

    private final String uri;

    private String contentType = DEFAULT_CONTENT_TYPE;

    private final ResourceResolverClient client;

    GetResourceRequest(final ResourceResolverClient client, final String uri) {
        this.client = client;
        this.uri = uri;
    }

    public GetResourceRequest contentType(final String value) {
        this.contentType = value;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public String getContentType() {
        return contentType;
    }

    public Response execute() {
        try {
            WebTarget target = client.content.path(URLEncoder.encode(getUri(), "UTF-8"));

            return new ResourceResolverClient.GetResourceCommand(target).execute();
        } catch (UnsupportedEncodingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
