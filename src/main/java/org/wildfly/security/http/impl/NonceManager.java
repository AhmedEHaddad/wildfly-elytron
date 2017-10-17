/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

package org.wildfly.security.http.impl;

import static org.wildfly.security._private.ElytronMessages.log;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.wildfly.security.http.HttpConstants;
import org.wildfly.security.mechanism.AuthenticationMechanismException;
import org.wildfly.security.util.ByteIterator;
import org.wildfly.security.util.CodePointIterator;
import org.wildfly.security.util._private.Arrays2;

/**
 * A utility responsible for managing nonces.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class NonceManager {

    private static final int PREFIX_LENGTH = Integer.BYTES + Long.BYTES;

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private final AtomicInteger nonceCounter = new AtomicInteger();
    private final Map<String, Integer> usedNonces = new HashMap<>();

    private final byte[] privateKey;

    private final long validityPeriodNano;
    private final boolean singleUse;
    private final String algorithm;

    /**
     * @param validityPeriod the time in ms that nonces are valid for in ms.
     * @param singleUse are nonces single use?
     * @param keySize the number of bytes to use in the private key of this node.
     * @param algorithm the message digest algorithm to use when creating the digest portion of the nonce.
     */
    NonceManager(long validityPeriod, boolean singleUse, int keySize, String algorithm) {
        this.validityPeriodNano = validityPeriod * 1000000;
        this.singleUse = singleUse;
        this.algorithm = algorithm;

        this.privateKey = new byte[keySize];
        new SecureRandom().nextBytes(privateKey);
    }

    /**
     * Generate a new encoded nonce to send to the client.
     *
     * @return a new encoded nonce to send to the client.
     */
    String generateNonce() {
        return generateNonce(null);
    }

    /**
     * Generate a new encoded nonce to send to the client.
     *
     * @param salt additional data to use when creating the overall signature for the nonce.
     * @return a new encoded nonce to send to the client.
     */
    String generateNonce(byte[] salt) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);

            ByteBuffer byteBuffer = ByteBuffer.allocate(PREFIX_LENGTH + messageDigest.getDigestLength());
            byteBuffer.putInt(nonceCounter.incrementAndGet());
            byteBuffer.putLong(System.nanoTime());
            byteBuffer.put(digest(byteBuffer.array(), 0, PREFIX_LENGTH, salt, messageDigest));

            String nonce = ByteIterator.ofBytes(byteBuffer.array()).base64Encode().drainToString();
            if (log.isTraceEnabled()) {
                String saltString = salt == null ? "null" : ByteIterator.ofBytes(salt).hexEncode().drainToString();
                log.tracef("New nonce generated %s, using seed %s", nonce, saltString);
            }
            return nonce;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] digest(byte[] prefix, int prefixOffset, int prefixLength, byte[] salt, MessageDigest messageDigest) throws DigestException {
        messageDigest.update(prefix, prefixOffset, prefixLength);
        if (salt != null) {
            messageDigest.update(salt);
        }

        return messageDigest.digest(privateKey);
    }

    /**
     * Attempt to use the supplied nonce.
     *
     * A nonce might not be usable for a couple of different reasons: -
     *
     * <ul>
     *     <li>It was created too far in the past.
     *     <li>Validation of the signature fails.
     *     <li>The nonce has been used previously and re-use is disabled.
     * </ul>
     *
     * @param nonce the nonce supplied by the client.
     * @param nonceCount the nonce count, or -1 if not present
     * @return {@code true} if the nonce can be used, {@code false} otherwise.
     * @throws AuthenticationMechanismException
     */
    boolean useNonce(String nonce, int nonceCount) throws AuthenticationMechanismException {
        return useNonce(nonce, null, nonceCount);
    }

    /**
     * Attempt to use the supplied nonce.
     *
     * A nonce might not be usable for a couple of different reasons: -
     *
     * <ul>
     *     <li>It was created too far in the past.
     *     <li>Validation of the signature fails.
     *     <li>The nonce has been used previously and re-use is disabled.
     * </ul>
     *
     * @param nonce the nonce supplied by the client.
     * @param salt additional data to use when creating the overall signature for the nonce.
     * @return {@code true} if the nonce can be used, {@code false} otherwise.
     * @throws AuthenticationMechanismException
     */
    boolean useNonce(final String nonce, byte[] salt, int nonceCount) throws AuthenticationMechanismException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            ByteIterator byteIterator = CodePointIterator.ofChars(nonce.toCharArray()).base64Decode();
            byte[] nonceBytes = byteIterator.drain();
            if (nonceBytes.length != PREFIX_LENGTH + messageDigest.getDigestLength()) {
                throw log.invalidNonceLength(HttpConstants.DIGEST_NAME);
            }

            long age = System.nanoTime() - ByteBuffer.wrap(nonceBytes, Integer.BYTES, Long.BYTES).getLong();
            if (age < 0 || age > validityPeriodNano) {
                log.tracef("Nonce %s rejected due to age %d (ns) being less than 0 or greater than the validity period %d (ns)", nonce, age, validityPeriodNano);
                return false;
            }

            if (Arrays2.equals(nonceBytes, PREFIX_LENGTH, digest(nonceBytes, 0, PREFIX_LENGTH, salt, messageDigest)) == false) {
                if (log.isTraceEnabled()) {
                    String saltString = salt == null ? "null" : ByteIterator.ofBytes(salt).hexEncode().drainToString();
                    log.tracef("Nonce %s rejected due to failed comparison using secret key with seed %s.", nonce,
                            saltString);
                }
                return false;
            }
            if(nonceCount > 0) {
                synchronized (usedNonces) {
                    Integer current = usedNonces.get(nonce);
                    if(current == null) {
                        executor.schedule(() -> {
                            synchronized(usedNonces) {
                                usedNonces.remove(nonce);
                            }
                        }, validityPeriodNano - age, TimeUnit.MILLISECONDS);
                        usedNonces.put(nonce, nonceCount);
                    } else if(nonceCount <= current) {
                        return false;
                    } else {
                        usedNonces.put(nonce, nonceCount);
                    }
                }
            } else if (singleUse) {
                synchronized(usedNonces) {
                    Integer used = usedNonces.put(nonce, 1);
                    if (used == null) {
                        executor.schedule(() -> {
                            synchronized(usedNonces) {
                                usedNonces.remove(nonce);
                            }
                        }, validityPeriodNano - age, TimeUnit.MILLISECONDS);
                        return true;
                    } else {
                        log.tracef("Nonce %s rejected as previously used.", nonce);
                        return false;
                    }
                }

            }

            return true;

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
