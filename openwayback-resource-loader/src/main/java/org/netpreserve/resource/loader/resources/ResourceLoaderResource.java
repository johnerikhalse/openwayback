/*
 * Copyright 2016 IIPC.
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
package org.netpreserve.resource.loader.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.netpreserve.commons.cdx.CdxRecord;
import org.netpreserve.commons.cdx.FieldName;
import org.netpreserve.commons.uri.Uri;
import org.netpreserve.commons.uri.UriBuilder;
import org.netpreserve.resource.loader.settings.Settings;
import org.netpreserve.resource.loader.ResourceResponse;
import org.netpreserve.resource.loader.ResourceStore;
import org.netpreserve.resource.resolver.client.ResourceResolverClient;

/**
 *
 */
@Path("resource")
@Singleton
public class ResourceLoaderResource {

    @Context
    Settings settings;

    @Context
    ResourceStore resourceStore;

    static ResourceResolverClient rrClient = new ResourceResolverClient("http://158.39.122.95:8082");

    @GET
    @Path("{resourceRef}")
    public Response getResource(@BeanParam ResourceLoaderQueryParameters params) throws IOException, URISyntaxException {
        try {
            EntityTag eTag = createEntityTag(params);
            Response.ResponseBuilder responseBuilder = params.getRequest().evaluatePreconditions(eTag);

            // Client's submitted 'If-None-Match'-header had same ETag. Returning 304 Not Modified.
            if (responseBuilder != null) {
                return responseBuilder.build();
            }

            ResourceResponse resourceResponse = resourceStore.getResource(params.getResourceRef());

            if (resourceResponse == null) {
                return Response
                        .status(404)
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .entity(params.getResourceRef() + " Not found")
                        .build();
            }

            switch (resourceResponse.getRecordType()) {
                case "response":
                    System.out.println("IS RESPONSE, DELIVER");
                    break;
                case "revisit":
                    System.out.println("IS REVISIT, LOOKUP RESPONSE");
                    break;
                default:
                    System.out.println("DON'T KNOW HOW TO HANDLE THIS RECORD TYPE: " + resourceResponse.getRecordType());
            }

            StreamingOutput output = new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    resourceResponse.writePayload(output);
                    resourceResponse.close();
                }

            };

            responseBuilder = Response.ok()
                    .type(resourceResponse.getContentType())
                    .tag(eTag)
                    .link(resourceResponse.getTargetUri(), "original")
                    //                    .link(record.header.warcTargetUriStr, "timemap")
                    //                    .link(record.header.warcTargetUriStr, "timegate")
                    .header("Vary", "prefer")
                    .header("Preference-Applied", "original-content")
                    .header("Preference-Applied", "original-links")
                    .header("Preference-Applied", "original-headers")
                    .header("X-Archive-Orig-Status", resourceResponse.getStatus())
                    .entity(output);

            for (Map.Entry<String, List<String>> h : resourceResponse.getHeaders().entrySet()) {
                responseBuilder.header("X-Archive-Orig-" + h.getKey(), h.getValue());
            }

            return responseBuilder.build();
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @GET
    @Path("{uri}/{timestamp}")
    public Response getResource(@PathParam("uri") final String uri, @PathParam("timestamp") final String timestamp,
            @Context Request request) throws IOException, URISyntaxException {
        List<CdxRecord> resources = rrClient.createResolveResourceRequest(uri, timestamp).recordType("response")
                .execute();
        ResourceLoaderQueryParameters params = new ResourceLoaderQueryParameters();
        params.resourceRef = resources.get(0).get(FieldName.RESOURCE_REF).getValue();
        params.request = request;

        return getResource(params);
    }

    @GET
    @Path("{baseUri}/{timestamp}/{uri}")
    public Response getResource(@PathParam("baseUri") final String baseUri, @PathParam("uri") final String uri, @PathParam("timestamp") final String timestamp,
            @Context Request request) throws IOException, URISyntaxException {

        Uri resolvedUri = UriBuilder.strictUriBuilder().uri(baseUri).resolve(uri).build();
        System.out.println("RESOLVED URI: " + resolvedUri);
        List<CdxRecord> resources = rrClient.createResolveResourceRequest(resolvedUri.toString(), timestamp).recordType("response")
                .execute();
        ResourceLoaderQueryParameters params = new ResourceLoaderQueryParameters();
        params.resourceRef = resources.get(0).get(FieldName.RESOURCE_REF).getValue();
        params.request = request;

        return getResource(params);
    }

    private static EntityTag createEntityTag(ResourceLoaderQueryParameters params) {

        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(params.getResourceRef().toString().getBytes("UTF-8"));

            return EntityTag.valueOf('"' + new BigInteger(1, crypt.digest()).toString(16) + '"');
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

}
