/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractScriptExecuteSettings;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;

public class PostgreScriptExecuteSettings extends AbstractScriptExecuteSettings<DBSObject> {

    private static final Log log = Log.getLog(PostgreScriptExecuteSettings.class);

    private PostgreDatabase database;

    public PostgreDatabase getDatabase() {
        return database;
    }

    public PostgreScriptExecuteSettings() {
    }

    public PostgreScriptExecuteSettings(@NotNull DBPProject project) {
        super(project);
    }

    public void setDatabase(PostgreDatabase database) {
        this.database = database;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
        super.loadSettings(runnableContext, store);
        String databaseId = null;
        if (store instanceof DBPPreferenceMap) {
            databaseId = store.getString("pg.script.database");
        }
        if (!CommonUtils.isEmpty(databaseId)) {
            try {
                String finalDatabaseId = databaseId;
                runnableContext.run(true, true, monitor -> {
                    try {
                        database = (PostgreDatabase) DBUtils.findObjectById(monitor, getProject(), finalDatabaseId);
                        if (database == null) {
                            throw new DBException("Database " + finalDatabaseId + " not found");
                        }
                    } catch (Throwable e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                log.error("Error loading objects configuration", e);
            } catch (InterruptedException e) {
                // Ignore
            }
        } else {
            findDatabase();
        }

        if (database == null) {
            throw new DBException("Cannot find database for script execution");
        }
    }

    private void findDatabase() {
        for (DBSObject object : getDatabaseObjects()) {
            if (object instanceof PostgreDatabase) {
                database = (PostgreDatabase) object;
                break;
            }
        }
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.saveSettings(runnableContext, store);
        if (database == null) {
            findDatabase();
        }
        store.setValue("pg.script.database", DBUtils.getObjectFullId(database));
    }
}
