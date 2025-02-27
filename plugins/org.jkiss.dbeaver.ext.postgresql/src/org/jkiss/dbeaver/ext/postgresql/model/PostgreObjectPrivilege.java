/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;

/**
 * PostgreObjectPrivilege
 */
public class PostgreObjectPrivilege extends PostgrePrivilege {

    private static final Log log = Log.getLog(PostgreObjectPrivilege.class);

    private final PostgreRoleReference grantee;

    public PostgreObjectPrivilege(PostgrePrivilegeOwner owner, PostgreRoleReference grantee, List<PostgrePrivilegeGrant> privileges) {
        super(owner, privileges);
        this.grantee = grantee;
    }

    @Property(viewable = true, order = 1)
    @NotNull
    public String getName() {
        return grantee == null ? "" : grantee.getDisplayString();
    }

    @Override
    public PostgreRole getTargetObject(DBRProgressMonitor monitor) throws DBException {
        return owner.getDatabase().getRoleByReference(monitor, this.grantee);
    }

    public PostgreRoleReference getGrantee() {
        return grantee;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(@NotNull PostgrePrivilege o) {
        if (o instanceof PostgreObjectPrivilege other) {
            return this.grantee.compareTo(other.grantee);
        } else {
            return 0;
        }
    }

}

