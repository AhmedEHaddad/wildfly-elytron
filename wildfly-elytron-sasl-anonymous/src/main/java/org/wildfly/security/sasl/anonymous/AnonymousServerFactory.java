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

package org.wildfly.security.sasl.anonymous;


import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.util.Map;

import org.kohsuke.MetaInfServices;
import org.wildfly.security.sasl.util.SaslMechanismInformation;

/**
 * The server factory for the anonymous SASL mechanism.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@MetaInfServices(value = SaslServerFactory.class)
public class AnonymousServerFactory extends AbstractAnonymousFactory implements SaslServerFactory {

    @Override
    public String[] getMechanismNames(Map<String, ?> props) {
        return super.getMechanismNames(props);
    }

    @Override
    public SaslServer createSaslServer(String mechanism, String protocol, String serverName, Map<String, ?> props, CallbackHandler cbh) throws SaslException {
        // Unless we are sure anonymous is required don't return a SaslServer
        if (SaslMechanismInformation.Names.ANONYMOUS.equals(mechanism) == false || matches(props, false) == false) {
            return null;
        }

        return new AnonymousSaslServer(protocol, serverName, cbh);
    }

}
