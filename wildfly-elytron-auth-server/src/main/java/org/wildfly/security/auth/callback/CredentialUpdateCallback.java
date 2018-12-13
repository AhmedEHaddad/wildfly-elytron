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

package org.wildfly.security.auth.callback;

import org.wildfly.common.Assert;
import org.wildfly.security.credential.Credential;

/**
 * A callback to inform the callback handler of a credential change.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public final class CredentialUpdateCallback implements ExtendedCallback {

    private final Credential credential;

    /**
     * Construct a new instance.
     *
     * @param credential the new credential
     */
    public CredentialUpdateCallback(final Credential credential) {
        Assert.checkNotNullParam("credential", credential);
        this.credential = credential;
    }

    /**
     * Get the new credential.
     *
     * @return the new credential
     */
    public Credential getCredential() {
        return credential;
    }

    /**
     * Get the new credential, if it is of the given credential class.
     *
     * @param credentialClass the credential class
     * @param <C> the credential type
     * @return the credential, or {@code null} if it is not of the given type
     */
    public <C extends Credential> C getCredential(final Class<C> credentialClass) {
        final Credential credential = this.credential;
        return credentialClass.isInstance(credential) ? credentialClass.cast(credential) : null;
    }

    public boolean isOptional() {
        return false;
    }
}
