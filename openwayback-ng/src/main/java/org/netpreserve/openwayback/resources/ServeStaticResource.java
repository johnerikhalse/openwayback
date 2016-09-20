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
package org.netpreserve.openwayback.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

/**
 *
 */
@Path("/")
@Singleton
public class ServeStaticResource {

    public ServeStaticResource() {
        System.out.println("Instanciated");
    }

    @GET
    @Path("{path:.*}")
    public Response getResource(@Context UriInfo uriInfo, @PathParam("path") String path) {
        if (path.isEmpty()) {
            path = "index.html";
        }

        final String resolvedPath = "frontend/" + path;

        System.out.println("Got Here " + path);

        String mediaType = "application/octet-stream";

        if (resolvedPath.endsWith(".html")) {
            mediaType = MediaType.TEXT_HTML;
        } else if (resolvedPath.endsWith(".css")) {
            mediaType = "text/css";
        } else if (resolvedPath.endsWith(".js")) {
            mediaType = "application/javascript";
        } else if (resolvedPath.endsWith(".png")) {
            mediaType = "image/png";
        }

        try {
            return Response.ok(new StreamingOutput() {

                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    System.out.println("URI: " + ClassLoader.getSystemResource(resolvedPath));
                    try (InputStream in = ClassLoader.getSystemResourceAsStream(resolvedPath);) {
                        byte[] buf = new byte[1024 * 16];
                        int len = in.read(buf);
                        while (len != -1) {
                            output.write(buf, 0, len);
                            len = in.read(buf);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw new WebApplicationException(ex);
                    }
                }

            }, mediaType).build();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new WebApplicationException(ex);
        }
    }

}
