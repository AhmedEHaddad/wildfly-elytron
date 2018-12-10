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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import javax.security.sasl.SaslException;

/**
 * A {@link SaslServerFactory} which uses a {@link ServiceLoader} to find implementations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceLoaderSaslServerFactory implements SaslServerFactory {
    private final ServiceLoader<SaslServerFactory> loader;

    /**
     * Construct a new instance.
     *
     * @param loader the service loader to use
     */
    public ServiceLoaderSaslServerFactory(final ServiceLoader<SaslServerFactory> loader) {
        this.loader = loader;
    }

    /**
     * Construct a new instance.
     *
     * @param classLoader the class loader to use as the basis of the provider search, or {@code null} to use the system
     *      or bootstrap class loader
     */
    public ServiceLoaderSaslServerFactory(final ClassLoader classLoader) {
        this(ServiceLoader.load(SaslServerFactory.class, classLoader));
    }

    public SaslServer createSaslServer(final String mechanism, final String protocol, final String serverName, final Map<String, ?> props, final CallbackHandler cbh) throws SaslException {
        synchronized (loader) {
            final Iterator<SaslServerFactory> iterator = loader.iterator();
            SaslServerFactory serverFactory;
            SaslServer saslServer;
            for (;;) try {
                // Service loader iterators can blow up in various ways; that's why the loop is structured this way
                if (! iterator.hasNext()) {
                    break;
                }
                serverFactory = iterator.next();
                // let SaslException bubble up
                saslServer = serverFactory.createSaslServer(mechanism, protocol, serverName, props, cbh);
                if (saslServer != null) {
                    return saslServer;
                }
            } catch (ServiceConfigurationError ignored) {}
            return null;
        }
    }

    public String[] getMechanismNames(final Map<String, ?> props) {
        synchronized (loader){
            final Set<String> set = new LinkedHashSet<>();
            final Iterator<SaslServerFactory> iterator = loader.iterator();
            SaslServerFactory serverFactory;
            for (; ; )
                try {
                    // Service loader iterators can blow up in various ways; that's why the loop is structured this way
                    if (!iterator.hasNext()) {
                        break;
                    }
                    serverFactory = iterator.next();
                    // let SaslException bubble up
                    Collections.addAll(set, serverFactory.getMechanismNames(props));
                } catch (ServiceConfigurationError ignored) {
                }
            return set.toArray(new String[set.size()]);
        }
    }
}
