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

package org.wildfly.security.evidence;

import java.util.function.Function;

/**
 * A piece of evidence which supports multiple algorithms.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface AlgorithmEvidence extends Evidence {
    /**
     * Get the algorithm name associated with this evidence (will never be {@code null}).
     *
     * @return the algorithm name
     */
    String getAlgorithm();

    default <E extends Evidence, R> R castAndApply(Class<E> evidenceType, String algorithmName, Function<E, R> function) {
        return evidenceType.isInstance(this) && (algorithmName == null || algorithmName.equals(getAlgorithm())) ? function.apply(evidenceType.cast(this)) : null;
    }
}
