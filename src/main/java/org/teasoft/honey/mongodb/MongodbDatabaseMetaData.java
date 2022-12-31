/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.mongodb;

import java.sql.SQLException;

import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.honey.jdbc.EmptyDatabaseMetaData;

/**
 * @author Kingstar
 * @since  2.0
 */
public class MongodbDatabaseMetaData extends EmptyDatabaseMetaData {
	
	@Override
	public String getDatabaseProductName() throws SQLException {
//		Logger.info("------------------ getDatabaseProductName in MongodbDatabaseMetaData");
		return DatabaseConst.MongoDB; //为了兼容JDBC
	}
}
