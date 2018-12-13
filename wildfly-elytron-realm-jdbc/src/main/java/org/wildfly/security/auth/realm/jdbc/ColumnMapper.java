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

import java.security.Provider;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * A column mapper is responsible to provide the mapping between a column in a table to some internal representation. For instance,
 * mapping a column to a specific credential type or attribute.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ColumnMapper {

    /**
     * Maps the given {@link ResultSet} to some internal representation.
     *
     * @param resultSet the result set previously created based on a query
     * @param providers the providers to use if required
     * @return the resulting object mapped from the given {@link ResultSet}
     * @throws SQLException if any error occurs when manipulating the given {@link ResultSet}
     */
    Object map(ResultSet resultSet, Supplier<Provider[]> providers) throws SQLException;
}
