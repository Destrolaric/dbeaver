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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.generator.SQLGeneratorContributor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class SQLEditorHandlerOpenObjectConsole extends AbstractHandler {

    private static final Log log = Log.getLog(SQLEditorHandlerOpenObjectConsole.class);

    private static final boolean OPEN_FILE_EDITOR = false;

    public SQLEditorHandlerOpenObjectConsole()
    {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        SQLNavigatorContext navContext = null;

        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(currentSelection);
        List<DBSEntity> entities = new ArrayList<>();
        for (DBSObject object : selectedObjects) {
            if (navContext == null) {
                navContext = new SQLNavigatorContext(object);
            }
            if (object instanceof DBSEntity) {
                entities.add((DBSEntity) object);
            }
            if (object instanceof DBNDatabaseFolder) {
                DBSObject parentObject = object.getParentObject(); // It can be table properties window or database navigator table folder (columns, keys etc.)
                if (parentObject instanceof DBSEntity) {
                    entities.add((DBSEntity) parentObject);
                }
            }
        }
        if (navContext == null || navContext.getDataSourceContainer() == null) {
            log.debug("No active datasource");
            return null;
        }
        DBRRunnableWithResult<String> generator = SQLGeneratorContributor.SELECT_GENERATOR(entities, true);
        String title = "Query";
        if (entities.size() == 1) {
            title = DBUtils.getObjectFullName(entities.get(0), DBPEvaluationContext.DML);
        }
        try {
            openConsole(workbenchWindow, generator, navContext, title, !entities.isEmpty(), currentSelection);
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Open console", "Can open SQL editor", e);
        }
        return null;
    }

    void openConsole(IWorkbenchWindow workbenchWindow, DBRRunnableWithResult<String> generator,
                     SQLNavigatorContext navigatorContext, String title, boolean doRun, ISelection currentSelection) throws Exception {
        UIUtils.runInUI(workbenchWindow, generator);
        String sql = CommonUtils.notEmpty(generator.getResult());

        openAndExecuteSQLScript(workbenchWindow, navigatorContext, title, doRun, currentSelection, sql);
    }
    
    /**
     * openAndExecuteSQLScript in a SQL window
     */
    public static void openAndExecuteSQLScript(
        IWorkbenchWindow workbenchWindow,
        SQLNavigatorContext navigatorContext,
        String title,
        boolean doRun,
        ISelection currentSelection,
        String sql
    ) throws CoreException {
        openAndExecuteSQLScript(
            workbenchWindow,
            navigatorContext,
            title,
            doRun,
            currentSelection,
            sql, 
            false);
    }

    /**
     * openAndExecuteSQLScript with force sqlScript executing
     */
    public static void openAndExecuteSQLScript(
        IWorkbenchWindow workbenchWindow,
        SQLNavigatorContext navigatorContext,
        String title,
        boolean doRun,
        ISelection currentSelection,
        String sql,
        boolean forceProcessAsScript
    ) throws CoreException {
        SQLEditor editor;
        DBPProject project = navigatorContext.getProject();
        if (OPEN_FILE_EDITOR && project != null && project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT)) {
            SQLEditorHandlerOpenEditor.checkProjectIsOpen(project);
            IFolder folder = SQLEditorHandlerOpenEditor.getCurrentScriptFolder(currentSelection);
            IFile scriptFile = SQLEditorUtils.createNewScript(project, folder, navigatorContext);

            FileEditorInput sqlInput = new FileEditorInput(scriptFile);
            editor = (SQLEditor) workbenchWindow.getActivePage().openEditor(sqlInput, SQLEditor.class.getName());
        } else {
            editor = SQLEditorHandlerOpenEditor.openSQLConsole(workbenchWindow, navigatorContext, title, sql);
        }

        if (editor != null && editor.getDocument() != null) {
            editor.getDocument().set(sql);
            AbstractJob execJob = new AbstractJob("Execute SQL in console") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    // If we open new connection for each editor it may take some time
                    // So let's give it a chance and wait for 10 seconds
                    if (editor.getExecutionContext() != null) {
                        if (doRun) {
                            UIUtils.syncExec(() -> editor.processSQL(
                                false,
                                forceProcessAsScript || NavigatorUtils.getSelectedObjects(currentSelection).size() > 1
                            ));
                        }
                        return Status.OK_STATUS;
                    }
                    schedule(100);
                    return Status.OK_STATUS;
                }
            };
            execJob.schedule();
        }
    }
}
