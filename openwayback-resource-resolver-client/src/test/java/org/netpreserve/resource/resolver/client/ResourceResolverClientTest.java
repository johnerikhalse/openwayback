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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;
import org.netpreserve.commons.cdx.CdxRecord;
import org.netpreserve.commons.cdx.CdxSource;
import org.netpreserve.commons.cdx.CdxSourceFactory;
import org.netpreserve.commons.cdx.FieldName;
import org.netpreserve.commons.cdx.HasUnparsedData;
import org.netpreserve.commons.cdx.cdxrecord.CdxLine;
import org.netpreserve.commons.uri.UriBuilder;
import org.netpreserve.resource.resolver.jaxrs.CdxSourceProvider;
import org.netpreserve.resource.resolver.jaxrs.CdxjHtmlWriter;
import org.netpreserve.resource.resolver.jaxrs.CdxjWriter;
import org.netpreserve.resource.resolver.jaxrs.CrossOriginResourceSharingFilter;
import org.netpreserve.resource.resolver.jaxrs.DateRangeParamConverterProvider;
import org.netpreserve.resource.resolver.jaxrs.LegacyCdxWriter;
import org.netpreserve.resource.resolver.jaxrs.SettingsProvider;
import org.netpreserve.resource.resolver.jaxrs.UriMatchTypeParamConverterProvider;
import org.netpreserve.resource.resolver.jaxrs.VersionHeaderFilter;
import org.netpreserve.resource.resolver.resources.ListResource;
import org.netpreserve.resource.resolver.resources.LookupResource;
import org.netpreserve.resource.resolver.settings.Settings;

import static org.assertj.core.api.Assertions.*;

/**
 *
 */
public class ResourceResolverClientTest extends JerseyTest {

    @Override
    protected Application configure() {
        CdxSource cdxSource = CdxSourceFactory
                .getCdxSource("cdxfile:src/test/resources/IAH-20080430204825-00000-blackbook.warc.cdxj");
        Settings settings = new Settings();
        settings.setLogTraffic(true);

        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig()
                .register(DateRangeParamConverterProvider.class)
                .register(UriMatchTypeParamConverterProvider.class)
                .register(CdxjHtmlWriter.class)
                .register(CdxjWriter.class)
                .register(LegacyCdxWriter.class)
                .register(LookupResource.class)
                .register(ListResource.class)
                .register(VersionHeaderFilter.class)
                .register(CrossOriginResourceSharingFilter.class)
                .register(new SettingsProvider(settings))
                .register(new CdxSourceProvider(cdxSource));
    }

    public ResourceResolverClientTest() {
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.command.ResolveResourceCommand.execution.isolation.thread.timeoutInMilliseconds", 2000);
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.command.FallbackViaNetwork.execution.isolation.thread.timeoutInMilliseconds", 8000);
//
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.command.default.metrics.rollingStats.timeInMilliseconds", 60000);
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.command.default.metrics.rollingStats.numBuckets", 60);
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.command.default.metrics.rollingPercentile.timeInMilliseconds", 60000);
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.command.default.metrics.rollingPercentile.numBuckets", 60);
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.command.default.metrics.rollingPercentile.enabled", true);
//
//        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.coreSize", 10);
//        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.maxQueueSize", 100);
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.threadpool.default.queueSizeRejectionThreshold", 100);
//
//        ConfigurationManager.getConfigInstance()
//                .setProperty("hystrix.command.ResolveResourceCommand.requestCache.enabled", false);
//
    }

    /**
     * Test of resolveResource method, of class ResourceResolverClient.
     */
    @Test
    public void testResolveResource() throws InterruptedException, ExecutionException {
        String uri = "https://www.archive.org";
        String timeStamp = "20070827225413";

        ResourceResolverClient instance = new ResourceResolverClient(getBaseUri().toString());
        ResolveResourceRequest request = instance.createResolveResourceRequest(uri, timeStamp);

        List<CdxRecord> cdxRecords = request.execute();
        System.out.println("Size: " + cdxRecords.size());
        System.out.println("MCT: " + cdxRecords.get(0).get(FieldName.CONTENT_TYPE));
        System.out.println(((HasUnparsedData) cdxRecords.get(0)).getUnparsed());

//        fail("The test case is a prototype.");
    }

    @Test
    public void testListResources() throws InterruptedException, ExecutionException {
        String uri = "https://www.archive.org";
        String timeStamp = "20070827225413";

        ResourceResolverClient instance = new ResourceResolverClient(getBaseUri().toString());
        ListResourcesRequest request = instance.createListResourcesRequest(uri).matchType("host").recordType("response");

        List<CdxRecord> cdxRecords = request.execute();

        if (cdxRecords != null) {
            System.out.println("Size: " + cdxRecords.size());

            for (CdxRecord r : cdxRecords) {
                System.out.println(r.get(FieldName.ORIGINAL_URI));
                System.out.println(r.get(FieldName.URI_KEY));
                System.out.println(r.get(FieldName.RESOURCE_REF));
//                System.out.println(r.get(FieldName.RESOURCE_REF).getValue().getDecodedPath());
//                System.out.println(r.get(FieldName.RESOURCE_REF).getValue().getFragment());
                System.out.println();
            }
        } else {
            System.out.println("FAILED");
        }

//        fail("The test case is a prototype.");
    }

}
