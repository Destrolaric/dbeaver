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
package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskConstants;
import org.jkiss.dbeaver.registry.task.TaskImpl;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.tasks.ui.registry.TaskUIRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

/**
 * Create task wizard page
 */
class TaskConfigurationWizardPageTask extends ActiveWizardPage<TaskConfigurationWizard> {
    private static final Log log = Log.getLog(TaskConfigurationWizardPageTask.class);

    private final DBPProject selectedProject;
    private Text taskLabelText;
    private Text taskDescriptionText;
    private Spinner maxExecutionTime;
    private Button maxExecutionTimeBtn;
    private Tree taskCategoryTree;
    private Combo taskFoldersCombo;

    private DBTTaskCategory selectedCategory;
    private DBTTaskType selectedTaskType;
    private String taskName;
    private String taskDescription;
    private String selectedTaskFolderName;
    private Map<String, Object> initialProperties = new LinkedHashMap<>();

    private TaskImpl task;
    private boolean filterTaskTypes = true;

    private Map<DBTTaskType, TaskConfigurationWizard> taskWizards = new HashMap<>();

    TaskConfigurationWizardPageTask(DBTTask task) {
        super(task == null ? TaskUIMessages.task_config_wizard_page_settings_create_task : TaskUIMessages.task_config_wizard_page_settings_edit_task);
        setTitle(task == null ? TaskUIMessages.task_config_wizard_page_task_title_new_task_prop : TaskUIMessages.task_config_wizard_page_settings_title_task_prop);
        setDescription(TaskUIMessages.task_config_wizard_page_settings_descr_set_task);

        this.task = (TaskImpl) task;
        if (this.task != null) {
            this.taskName = this.task.getName();
            this.taskDescription = this.task.getDescription();
            this.selectedTaskFolderName = this.task.getTaskFolder() != null ? this.task.getTaskFolder().getName() : null;
            this.selectedTaskType = this.task.getType();
            this.selectedCategory = selectedTaskType.getCategory();
        }
        this.selectedProject = NavigatorUtils.getSelectedProject();
        setPageComplete(this.task != null);
    }

    public DBTTaskCategory getSelectedCategory() {
        return selectedCategory;
    }

