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

package org.wildfly.security.credential;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.wildfly.common.Assert;
import org.wildfly.common.math.HashMath;
import org.wildfly.security._private.ElytronMessages;
import org.wildfly.security.key.KeyUtil;

/**
 * A credential containing a private key and an X.509 certificate chain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class X509CertificateChainPrivateCredential extends AbstractX509CertificateChainCredential {
    private final PrivateKey privateKey;

    /**
     * Construct a new instance.
     *
     * @param privateKey the private key (not {@code null})
     * @param certificateChain the certificate chain (not {@code null}, cannot contain {@code null} elements)
     */
    public X509CertificateChainPrivateCredential(final PrivateKey privateKey, final X509Certificate... certificateChain) {
        super(certificateChain);
        Assert.checkNotNullParam("privateKey", privateKey);
        if (! getFirstCertificate().getPublicKey().getAlgorithm().equals(privateKey.getAlgorithm())) {
            throw ElytronMessages.log.mismatchedPublicPrivateKeyAlgorithms();
        }

        /*
         * Where keys from PKCS#11 are in use we can not guarantee to convert them to a comparable parameter spec
         * which leads to the check below returning false - the most we can check is matching algorithms which are checked above.
         */

        //if (! KeyUtil.hasSameParameters(getFirstCertificate().getPublicKey(), privateKey)) {
        //    throw ElytronMessages.log.mismatchedPublicPrivateKeyParameters();
        //}
        this.privateKey = privateKey;
    }

    /**
     * Get the private key.
     *
     * @return the private key
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509CertificateChainPrivateCredential clone() {
        final PrivateKey privateKey = this.privateKey;
        final PrivateKey clone = KeyUtil.cloneKey(PrivateKey.class, privateKey);
        return privateKey == clone ? this : new X509CertificateChainPrivateCredential(clone, getCertificateChain());
    }

    public int hashCode() {
        return HashMath.multiHashOrdered(Arrays.hashCode(certificateChain), privateKey.hashCode());
    }

    public boolean equals(final Object obj) {
        return obj instanceof X509CertificateChainPrivateCredential && equals((X509CertificateChainPrivateCredential) obj);
    }

    private boolean equals(final X509CertificateChainPrivateCredential obj) {
        return privateKey.equals(obj.privateKey) && Arrays.equals(certificateChain, obj.certificateChain);
    }
}
