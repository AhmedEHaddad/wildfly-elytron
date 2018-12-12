package org.wildfly.security.auth.realm.ldap;

import org.wildfly.security.auth.server.RealmUnavailableException;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import java.util.Collection;

/**
 * Within LDAP credentials could be stored in different ways, splitting out a CredentialPersister allows different strategies to
 * be plugged into the realm.
 *
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public interface CredentialPersister extends CredentialLoader {

    /**
     * Obtain an {@link IdentityCredentialLoader} to query the credentials for a specific identity.
     *
     * Note: By this point referrals relating to the identity should have been resolved so the {@link DirContextFactory} should
     * be suitable for use with the supplied {@code distinguishedName}
     *
     * @param dirContext the {@link DirContext} to use to connect to LDAP.
     * @param distinguishedName the distinguished name of the identity.
     * @param attributes the identity attributes requested by {@link #addRequiredIdentityAttributes(Collection)}
     * @return An {@link IdentityCredentialLoader} for the specified identity identified by their distinguished name.
     */
    IdentityCredentialPersister forIdentity(DirContext dirContext, String distinguishedName, Attributes attributes) throws RealmUnavailableException;

}
