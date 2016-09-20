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
package org.netpreserve.resource.loader.resources;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;
import org.netpreserve.resource.loader.ResourceStore;
import org.netpreserve.resource.loader.ResourceStoreFactory;
import org.netpreserve.resource.loader.jaxrs.CrossOriginResourceSharingFilter;
import org.netpreserve.resource.loader.jaxrs.ResourceStoreProvider;
import org.netpreserve.resource.loader.jaxrs.SettingsProvider;
import org.netpreserve.resource.loader.jaxrs.UriParamConverterProvider;
import org.netpreserve.resource.loader.jaxrs.VersionHeaderFilter;
import org.netpreserve.resource.loader.resourcestore.MultiResourceStore;
import org.netpreserve.resource.loader.settings.Settings;


/**
 *
 */
public class ResourceLoaderResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
//        ResourceStore resourceStore = ResourceStoreFactory.getResourceStore("warcdir:src/test/resources");
        MultiResourceStore resourceStore = new MultiResourceStore();
        ResourceStore rs = ResourceStoreFactory.getResourceStore("warcdir:src/test/resources");
        resourceStore.addResourceStore(rs);

        Settings settings = new Settings();
        settings.setLogTraffic(true);

        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig()
                .register(UriParamConverterProvider.class)
                .register(ResourceLoaderResource.class)
                .register(VersionHeaderFilter.class)
                .register(CrossOriginResourceSharingFilter.class)
                .register(new SettingsProvider(settings))
                .register(new ResourceStoreProvider(resourceStore));
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(UriParamConverterProvider.class);
    }

    /**
     * Test of getResource method, of class ResourceLoaderResource.
     */
    @Test
    public void testGetResource() throws Exception {

//        Uri resource = UriBuilder.strictUriBuilder().uri("warcfile:IAH-20080430204825-00000-blackbook.warc.gz#3380")
        String resourceRef = "warcfile:IAH-20080430204825-00000-blackbook.warc.gz#2052";

        WebTarget target = target("resource").path(URLEncoder.encode(resourceRef, "UTF-8"));

//        Response response = target.request().header("If-None-Match", "\"ac5517803089c33ff74060ccb6cc8055f1d476d6\"").get();
        Response response = target.request().get();

        System.out.println("Status " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase());
        System.out.println("Headers:");
        for (Map.Entry<String, List<Object>> header : response.getHeaders().entrySet()) {
            System.out.println("  " + header.getKey() + " = " + header.getValue());
        }

        System.out.println("==============================");
        System.out.println(response.readEntity(String.class));
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

}
