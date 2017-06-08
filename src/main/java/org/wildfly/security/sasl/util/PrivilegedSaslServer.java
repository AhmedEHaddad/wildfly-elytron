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

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.wildfly.common.Assert;

/**
 * A {@code SaslServer} which evaluates responses and wrap/unwrap requests in an privileged context.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PrivilegedSaslServer extends AbstractDelegatingSaslServer implements SaslWrapper {
    private final AccessControlContext accessControlContext;

    PrivilegedSaslServer(final SaslServer delegate, final AccessControlContext accessControlContext) {
        super(delegate);
        Assert.checkNotNullParam("delegate", delegate);
        Assert.checkNotNullParam("accessControlContext", accessControlContext);
        this.accessControlContext = accessControlContext;
    }

    public PrivilegedSaslServer(final SaslServer delegate) {
        this(delegate, AccessController.getContext());
    }

    public byte[] evaluateResponse(final byte[] response) throws SaslException {
        try {
            return doPrivileged((PrivilegedExceptionAction<byte[]>) () -> delegate.evaluateResponse(response), accessControlContext);
        } catch (PrivilegedActionException pae) {
            try {
                throw pae.getCause();
            } catch (SaslException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable throwable) {
                throw new UndeclaredThrowableException(throwable);
            }
        }
    }

    public byte[] unwrap(final byte[] incoming, final int offset, final int len) throws SaslException {
        try {
            return doPrivileged((PrivilegedExceptionAction<byte[]>) () -> delegate.unwrap(incoming, offset, len), accessControlContext);
        } catch (PrivilegedActionException pae) {
            try {
                throw pae.getCause();
            } catch (SaslException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable throwable) {
                throw new UndeclaredThrowableException(throwable);
            }
        }
    }

    public byte[] wrap(final byte[] outgoing, final int offset, final int len) throws SaslException {
        try {
            return doPrivileged((PrivilegedExceptionAction<byte[]>) () -> delegate.wrap(outgoing, offset, len), accessControlContext);
        } catch (PrivilegedActionException pae) {
            try {
                throw pae.getCause();
            } catch (SaslException | RuntimeException | Error e) {
                throw e;
            } catch (Throwable throwable) {
                throw new UndeclaredThrowableException(throwable);
            }
        }
    }
}
