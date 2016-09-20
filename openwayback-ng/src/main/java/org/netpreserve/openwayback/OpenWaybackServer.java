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

package org.netpreserve.openwayback;

import java.util.Set;

import org.netpreserve.openwayback.resources.ServeStaticResource;
import org.netpreserve.openwayback.server.BaseApplicationConfig;
import org.netpreserve.openwayback.server.OwbServer;
import org.netpreserve.openwayback.server.settings.Settings;

/**
 *
 */
public class OpenWaybackServer extends OwbServer {
    @Override
    public Class<? extends Settings> getSettingsClass() {
        return Settings.class;
    }

    @Override
    public BaseApplicationConfig createApplicationConfig(Settings settings) {
        return new BaseApplicationConfig(settings, OpenWaybackServer.class) {
            @Override
            public Set<Class<?>> getClasses() {
                Set<Class<?>> classes = super.getClasses();
                classes.add(ServeStaticResource.class);
                return classes;
            }

        };
    }

    @Override
    public String getServerName() {
        return "OpenWayback";
    }

}
