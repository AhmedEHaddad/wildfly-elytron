/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.security.sasl.util;

import static org.wildfly.common.math.HashMath.multiHashOrdered;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

import org.wildfly.security.auth.callback.SocketAddressQueryCallbackHandler;

/**
 * A {@link SaslClientFactory} which adds {@link org.wildfly.security.auth.callback.SocketAddressCallback SocketAddressCallback} capability to a delegate {@code SaslClientFactory}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SocketAddressCallbackSaslClientFactory extends AbstractDelegatingSaslClientFactory {
    private final SocketAddress localAddress;
    private final SocketAddress peerAddress;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate client factory
     * @param localAddress the local socket address, or {@code null} if unknown
     * @param peerAddress the peer socket address, or {@code null} if unknown
     */
    public SocketAddressCallbackSaslClientFactory(final SaslClientFactory delegate, final SocketAddress localAddress, final SocketAddress peerAddress) {
        super(delegate);
        this.localAddress = localAddress;
        this.peerAddress = peerAddress;
    }

    public SaslClient createSaslClient(final String[] mechanisms, final String authorizationId, final String protocol, final String serverName, final Map<String, ?> props, final CallbackHandler cbh) throws SaslException {
        return delegate.createSaslClient(mechanisms, authorizationId, protocol, serverName, props, new SocketAddressQueryCallbackHandler(cbh, localAddress, peerAddress));
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final Object other) {
        return other instanceof SocketAddressCallbackSaslClientFactory && equals((SocketAddressCallbackSaslClientFactory) other);
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final AbstractDelegatingSaslClientFactory other) {
        return other instanceof SocketAddressCallbackSaslClientFactory && equals((SocketAddressCallbackSaslClientFactory) other);
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final SocketAddressCallbackSaslClientFactory other) {
        return super.equals(other) && Objects.equals(localAddress, other.localAddress) && Objects.equals(peerAddress, other.peerAddress);
    }

    protected int calculateHashCode() {
        return multiHashOrdered(multiHashOrdered(multiHashOrdered(super.calculateHashCode(), getClass().hashCode()), Objects.hashCode(localAddress)), Objects.hashCode(peerAddress));
    }
}
