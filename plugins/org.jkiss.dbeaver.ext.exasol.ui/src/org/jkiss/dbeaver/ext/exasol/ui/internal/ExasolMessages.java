/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

public class ExasolMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.exasol.ui.internal.ExasolResources"; //$NON-NLS-1$

	public static String dialog_table_tools_options;
	public static String dialog_table_tools_result;

	public static String dialog_connection_password;
	public static String dialog_connection_port;
	public static String dialog_connection_user_name;

	public static String dialog_consumer_precedence;
	public static String dialog_create_consumer_group;
	public static String dialog_consumer_group_name;
	public static String dialog_consumer_group_user_limit;
	public static String dialog_consumer_group_cpu_weight;
	public static String dialog_consumer_group_group_limit;
	public static String dialog_consumer_group_session_limit;
	public static String dialog_priority_group_description;
	public static String dialog_create_priority_group;
	public static String dialog_priority_group_name;
	public static String dialog_priority_group_weight;

	public static String dialog_create_user_userid;
	public static String dialog_create_user_comment;
	public static String dialog_create_user_kerberos;
	public static String dialog_create_user_ldap;
	public static String dialog_create_user_local;
	public static String dialog_create_user_local_password;
	public static String dialog_create_user_kerberos_principal;
	public static String dialog_create_user_ldap_dn;

	public static String edit_exasol_constraint_manager_dialog_title;

	public static String dialog_table_tools_progress;
	public static String dialog_table_tools_success_title;
	public static String dialog_table_open_input_directory;
	public static String dialog_table_open_output_directory;
	public static String dialog_table_tools_export_title;
	public static String dialog_table_tools_export_compress;
	public static String dialog_table_tools_column_heading;
	public static String dialog_table_tools_file_template;
	public static String dialog_table_tools_string_sep_mode;
	public static String dialog_table_tools_string_sep;
	public static String dialog_table_tools_column_sep;
	public static String dialog_table_tools_row_sep_mode;
	public static String dialog_table_tools_encoding;
	public static String dialog_table_tools_import_title;

	public static String editors_exasol_session_editor_title_kill_session;
	public static String editors_exasol_session_editor_action_kill;
	public static String editors_exasol_session_editor_confirm_action;
	public static String editors_exasol_session_editor_title_kill_session_statement;

	public static String exasol_partition_name;
	public static String exasol_partition_description;
	public static String label_backup_host_list;
	public static String label_database;
	public static String label_encrypt;
	public static String connection_page_checkbox_legacy_encrypt;
	public static String connection_page_checkbox_legacy_encrypt_tip;
	public static String label_host_list;
	public static String label_security;
	public static String label_use_backup_host_list;

	public static String dialog_create_connection_title;
	public static String dialog_create_connection_connection_name;
	public static String dialog_create_connection_connection_url;
	public static String dialog_create_connection_description;
	public static String dialog_create_connection_provide_credentials;
	public static String dialog_create_connection_provide_credentials_tip;
	public static String dialog_create_connection_user;
	public static String dialog_create_connection_password;

	public static String dialog_create_schema_title;
	public static String dialog_create_schema_schema_name;
	public static String dialog_create_schema_owner;

	public static String dialog_create_role_title;
	public static String dialog_create_role_role_name;
	public static String dialog_create_role_description;

	public static String dialog_create_foreign_key_title;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ExasolMessages.class);
	}

	private ExasolMessages() {
	}

}
