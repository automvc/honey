/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.database;

import java.io.Closeable;
import java.io.IOException;

import org.teasoft.honey.database.ClientDataSource;

/**
 * @author Kingstar
 * @since  2.0
 */
public class DatabaseClientConnection implements ClientConnection, Closeable {
//	DatabaseClientConnection 同时持有连接和关闭两个方法

	private ClientDataSource ds;

	public DatabaseClientConnection(ClientDataSource clientDs) {
		this.ds = clientDs;
	}

	@Override
	public Object getDbConnection() { // like MongoDatabase
		return ds.getDbConnection();
	}

	@Override
	public void close() throws IOException {
		ds.close();
	}
}
