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

/**
 * Main class for starting the OpenWayback Server.
 */
public class Main {

    /**
     * Start the server.
     * <p>
     * @param args the command line args
     */
    public static void main(String[] args) {
        // This class intentionally doesn't do anything except for instanciating a ResourceResolverServer.
        // This is necessary to be able to replace the LogManager. The system property must be set before any other
        // logging is even loaded.
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        new OpenWaybackServer();
    }

}
