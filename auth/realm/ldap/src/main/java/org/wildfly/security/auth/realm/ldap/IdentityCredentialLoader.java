/*
 * JBoss, Home of Professional Open Source
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

package org.wildfly.security.auth.realm.ldap;


import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.util.function.Supplier;

import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.credential.Credential;

/**
 * A {@link CredentialLoader} for loading credentials stored in LDAP directory.
 *
 * Implementations of this interface are instantiated for a specific identity, as a result all of the methods on this interface
 * are specific to that identity.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
interface IdentityCredentialLoader {

    /**
     * Determine whether a given credential is definitely supported, possibly supported, or definitely not supported.
     *
     * @param credentialType the credential type (must not be {@code null})
     * @param algorithmName the credential algorithm name, if any
     * @param parameterSpec the algorithm parameters to match, or {@code null} if any parameters are acceptable or the credential type
     *  does not support algorithm parameters
     * @param providers the providers to use when checking ability to obtain the credential
     * @return the level of support for this credential type
     */
    SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName, AlgorithmParameterSpec parameterSpec, Supplier<Provider[]> providers);

    /**
     * Acquire a credential of the given type.
     *
     * @param <C> the type to which should be credential casted
     * @param credentialType the credential type (must not be {@code null})
     * @param algorithmName the credential algorithm name, if any
     * @param parameterSpec the algorithm parameters to match, or {@code null} if any parameters are acceptable or the credential type
     *  does not support algorithm parameters
     * @param providers the providers to use when obtaining the credential
     * @return the credential, or {@code null} if the principal has no credential of that name or cannot be casted to that type
     */
    <C extends Credential> C getCredential(Class<C> credentialType, String algorithmName, AlgorithmParameterSpec parameterSpec, Supplier<Provider[]> providers);
}
