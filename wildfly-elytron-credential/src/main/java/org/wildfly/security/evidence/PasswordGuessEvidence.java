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

import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.Arrays;

import javax.security.auth.Destroyable;

/**
 * A piece of evidence that is comprised of a password guess.
 */
public final class PasswordGuessEvidence implements Evidence, Destroyable {
    private final char[] guess;
    private boolean destroyed;

    /**
     * Construct a new instance.
     *
     * @param guess the non {@code null} password guess
     */
    public PasswordGuessEvidence(final char[] guess) {
        this.guess = checkNotNullParam("guess", guess);
    }

    /**
     * Get the password guess.
     *
     * @return the password guess
     */
    public char[] getGuess() {
        return guess;
    }

    public void destroy() {
        if (! destroyed) {
            destroyed = true;
            Arrays.fill(guess, '\0');
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
