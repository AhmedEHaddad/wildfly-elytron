/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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
package org.wildfly.security.auth.realm.jdbc;

import org.wildfly.common.Assert;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.realm.CacheableSecurityRealm;
import org.wildfly.security.auth.realm.jdbc.mapper.AttributeMapper;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;

import javax.sql.DataSource;

import java.security.Principal;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.wildfly.security._private.ElytronMessages.log;

/**
 * Security realm implementation backed by a database.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JdbcSecurityRealm implements CacheableSecurityRealm {

    private final Supplier<Provider[]> providers;
    private final List<QueryConfiguration> queryConfiguration;

    public static JdbcSecurityRealmBuilder builder() {
        return new JdbcSecurityRealmBuilder();
    }

    JdbcSecurityRealm(List<QueryConfiguration> queryConfiguration, Supplier<Provider[]> providers) {
        this.queryConfiguration = queryConfiguration;
        this.providers = providers;
    }

    @Override
    public RealmIdentity getRealmIdentity(final Principal principal) {
        if (! (principal instanceof NamePrincipal)) {
            return RealmIdentity.NON_EXISTENT;
        }
        return new JdbcRealmIdentity(principal.getName());
    }

    @Override
    public SupportLevel getCredentialAcquireSupport(final Class<? extends Credential> credentialType, final String algorithmName, final AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
        Assert.checkNotNullParam("credentialType", credentialType);
        SupportLevel support = SupportLevel.UNSUPPORTED;
        for (QueryConfiguration configuration : queryConfiguration) {
            for (KeyMapper keyMapper : configuration.getColumnMappers(KeyMapper.class)) {
                final SupportLevel mapperSupport = keyMapper.getCredentialAcquireSupport(credentialType, algorithmName, parameterSpec);
                if (support.compareTo(mapperSupport) < 0) {
                    support = mapperSupport;
                }
            }
        }
        return support;
    }

    @Override
    public SupportLevel getEvidenceVerifySupport(final Class<? extends Evidence> evidenceType, final String algorithmName) throws RealmUnavailableException {
        Assert.checkNotNullParam("evidenceType", evidenceType);
        SupportLevel support = SupportLevel.UNSUPPORTED;
        for (QueryConfiguration configuration : queryConfiguration) {
            for (KeyMapper keyMapper : configuration.getColumnMappers(KeyMapper.class)) {
                final SupportLevel mapperSupport = keyMapper.getEvidenceVerifySupport(evidenceType, algorithmName);
                if (support.compareTo(mapperSupport) < 0) {
                    support = mapperSupport;
                }
            }
        }
        return support;
    }

    @Override
    public void registerIdentityChangeListener(Consumer<Principal> listener) {
        // no notifications from this realm about changes on the underlying storage
    }

    private class JdbcRealmIdentity implements RealmIdentity {

        private final String name;
        private JdbcIdentity identity;

        public JdbcRealmIdentity(String name) {
            this.name = name;
        }

        public Principal getRealmIdentityPrincipal() {
            return new NamePrincipal(name);
        }

        @Override
        public SupportLevel getCredentialAcquireSupport(final Class<? extends Credential> credentialType, final String algorithmName, final AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
            Assert.checkNotNullParam("credentialType", credentialType);
            SupportLevel support = SupportLevel.UNSUPPORTED;
            for (QueryConfiguration configuration : JdbcSecurityRealm.this.queryConfiguration) {
                for (KeyMapper keyMapper : configuration.getColumnMappers(KeyMapper.class)) {
                    if (keyMapper.getCredentialAcquireSupport(credentialType, algorithmName, parameterSpec).mayBeSupported()) {
                        final SupportLevel mapperSupport = executePrincipalQuery(configuration, r -> keyMapper.getCredentialSupport(r, providers));
                        if (mapperSupport == SupportLevel.SUPPORTED) {
                            return SupportLevel.SUPPORTED;
                        } else if (mapperSupport == SupportLevel.POSSIBLY_SUPPORTED) {
                            support = SupportLevel.POSSIBLY_SUPPORTED;
                        }
                    }
                }
            }

            return support;
        }

        @Override
        public <C extends Credential> C getCredential(final Class<C> credentialType) throws RealmUnavailableException {
            return getCredential(credentialType, null);
        }

        @Override
        public <C extends Credential> C getCredential(final Class<C> credentialType, final String algorithmName) throws RealmUnavailableException {
            return getCredential(credentialType, algorithmName, null);
        }

        @Override
        public <C extends Credential> C getCredential(final Class<C> credentialType, final String algorithmName, final AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
            Assert.checkNotNullParam("credentialType", credentialType);
            for (QueryConfiguration configuration : JdbcSecurityRealm.this.queryConfiguration) {
                for (KeyMapper keyMapper : configuration.getColumnMappers(KeyMapper.class)) {
                    if (keyMapper.getCredentialAcquireSupport(credentialType, algorithmName, parameterSpec).mayBeSupported()) {
                        final Credential credential = executePrincipalQuery(configuration, r -> keyMapper.map(r, providers));
                        if (credential.matches(credentialType, algorithmName, parameterSpec)) {
                            return credentialType.cast(credential);
                        }
                    }
                }
            }

            return null;
        }

        @Override
        public SupportLevel getEvidenceVerifySupport(final Class<? extends Evidence> evidenceType, final String algorithmName) throws RealmUnavailableException {
            Assert.checkNotNullParam("evidenceType", evidenceType);
            SupportLevel support = SupportLevel.UNSUPPORTED;
            for (QueryConfiguration configuration : JdbcSecurityRealm.this.queryConfiguration) {
                for (KeyMapper keyMapper : configuration.getColumnMappers(KeyMapper.class)) {
                    if (keyMapper.getEvidenceVerifySupport(evidenceType, algorithmName).mayBeSupported()) {
                        final SupportLevel mapperSupport = executePrincipalQuery(configuration, r -> keyMapper.getCredentialSupport(r, providers));
                        if (mapperSupport == SupportLevel.SUPPORTED) {
                            return SupportLevel.SUPPORTED;
                        } else if (mapperSupport == SupportLevel.POSSIBLY_SUPPORTED) {
                            support = SupportLevel.POSSIBLY_SUPPORTED;
                        }
                    }
                }
            }

            return support;
        }

        @Override
        public boolean verifyEvidence(final Evidence evidence) throws RealmUnavailableException {
            Assert.checkNotNullParam("evidence", evidence);

            if (exists()) {
                for (Credential credential : this.identity.credentials) {
                    if (credential.canVerify(evidence)) {
                        return credential.verify(evidence);
                    }
                }
            }

            return false;
        }

        public boolean exists() throws RealmUnavailableException {
            return getIdentity() != null;
        }

        @Override
        public AuthorizationIdentity getAuthorizationIdentity() throws RealmUnavailableException {
            if (!exists()) {
                return AuthorizationIdentity.EMPTY;
            }

            return AuthorizationIdentity.basicIdentity(this.identity.attributes);
        }

        private JdbcIdentity getIdentity() {
            if (this.identity == null) {
                this.identity = JdbcSecurityRealm.this.queryConfiguration.stream().map(queryConfiguration -> executePrincipalQuery(queryConfiguration, resultSet -> {
                    if (resultSet.next()) {
                        MapAttributes attributes = new MapAttributes();

                        do {
                            queryConfiguration.getColumnMappers(AttributeMapper.class).forEach(attributeMapper -> {
                                try {
                                    Object value = attributeMapper.map(resultSet, providers);

                                    if (value != null) {
                                        attributes.addFirst(attributeMapper.getName(), value.toString());
                                    }
                                } catch (SQLException cause) {
                                    throw log.ldapRealmFailedObtainAttributes(this.name, cause);
                                }
                            });
                        } while (resultSet.next());

                        return attributes;
                    }

                    return null;
                })).collect(Collectors.reducing((lAttribute, rAttribute) -> {
                    if (rAttribute == null) {
                        return lAttribute;
                    }

                    MapAttributes attributes = new MapAttributes(lAttribute);

                    for (Attributes.Entry rEntry : rAttribute.entries()) {
                        attributes.get(rEntry.getKey()).addAll(rEntry);
                    }

                    return attributes;
                })).map(attributes -> {
                    List<Credential> credentials = new ArrayList<>();

                    for (QueryConfiguration configuration : queryConfiguration) {
                        for (KeyMapper keyMapper : configuration.getColumnMappers(KeyMapper.class)) {
                            credentials.add(executePrincipalQuery(configuration, r -> keyMapper.map(r, providers)));
                        }
                    }

                    return new JdbcIdentity(attributes, credentials);
                }).orElse(null);
            }

            return this.identity;
        }

        private Connection getConnection(QueryConfiguration configuration) {
            try {
                DataSource dataSource = configuration.getDataSource();
                return dataSource.getConnection();
            } catch (Exception e) {
                throw log.couldNotOpenConnection(e);
            }
        }

        private <E> E executePrincipalQuery(QueryConfiguration configuration, ResultSetCallback<E> resultSetCallback) {
            String sql = configuration.getSql();

            log.tracef("Executing principalQuery %s with value %s", sql, name);

            try (
                    Connection connection = getConnection(configuration);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)
            ) {
                preparedStatement.setString(1, name);

                try (
                        ResultSet resultSet = preparedStatement.executeQuery()
                ) {
                    return resultSetCallback.handle(resultSet);
                }
            } catch (SQLException e) {
                throw log.couldNotExecuteQuery(sql, e);
            } catch (Exception e) {
                throw log.unexpectedErrorWhenProcessingAuthenticationQuery(sql, e);
            }
        }

        private class JdbcIdentity {

            private final Attributes attributes;
            private List<Credential> credentials = new ArrayList<>();

            JdbcIdentity(Attributes attributes, List<Credential> credentials) {
                this.attributes = attributes;
                this.credentials = credentials;
            }
        }
    }

    private interface ResultSetCallback<E> {
        E handle(ResultSet resultSet) throws SQLException;
    }
}
