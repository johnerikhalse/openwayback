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

import java.util.Collections;
import java.util.List;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.netpreserve.commons.cdx.CdxRecord;
import org.netpreserve.resource.resolver.client.jaxrs.CdxjReader;
import org.netpreserve.resource.resolver.client.jaxrs.LegacyCdxReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class ResourceResolverClient {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceResolverClient.class);

    private static final GenericType<List<CdxRecord>> RETURN_TYPE = new GenericType<List<CdxRecord>>() {
    };

    final static Client client = ClientBuilder.newClient(
            new ClientConfig()
            .connectorProvider(new GrizzlyConnectorProvider())
            .register(CdxjReader.class)
            .register(LegacyCdxReader.class));

    final WebTarget root;

    final WebTarget resource;

    final WebTarget resourceList;

    final WebTarget content;

    public ResourceResolverClient(String baseUri) {
        System.out.println("Client base Uri: " + baseUri);
        root = client.target(baseUri);
        resource = root.path("resource");
        resourceList = root.path("resourcelist");
        content = client.target("http://158.39.122.95:8083/resource");
    }

    public ResolveResourceRequest createResolveResourceRequest(final String uri, final String timeStamp) {
        return new ResolveResourceRequest(this, uri, timeStamp);
    }

    public ListResourcesRequest createListResourcesRequest(final String uri) {
        return new ListResourcesRequest(this, uri);
    }

    public GetResourceRequest createGetResourceRequest(final String uri) {
        return new GetResourceRequest(this, uri);
    }

    public void close() {
        client.close();
    }

    final static class ResolveResourceCommand extends HystrixCommand<List<CdxRecord>> {

        private final WebTarget request;

        private final String contentType;

        public ResolveResourceCommand(WebTarget request, String contentType) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ResourceResolver"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withExecutionTimeoutInMilliseconds(10000).withRequestCacheEnabled(false)));

            this.request = request;
            this.contentType = contentType;
        }

        @Override
        protected List<CdxRecord> run() throws Exception {
            System.out.println("EXECUTING: " + request.getUri());
            Response response = request.request(contentType).get();

            if (response.getStatus() >= 300) {
                LOG.error("Failed getting response for '{}'. Response code: {} {}",
                        request.getUri(), response.getStatus(), response.getStatusInfo().getReasonPhrase());
                throw new WebApplicationException(response.getStatus());
            }

            List<CdxRecord> cdxRecords = response.readEntity(RETURN_TYPE);
            return cdxRecords;
        }

//        @Override
//        protected List<CdxRecord> getFallback() {
//            return Collections.EMPTY_LIST;
//        }

    }

    final static class GetResourceCommand extends HystrixCommand<Response> {

        private final WebTarget request;

        public GetResourceCommand(WebTarget request) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ResourceLoader"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withExecutionTimeoutInMilliseconds(10000).withRequestCacheEnabled(false)));

            this.request = request;
        }

        @Override
        protected Response run() throws Exception {
            System.out.println("EXECUTING: " + request.getUri());
            Response response = request.request().get();

            if (response.getStatus() >= 300) {
                LOG.error("Failed getting response for '{}'. Response code: {} {}",
                        request.getUri(), response.getStatus(), response.getStatusInfo().getReasonPhrase());
                throw new WebApplicationException(response.getStatus());
            }

            return response;
        }

//        @Override
//        protected Response getFallback() {
//            return null;
//        }

    }

}