    public DBTTaskType getSelectedTaskType() {
        return selectedTaskType;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public Map<String, Object> getInitialProperties() {
        return initialProperties;
    }

    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    @Override
    public void createControl(Composite parent) {
        boolean taskSaved = task != null && !CommonUtils.isEmpty(task.getId());

        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite formPanel = UIUtils.createControlGroup(composite, TaskUIMessages.task_config_wizard_page_task_label_task_type, task == null ? 1 : 2, GridData.FILL_BOTH, 0);
            formPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            {
                Composite infoPanel = UIUtils.createComposite(formPanel, 2);
                infoPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                ModifyListener modifyListener = e -> updatePageCompletion();

                taskLabelText = UIUtils.createLabelText(infoPanel, TaskUIMessages.task_config_wizard_page_task_text_label_name, task == null ? "" : CommonUtils.notEmpty(task.getName()), SWT.BORDER);
                if (taskSaved) {
                    taskLabelText.setEditable(false);
                    //taskLabelText.setEnabled(false);
                }
                taskLabelText.addModifyListener(e -> {
                    taskName = taskLabelText.getText();
                    modifyListener.modifyText(e);
                });

                UIUtils.createControlLabel(infoPanel, TaskUIMessages.task_config_wizard_page_task_control_label_descr).setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

                taskDescriptionText = new Text(infoPanel, SWT.BORDER | SWT.MULTI);
                taskDescriptionText.setText(task == null ? "" : CommonUtils.notEmpty(task.getDescription()));
                taskDescriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                //((GridData) taskDescriptionText.getLayoutData()).heightHint = taskDescriptionText.getLineHeight() * 6;
                taskDescriptionText.addModifyListener(e -> {
                    taskDescription = taskDescriptionText.getText();
                    modifyListener.modifyText(e);
                });

                UIUtils.createControlLabel(infoPanel, TaskUIMessages.task_config_wizard_page_task_create_folder_label);
                taskFoldersCombo = new Combo(infoPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
                taskFoldersCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                // Show only selected project folders list (not all projects folders) to avoid folder naming mess
                // (Because we can create folders with equal names in different projects)
                DBTTaskManager taskManager = selectedProject.getTaskManager();
                DBTTaskFolder[] tasksFolders = taskManager.getTasksFolders();
                if (!ArrayUtils.isEmpty(tasksFolders)) {
                    taskFoldersCombo.add(""); // Empty row as ability to remove task folder from task
                    for (DBTTaskFolder taskFolder : tasksFolders) {
                        taskFoldersCombo.add(taskFolder.getName());
                    }
                }

                // Set current task folder name for Edit dialog or selected folder from new created from context menu task
                if (task != null && task.getTaskFolder() != null) {
                    taskFoldersCombo.setText(task.getTaskFolder().getName());
                } else if (task == null) {
                    TaskConfigurationWizard wizard = getWizard();
                    if (wizard != null) {
                        DBTTaskFolder currentSelectedTaskFolder = wizard.getCurrentSelectedTaskFolder();
                        if (currentSelectedTaskFolder != null) {
                            taskFoldersCombo.setText(currentSelectedTaskFolder.getName());
                        }
                    }
                }

                // Check task folder changing in Edit task dialog
                if (task != null) {
                    taskFoldersCombo.addModifyListener(e -> {
                        String foldersComboText = taskFoldersCombo.getText();
                        if (task.getTaskFolder() == null || !foldersComboText.equals(task.getTaskFolder().getName())) {
                            selectedTaskFolderName = foldersComboText;
                        }
                    });
                }

                if (task != null && !CommonUtils.isEmpty(task.getId())) {
                    UIUtils.createLabelText(infoPanel, TaskUIMessages.task_config_wizard_page_task_text_label_task_id, task.getId(), SWT.BORDER | SWT.READ_ONLY);
                }

                UIUtils.asyncExec(() -> {
                    Text widgetToFocus = taskSaved ? taskDescriptionText : taskLabelText;
                    if (widgetToFocus != null && !widgetToFocus.isDisposed()) {
                        widgetToFocus.setFocus();
                    }
                });

                if (task != null) {
                    UIUtils.createControlLabel(infoPanel, TaskUIMessages.task_config_wizard_page_task_control_label_category);
                    Composite catPanel = UIUtils.createComposite(infoPanel, 2);
                    DBTTaskType taskType = task.getType();
                    UIUtils.createLabel(catPanel, taskType.getCategory().getIcon());
                    UIUtils.createLabel(catPanel, taskType.getCategory().getName());

                    UIUtils.createControlLabel(infoPanel, TaskUIMessages.task_config_wizard_page_task_control_label_type);
                    Composite typePanel = UIUtils.createComposite(infoPanel, 2);
                    UIUtils.createLabel(typePanel, taskType.getIcon());
                    UIUtils.createLabel(typePanel, taskType.getName());
                }
            }
            Composite advancedPanel = UIUtils.createControlGroup(
                composite,
                TaskUIMessages.task_config_wizard_page_task_advanced_label, 2,
                GridData.FILL_HORIZONTAL, 0);
            maxExecutionTimeBtn = UIUtils.createCheckbox(
                advancedPanel,
                TaskUIMessages.task_config_wizard_page_task_max_exec_time,
                TaskUIMessages.task_config_wizard_page_task_max_exec_time_descr,
                true,
                1);
            maxExecutionTime = UIUtils.createSpinner(advancedPanel, null, TaskConstants.DEFAULT_MAX_EXECUTION_TIME, 1, Integer.MAX_VALUE);
            maxExecutionTimeBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(
                    SelectionEvent e) {
                    maxExecutionTime.setEnabled(maxExecutionTimeBtn.getSelection());
                    if (!maxExecutionTimeBtn.getSelection()) {
                        maxExecutionTime.setSelection(0);
                    }
                }
            });
            maxExecutionTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            if (task != null && task.getMaxExecutionTime() != 0) {
                maxExecutionTimeBtn.setSelection(true);
                maxExecutionTime.setEnabled(true);
                maxExecutionTime.setSelection(task.getMaxExecutionTime());
            } else {
                maxExecutionTimeBtn.setSelection(false);
                maxExecutionTime.setEnabled(false);
                maxExecutionTime.setSelection(TaskConstants.DEFAULT_MAX_EXECUTION_TIME);
            }

            if (task == null) {
                taskCategoryTree = new Tree(formPanel, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
                GridData gd = new GridData(GridData.FILL_BOTH);
                gd.heightHint = 100;
                gd.widthHint = 200;
                taskCategoryTree.setLayoutData(gd);
                taskCategoryTree.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        TreeItem[] selection = taskCategoryTree.getSelection();
                        if (selection.length == 1) {
                            Object itemData = selection[0].getData();
                            if (itemData instanceof DBTTaskType) {
                                if (selectedTaskType == itemData) {
                                    return;
                                }
                                selectedTaskType = (DBTTaskType) itemData;
                                selectedCategory = selectedTaskType.getCategory();
                            } else {
                                if (selectedCategory == itemData && selectedTaskType == null) {
                                    return;
                                }
                                selectedCategory = (DBTTaskCategory) itemData;
                                selectedTaskType = null;
                            }
                            updateTaskTypeSelection();
                        }
                    }
                });
                TreeColumn nameColumn = new TreeColumn(taskCategoryTree, SWT.LEFT);
                nameColumn.setText("Task");
                TreeColumn descColumn = new TreeColumn(taskCategoryTree, SWT.RIGHT);
                descColumn.setText("Description");
                addTaskCategories(null, TaskRegistry.getInstance().getRootCategories());
                taskCategoryTree.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseDoubleClick(MouseEvent e) {
                        if (canFlipToNextPage()) {
                            getWizard().getContainer().buttonPressed(IDialogConstants.NEXT_ID);
                        }
                    }
                });
                UIUtils.asyncExec(() -> UIUtils.packColumns(taskCategoryTree, true, new float[] { 0.3f, 0.7f}));
                taskCategoryTree.addControlListener(new ControlAdapter() {
                    @Override
                    public void controlResized(ControlEvent e) {
                        taskCategoryTree.removeControlListener(this);
                        UIUtils.packColumns(taskCategoryTree, true, new float[] { 0.3f, 0.7f });
                    }
                });
                taskCategoryTree.addPaintListener(e -> {
                    if (taskCategoryTree.getItemCount() == 0) {
                        UIUtils.drawMessageOverControl(taskCategoryTree, e, TaskUIMessages.task_config_wizard_page_task_no_task_types_available, 0);
                    }
                });

            }
        }

        updatePageCompletion();
        setControl(composite);
    }

    private void updateTaskTypeSelection() {
        if (task != null && selectedTaskType != null) {
            if (task.getType() != selectedTaskType) {
                task.setType(selectedTaskType);
                task.setProperties(new LinkedHashMap<>());
            }
        }

        updatePageCompletion();
        getShell().layout(true, true);
        getWizard().getContainer().updateButtons();
    }

    private void addTaskCategories(TreeItem parentItem, DBTTaskCategory[] categories) {
        List<DBTTaskCategory> allCats = Arrays.asList(categories);
        allCats.sort(Comparator.comparing(DBTTaskCategory::getName));

        for (DBTTaskCategory cat : categories) {
            if (!isTaskCategoryApplicable(cat)) {
                continue;
            }
            TreeItem item = parentItem == null ? new TreeItem(taskCategoryTree, SWT.NONE) : new TreeItem(parentItem, SWT.NONE);
            item.setText(0, cat.getName());
            item.setImage(0, DBeaverIcons.getImage(cat.getIcon() == null ? DBIcon.TREE_TASK : cat.getIcon()));
            item.setText(1, CommonUtils.notEmpty(cat.getDescription()));
            item.setData(cat);
            addTaskCategories(item, cat.getChildren());
            addTaskTypes(item, cat);
            item.setExpanded(true);
        }
    }

    private void addTaskTypes(TreeItem parentItem, DBTTaskCategory category) {
        DBTTaskType[] taskTypes = category.getTaskTypes();
        Arrays.sort(taskTypes, Comparator.comparing(DBTTaskType::getName));
        for (DBTTaskType type : taskTypes) {
            if (!isTaskTypeApplicable(type)) {
                continue;
            }

            TreeItem item = new TreeItem(parentItem, SWT.NONE);
            item.setText(0, type.getName());
            item.setText(1, CommonUtils.notEmpty(type.getDescription()));
            if (type.getIcon() != null) {
                item.setImage(0, DBeaverIcons.getImage(type.getIcon()));
            }
            item.setData(type);
        }
    }

    private boolean isTaskCategoryApplicable(DBTTaskCategory category) {
        if (!filterTaskTypes) {
            return true;
        }
        for (DBTTaskCategory child : category.getChildren()) {
            if (isTaskCategoryApplicable(child)) {
                return true;
            }
        }
        for (DBTTaskType child : category.getTaskTypes()) {
            if (isTaskTypeApplicable(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTaskTypeApplicable(DBTTaskType type) {
        if (!filterTaskTypes || selectedProject == null || !selectedProject.isRegistryLoaded()) {
            return true;
        }
        if (type.isStandalone()) {
            return true;
        }
        for (DBPDataSourceContainer ds : selectedProject.getDataSourceRegistry().getDataSources()) {
            if (type.isDriverApplicable(ds.getDriver())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean determinePageCompletion() {
        if (CommonUtils.isEmpty(taskName)) {
            setErrorMessage(TaskUIMessages.task_configuration_wizard_page_task_error_message_enter_task_name);
            return false;
        }
        if (task == null) {
            DBTTask task2 = selectedProject.getTaskManager().getTaskByName(taskName);
            if (task2 != null) {
                setErrorMessage(NLS.bind(TaskUIMessages.task_configuration_wizard_page_task_already_exists, taskName, selectedProject.getName()));
                return false;
            }
        }
        if (selectedTaskType == null) {
            setErrorMessage(TaskUIMessages.task_configuration_wizard_page_task_enter_type);
            return false;
        }
        setErrorMessage(null);
        return true;
    }

    public TaskConfigurationWizard getTaskWizard() throws DBException {
        if (!(getWizard() instanceof NewTaskConfigurationWizard)) {
            // We already have it
            return getWizard();
        }
        TaskConfigurationWizard realWizard = taskWizards.get(selectedTaskType);
        if (realWizard == null) {
            DBTTaskConfigurator configurator = TaskUIRegistry.getInstance().createConfigurator(selectedTaskType);

            String taskFolderName = taskFoldersCombo.getText();

            if (task == null) {
                task = (TaskImpl) selectedProject.getTaskManager().createTask(selectedTaskType, CommonUtils.notEmpty(taskName), taskDescription, taskFolderName, new LinkedHashMap<>());
            }
            realWizard = configurator.createTaskConfigWizard(task);
            IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            IWorkbenchPart activePart = workbenchWindow.getActivePage().getActivePart();
            ISelection selection = activePart == null || activePart.getSite() == null || activePart.getSite().getSelectionProvider() == null ?
                null : activePart.getSite().getSelectionProvider().getSelection();
            realWizard.setContainer(getContainer());
            realWizard.init(workbenchWindow.getWorkbench(), selection instanceof IStructuredSelection ? (IStructuredSelection) selection : null);

            taskWizards.put(selectedTaskType, realWizard);
        }
        return realWizard;
    }

    public void saveSettings() {
        if (task != null) {
            task.setName(taskLabelText.getText());
            task.setDescription(taskDescriptionText.getText());
            task.setType(selectedTaskType);
            // Change task folder in edit task case
            if (selectedTaskFolderName != null) {
                DBTTaskFolder[] tasksFolders = selectedProject.getTaskManager().getTasksFolders();
                List<DBTTaskFolder> taskFoldersList = Arrays.asList(tasksFolders != null ? tasksFolders : new DBTTaskFolder[0]);
                DBTTaskFolder folder = DBUtils.findObject(taskFoldersList, selectedTaskFolderName);
                DBTTaskFolder currentTaskFolder = task.getTaskFolder();
                if (folder != null) {
                    task.setTaskFolder(folder);
                    folder.addTaskToFolder(task);
                } else {
                    task.setTaskFolder(null);
                }
                if (currentTaskFolder != null) {
                    currentTaskFolder.removeTaskFromFolder(task);
                }
                TaskRegistry.getInstance().notifyTaskFoldersListeners(new DBTTaskFolderEvent(folder, DBTTaskFolderEvent.Action.TASK_FOLDER_REMOVE));
            }
            if (maxExecutionTimeBtn.getSelection()) {
                task.setMaxExecutionTime(maxExecutionTime.getSelection());
            } else {
                task.setMaxExecutionTime(0);
            }
        }
    }

}
