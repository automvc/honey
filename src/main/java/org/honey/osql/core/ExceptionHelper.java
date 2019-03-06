/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.core;

import java.sql.SQLException;

import org.bee.osql.BeeSQLException;
import org.bee.osql.exception.BeeIllegalAccessException;
import org.bee.osql.exception.BeeInstantiationException;

/**
 * @author Kingstar
 * @since  1.4
 */
public class ExceptionHelper {

	//convertSQLException
	public static BeeSQLException convert(SQLException e) {
		return new BeeSQLException(e.getMessage(),e.getSQLState(), e.getErrorCode(), e);
	}
	
	public static BeeIllegalAccessException convert(IllegalAccessException e) {
		return new BeeIllegalAccessException(e.getMessage(), e);
	}
	
	public static BeeInstantiationException convert(InstantiationException e) {
		return new BeeInstantiationException(e.getMessage(), e);
	}
}
