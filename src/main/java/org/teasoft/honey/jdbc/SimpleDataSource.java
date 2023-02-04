/*
 * Copyright 2020-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.SessionFactory;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  2.0
 */
public class SimpleDataSource extends EmptyDataSource {

	private String url;
	private String username;
	private String password;
	private String driverName;

	private int counter = 0;

	private volatile boolean inited = false;

	private Connection conn = null;

	public SimpleDataSource() {
		counter = 0;
	};

	public SimpleDataSource(String url, String username, String pwd0) {
		counter = 0;
		this.url = url;
		this.username = username;
		this.password = pwd0;

		init();
	}

	public void init() {
		if (inited) return;
		if (counter >= 1) inited = true;
	}

	@Override
	public Connection getConnection() throws SQLException {

		try {
			if (!inited) Logger.warn("Do not config the connection info!");
			// have not ds pool, will gen conn every time.
			this.conn = SessionFactory.getOriginalConnForIntra(getUrl(), getUsername(), getPassword(), getDriverName());
			inited = true;
		} catch (Exception ex) {
			Logger.warn(ex.getMessage(), ex);
		}

		return this.conn;
	}

	// get,set

	public String getUrl() {
		//Ms Access
		if(StringUtils.isNotBlank(this.password) && url!=null && url.startsWith("jdbc:ucanaccess:") && !url.contains("jackcessOpener=")) {
			return url+=";jackcessOpener=org.teasoft.beex.access.BeeAccessCryptOpener";
		}
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
		counter= 1;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverName() {
		return driverName;
	}

	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}

}
