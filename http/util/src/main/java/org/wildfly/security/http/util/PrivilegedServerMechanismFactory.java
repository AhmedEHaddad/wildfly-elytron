/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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
package org.wildfly.security.http.util;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.wildfly.security.http.HttpAuthenticationException;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;

/**
 * A {@link HttpServerAuthenticationMechanismFactory} that wraps a delegate so that any returned
 * {@link HttpServerAuthenticationMechanism} is wrapped by a wrapper that ensures all calls are using the provided
 * {@link AccessControlContext}, if no AccessControlContext is provided then the one in place at the time this factory is
 * instantiated is used instead.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public final class PrivilegedServerMechanismFactory implements HttpServerAuthenticationMechanismFactory {

    private final HttpServerAuthenticationMechanismFactory delegate;
    private final AccessControlContext accessControlContext;

    /**
     * Construct a new instance of {@code PrivilegedServerMechanismFactory}.
     *
     * @param delegate the {@link HttpServerAuthenticationMechanismFactory} to delegate to.
     * @param accessControlContext the {@link AccessControlContext} to use for calls to the wrapped factory and subsequently any
     *        mechanism created by that factory.
     */
    public PrivilegedServerMechanismFactory(final HttpServerAuthenticationMechanismFactory delegate,
            final AccessControlContext accessControlContext) {
        this.delegate = checkNotNullParam("delegate", delegate);
        this.accessControlContext = checkNotNullParam("accessControlContext", accessControlContext);
    }

    /**
     * Construct a new instance of {@code PrivilegedServerMechanismFactory} using the current {@link AccessControlContext} for
     * calls to the wrapped factory
     *
     * @param delegate the {@link HttpServerAuthenticationMechanismFactory} to delegate to.
     */
    public PrivilegedServerMechanismFactory(final HttpServerAuthenticationMechanismFactory delegate) {
        this(delegate, AccessController.getContext());
    }

    /**
     * @see org.wildfly.security.http.HttpServerAuthenticationMechanismFactory#getMechanismNames(java.util.Map)
     */
    @Override
    public String[] getMechanismNames(Map<String, ?> properties) {
        return delegate.getMechanismNames(properties);
    }

    /**
     * @see org.wildfly.security.http.HttpServerAuthenticationMechanismFactory#createAuthenticationMechanism(java.lang.String, java.util.Map, javax.security.auth.callback.CallbackHandler)
     */
    @Override
    public HttpServerAuthenticationMechanism createAuthenticationMechanism(String mechanismName, Map<String, ?> properties,
            CallbackHandler callbackHandler) throws HttpAuthenticationException {
        HttpServerAuthenticationMechanism serverMechanism = delegate.createAuthenticationMechanism(mechanismName, properties,
                callbackHandler);
        return serverMechanism != null ? new PrivilegedServerMechanism(serverMechanism, accessControlContext) : null;
    }

}
