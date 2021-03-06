/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.storage.inmemory;

import org.apache.vysper.storage.OpenStorageProviderRegistry;
import org.apache.vysper.xmpp.authentication.SimpleUserAuthentication;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;

/**
 * all the memory based stores. information put here is lost on JVM exit.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class MemoryStorageProviderRegistry extends OpenStorageProviderRegistry {

    public MemoryStorageProviderRegistry() {
        add(new SimpleUserAuthentication());
        add(new MemoryRosterManager());

        // provider from external modules, low coupling, fail when modules are not present
        add("org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.LeafNodeInMemoryStorageProvider");
        add("org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.storageprovider.CollectionNodeInMemoryStorageProvider");
        //add("org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.MemoryOfflineStorageProvider");
    }

}