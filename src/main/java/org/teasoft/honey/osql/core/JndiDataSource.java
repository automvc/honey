/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Jndi DataSource.
 * @author Kingstar
 * @since  1.11
 */
public class JndiDataSource {

	private DataSource dataSource;

	private String jndiName;

	public JndiDataSource() {
		initJndiDs();
	}

	public String getJndiName() {
		if (jndiName == null) {
			jndiName = "java:comp/env/" + HoneyConfig.getHoneyConfig().jndiName;
		}
		return jndiName;
	}

	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	private void initJndiDs() {
		try {
//			String jndiName = "jdbc/Bee";
			Context ctx = new InitialContext();
			dataSource = (DataSource) ctx.lookup(getJndiName());
			Logger.info("[Bee] ==========get the DataSource with Jndi Type!");
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}
	}

	public DataSource getDataSource() {
		return dataSource;
	}
}
