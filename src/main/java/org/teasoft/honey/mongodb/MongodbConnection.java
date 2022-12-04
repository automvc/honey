/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.mongodb;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.teasoft.honey.jdbc.EmptyConnection;

/**
 * @author Kingstar
 * @since  2.0
 */
public class MongodbConnection extends EmptyConnection {

	private static MongodbDatabaseMetaData metaData = new MongodbDatabaseMetaData();

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return metaData;
	}
}
