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

package org.wildfly.security.auth.server;

import java.security.Principal;

import org.wildfly.security.evidence.Evidence;

/**
 * A realm which can be modified.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ModifiableSecurityRealm extends SecurityRealm {

    /**
     * Get an update handle for to the identity for the given principal in the context of this security realm. Any
     * validation / name mapping is an implementation detail for the realm.  The identity may or may not exist.  The
     * returned handle <em>must</em> be cleaned up by a call to {@link ModifiableRealmIdentity#dispose()}.  During
     * the lifespan of a {@code ModifiableRealmIdentity}, no other updates or authentications may take place for the
     * corresponding realm identity, thus care should be taken to minimize the duration of the identity's lifespan.
     * <p>
     * If there is not enough information to locate an identity compatible with this realm, {@link
     * ModifiableRealmIdentity#NON_EXISTENT}
     * may be returned.
     *
     * @param principal the principal to use to locate the {@link ModifiableRealmIdentity} handle (must not be {@code
     * null})
     * @return the {@link ModifiableRealmIdentity} for the provided information (not {@code null})
     */
    default ModifiableRealmIdentity getRealmIdentityForUpdate(Principal principal) throws RealmUnavailableException {
        return ModifiableRealmIdentity.NON_EXISTENT;
    }

    /**
     * Get an update handle for to the identity for the given principal in the context of this security realm. Any
     * validation / name mapping is an implementation detail for the realm.  The identity may or may not exist.  The
     * returned handle <em>must</em> be cleaned up by a call to {@link ModifiableRealmIdentity#dispose()}.  During
     * the lifespan of a {@code ModifiableRealmIdentity}, no other updates or authentications may take place for the
     * corresponding realm identity, thus care should be taken to minimize the duration of the identity's lifespan.
     * <p>
     * If there is not enough information to locate an identity compatible with this realm, {@link
     * ModifiableRealmIdentity#NON_EXISTENT}
     * may be returned.
     *
     * @param evidence the evidence to use to locate the {@link ModifiableRealmIdentity} handle (must not be {@code
     * null})
     * @return the {@link ModifiableRealmIdentity} for the provided information (not {@code null})
     */
    default ModifiableRealmIdentity getRealmIdentityForUpdate(Evidence evidence) throws RealmUnavailableException {
        final Principal principal = evidence.getDecodedPrincipal();
        return principal == null ? ModifiableRealmIdentity.NON_EXISTENT : getRealmIdentityForUpdate(principal);
    }

    /**
     * Get an iterator over all of this realm's identities.
     *
     * @return the identity iterator
     * @throws RealmUnavailableException if the realm fails for some reason
     */
    ModifiableRealmIdentityIterator getRealmIdentityIterator() throws RealmUnavailableException;
}
