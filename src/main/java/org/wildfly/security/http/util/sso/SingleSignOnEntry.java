/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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
package org.wildfly.security.http.util.sso;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.wildfly.security.cache.CachedIdentity;

/**
 * Single sign-on cache entry.
 *
 * @author Paul Ferraro
 */
public interface SingleSignOnEntry {

    /**
     * Returns the {@link CachedIdentity} associated with this single sign-on entry.
     * @return a cached identity
     */
    CachedIdentity getCachedIdentity();

    /**
     * Reassociates the specified {@link CachedIdentity} with this single sign-on entry.
     * @param cachedIdentity a cached identity
     */
    void setCachedIdentity(CachedIdentity cachedIdentity);

    /**
     * Returns the participants associated with this single sign-on entry.
     * @return a mapping of application to tuple containing a session identifier and request URI.
     */
    ConcurrentMap<String, Map.Entry<String, URI>> getParticipants();
}
