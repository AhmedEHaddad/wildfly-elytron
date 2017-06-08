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

import static java.security.AccessController.doPrivileged;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

import org.wildfly.common.math.HashMath;

/**
 * A {@code SaslClientFactory} whose {@code SaslClient} instances evaluate challenges and wrap/unwrap requests in a
 * privileged context.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PrivilegedSaslClientFactory extends AbstractDelegatingSaslClientFactory {

    private final AccessControlContext context;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate SASL client factory
     */
    public PrivilegedSaslClientFactory(final SaslClientFactory delegate) {
        this(delegate, AccessController.getContext());
    }

    PrivilegedSaslClientFactory(final SaslClientFactory delegate, final AccessControlContext context) {
        super(delegate);
        this.context = context;
    }

    public SaslClient createSaslClient(final String[] mechanisms, final String authorizationId, final String protocol, final String serverName, final Map<String, ?> props, final CallbackHandler cbh) throws SaslException {
        final SaslClient saslClient;
        try {
            saslClient = doPrivileged((PrivilegedExceptionAction<SaslClient>) () -> delegate.createSaslClient(mechanisms, authorizationId, protocol, serverName, props, cbh), context);
        } catch (PrivilegedActionException pae) {
            try {
                throw pae.getCause();
            } catch (SaslException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable throwable) {
                throw new UndeclaredThrowableException(throwable);
            }
        }
        return saslClient == null ? null : new PrivilegedSaslClient(saslClient, context);
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final Object other) {
        return other instanceof PrivilegedSaslClientFactory && equals((PrivilegedSaslClientFactory) other);
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final AbstractDelegatingSaslClientFactory other) {
        return other instanceof PrivilegedSaslClientFactory && equals((PrivilegedSaslClientFactory) other);
    }

    @SuppressWarnings("checkstyle:equalshashcode")
    public boolean equals(final PrivilegedSaslClientFactory other) {
        return super.equals(other) && context.equals(other.context);
    }

    protected int calculateHashCode() {
        return HashMath.multiHashOrdered(HashMath.multiHashOrdered(super.calculateHashCode(), getClass().hashCode()), context.hashCode());
    }
}
