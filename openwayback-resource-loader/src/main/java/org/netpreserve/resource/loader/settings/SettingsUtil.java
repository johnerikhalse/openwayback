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
package org.netpreserve.resource.loader.settings;

import java.util.List;
import java.util.stream.Collectors;

import org.netpreserve.resource.loader.ResourceStore;
import org.netpreserve.resource.loader.ResourceStoreFactory;
import org.netpreserve.resource.loader.resourcestore.MultiResourceStore;

/**
 * Utilities used by the settings classes.
 */
public final class SettingsUtil {

    /**
     * Private constructor to avoid instantiation.
     */
    private SettingsUtil() {
    }

    public static ResourceStore createResourceStore(ResourceStoreSettings settings) {
        List<ResourceStore> resourceStores = settings.getIdentifiers().stream()
                .map(id -> ResourceStoreFactory.getResourceStore(id))
                .filter(cdx -> cdx != null)
                .collect(Collectors.toList());

        ResourceStore src;
        switch (resourceStores.size()) {
            case 0:
                throw new RuntimeException("No cdx resources configured");
            case 1:
                src = resourceStores.get(0);
                break;
            default:
                src = new MultiResourceStore();
                resourceStores.stream().forEach((s) -> {
                    ((MultiResourceStore) src).addResourceStore(s);
                });
                break;
        }
        return src;
    }

}
