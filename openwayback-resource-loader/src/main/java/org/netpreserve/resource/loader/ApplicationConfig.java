/*
 * Copyright 2015 IIPC.
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
package org.netpreserve.resource.loader;

import java.util.HashMap;
import java.util.Map;

import org.netpreserve.resource.loader.resources.ResourceLoaderResource;
import org.netpreserve.resource.loader.jaxrs.CrossOriginResourceSharingFilter;
import org.netpreserve.resource.loader.jaxrs.SettingsProvider;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.netpreserve.resource.loader.jaxrs.ResourceStoreProvider;
import org.netpreserve.resource.loader.jaxrs.UriParamConverterProvider;
import org.netpreserve.resource.loader.jaxrs.VersionHeaderFilter;
import org.netpreserve.resource.loader.settings.Settings;

/**
 * Jersey application configuration.
 * <p>
 * Defines the components of the JAX-RS application and supplies additional meta-data.
 */
@ApplicationPath("/")
public class ApplicationConfig extends Application {
    private final Settings settings;
    private final ResourceStore resourceStore;

    public ApplicationConfig(Settings settings, ResourceStore resourceStore) {
        this.settings = settings;
        this.resourceStore = resourceStore;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();

        resources.add(UriParamConverterProvider.class);
        resources.add(ResourceLoaderResource.class);
        resources.add(VersionHeaderFilter.class);
        resources.add(CrossOriginResourceSharingFilter.class);
        resources.add(EncodingFilter.class);
        resources.add(GZipEncoder.class);
        resources.add(DeflateEncoder.class);

        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> resources = new java.util.HashSet<>();

        resources.add(new SettingsProvider(settings));
        resources.add(new ResourceStoreProvider(resourceStore));

        return resources;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<>();

        if (settings.isLogTraffic()) {
            props.put(LoggingFeature.LOGGING_FEATURE_LOGGER_NAME, "org.netpreserve.resource.loader.traffic");
            props.put(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_SERVER, "FINE");
            props.put(LoggingFeature.LOGGING_FEATURE_VERBOSITY_SERVER, LoggingFeature.Verbosity.HEADERS_ONLY);
        }

        return props;
    }

}
