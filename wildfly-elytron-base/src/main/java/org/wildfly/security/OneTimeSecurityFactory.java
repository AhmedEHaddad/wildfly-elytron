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

package org.wildfly.security;

import static org.wildfly.security.ElytronMessages.log;

import java.security.GeneralSecurityException;

/**
 * A {@link SecurityFactory} implementation which calls delegated factory at first and
 * returns created object for any other create call. Thread safe.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class OneTimeSecurityFactory<T> implements SecurityFactory<T> {
    private volatile SecurityFactory<T> factory;
    private volatile T obj;

    /**
     * Creates a new factory instance.
     *
     * @param factory a security factory to use to obtain object which should be returned by this factory every time
     */
    public OneTimeSecurityFactory(final SecurityFactory<T> factory) {
        this.factory = factory;
    }

    public T create() throws GeneralSecurityException {
        T val = obj;
        if (val == null) {
            if (Thread.holdsLock(this)) {
                throw log.cannotInstantiateSelfReferentialFactory();
            }
            synchronized (this) {
                val = obj;
                if (val == null) {
                    val = obj = factory.create();
                    factory = null;
                }
            }
        }
        return val;
    }
}
