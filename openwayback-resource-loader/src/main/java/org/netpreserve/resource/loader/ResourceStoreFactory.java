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

import java.util.Objects;
import java.util.ServiceLoader;

import org.netpreserve.commons.uri.Uri;
import org.netpreserve.commons.uri.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CdxSourceFactory using a ServiceLoader so that new CdxSource types could be plugged in by adding a jar file.
 */
public abstract class ResourceStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceStoreFactory.class);

    private static final ServiceLoader<ResourceStoreFactory> SERVICE_LOADER = ServiceLoader.load(ResourceStoreFactory.class);

    /**
     * Default constructor.
     */
    public ResourceStoreFactory() {
        init();
    }

    /**
     * Initialize Factory.
     */
    private void init() {
        LOG.info("Loaded Resource Store Factory for scheme '" + getSupportedScheme() + "'");
    }

    /**
     * Create a new CdxSource.
     * <p>
     * @param identifier a string formatted as a URI with schema identifying the factory to use and the remaining is
     * then understood by that factory.
     * @return a matching source descriptor or null if none could be found
     */
    public static final ResourceStore getResourceStore(String identifier) {
        Uri uri = UriBuilder.strictUriBuilder().uri(identifier).build();
        String scheme = Objects.requireNonNull(uri.getScheme(), "Resource Store Identifier must start with a scheme");

        for (ResourceStoreFactory csf : SERVICE_LOADER) {
            if (scheme.equalsIgnoreCase(csf.getSupportedScheme())) {
                return csf.createCdxSource(uri);
            }
        }

        LOG.warn("No Resource Store Factory found for scheme '{}'", scheme);

        return null;
    }

    /**
     * Which schema this factory understands.
     * <p>
     * Must be implemented by subclasses.
     * <p>
     * @return the name of the schema this factory understands.
     */
    protected abstract String getSupportedScheme();

    /**
     * Create a CdxSource from a URI with factory dependent syntax describing a CDX source.
     * <p>
     * @param identifier a URI describing the CDX source
     * @return a CdxSource obtained from the submitted identifier
     */
    protected abstract ResourceStore createCdxSource(Uri identifier);

}
