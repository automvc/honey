/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.database;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.teasoft.honey.jdbc.EmptyDataSource;
import org.teasoft.honey.mongodb.MongodbConnection;

/**
 * @author Kingstar
 * @since  2.0
 */
public class ClientDataSource extends EmptyDataSource implements Closeable {

	public Object getDbConnection() {
		return null;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public Connection getConnection() throws SQLException { // 为了兼容JDBC
		return new MongodbConnection();
	}

}
