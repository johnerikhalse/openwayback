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
package org.netpreserve.resource.loader.jaxrs;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.netpreserve.resource.loader.ResourceStore;

/**
 * Provides ResourceStore for injections.
 * <p>
 * By registering an instance of this class in the application configuration, it is possible to use @Context annotation
 * to inject the ResourceStore. It will not work if it is registered as a class.
 */
public class ResourceStoreProvider extends AbstractBinder {

    private final ResourceStore resourceStore;

    public ResourceStoreProvider(ResourceStore resourceStore) {
        this.resourceStore = resourceStore;
    }

    @Override
    protected void configure() {
        bindFactory(new Factory() {
            @Override
            public ResourceStore provide() {
                return resourceStore;
            }

            @Override
            public void dispose(Object instance) {
            }

        }).to(ResourceStore.class);
    }

}
