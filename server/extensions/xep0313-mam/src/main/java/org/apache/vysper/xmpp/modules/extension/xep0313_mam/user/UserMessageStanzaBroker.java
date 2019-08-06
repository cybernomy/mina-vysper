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
package org.apache.vysper.xmpp.modules.extension.xep0313_mam.user;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.DeliveryFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.MessageStanzaWithId;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.SimpleMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.ArchivedMessage;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchive;
import org.apache.vysper.xmpp.modules.extension.xep0313_mam.spi.MessageArchives;
import org.apache.vysper.xmpp.protocol.DelegatingStanzaBroker;
import org.apache.vysper.xmpp.protocol.StanzaBroker;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.MessageStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Réda Housni Alaoui
 */
class UserMessageStanzaBroker extends DelegatingStanzaBroker {

    private static final Logger LOG = LoggerFactory.getLogger(UserMessageStanzaBroker.class);

    private final ServerRuntimeContext serverRuntimeContext;

    private final SessionContext sessionContext;

    private final boolean isOutbound;

    public UserMessageStanzaBroker(StanzaBroker delegate, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, boolean isOutbound) {
        super(delegate);
        this.serverRuntimeContext = requireNonNull(serverRuntimeContext);
        this.sessionContext = sessionContext;
        this.isOutbound = isOutbound;
    }

    @Override
    public void write(Entity receiver, Stanza stanza, DeliveryFailureStrategy deliveryFailureStrategy)
            throws DeliveryException {
        super.write(receiver, archive(stanza), deliveryFailureStrategy);
    }

    @Override
    public void writeToSession(Stanza stanza) {
        super.writeToSession(archive(stanza));
    }

    private Stanza archive(Stanza stanza) {
        if (!MessageStanza.isOfType(stanza)) {
            return stanza;
        }

        MessageStanza messageStanza = new MessageStanza(stanza);
        MessageStanzaType messageStanzaType = messageStanza.getMessageType();
        if (messageStanzaType != MessageStanzaType.NORMAL && messageStanzaType != MessageStanzaType.CHAT) {
            // A server SHOULD include in a user archive all of the messages a user sends
            // or receives of type 'normal' or 'chat' that contain a <body> element.
            LOG.debug("Message {} is neither of type 'normal' or 'chat'. It will not be archived.", messageStanza);
            return messageStanza;
        }

        Entity archiveJID;
        if (isOutbound) {
            // We will store the message in the sender archive
            archiveJID = XMPPCoreStanzaHandler.extractSenderJID(messageStanza, sessionContext);
        } else {
            // We will store the message in the receiver archive
            archiveJID = requireNonNull(messageStanza.getTo(), "No 'to' found in " + messageStanza);
        }

        // Servers that expose archive messages of sent/received messages on behalf of
        // local users MUST expose these archives to the user on the user's bare JID.
        archiveJID = archiveJID.getBareJID();

        MessageArchives archives = requireNonNull(serverRuntimeContext.getStorageProvider(MessageArchives.class),
                "Could not find an instance of " + MessageArchives.class);

        Optional<MessageArchive> userArchive = archives.retrieveUserMessageArchive(archiveJID);
        if (!userArchive.isPresent()) {
            LOG.debug("No archive returned for user with bare JID '{}'", archiveJID);
            return messageStanza;
        }

        ArchivedMessage archivedMessage = userArchive.get().archive(new SimpleMessage(messageStanza));
        if (isOutbound) {
            return messageStanza;
        } else {
            return new MessageStanzaWithId(archivedMessage).toStanza();
        }
    }
}
