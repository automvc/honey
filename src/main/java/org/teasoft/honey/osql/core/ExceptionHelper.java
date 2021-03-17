/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.sql.SQLException;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.BeeSQLException;
//import org.teasoft.bee.osql.exception.BeeIllegalAccessException;
//import org.teasoft.bee.osql.exception.BeeInstantiationException;

/**
 * @author Kingstar
 * @since  1.4
 */
public class ExceptionHelper {

	//convertSQLException
	public static BeeSQLException convert(SQLException e) {
		return new BeeSQLException(e.getMessage(),e.getSQLState(), e.getErrorCode(), e);
	}
	
//	public static BeeIllegalAccessException convert(IllegalAccessException e) {
//		return new BeeIllegalAccessException(e.getMessage(), e);
//	}
	
//	public static BeeInstantiationException convert(InstantiationException e) {
//		return new BeeInstantiationException(e.getMessage(), e);
//	}
	
	public static BeeException convert(Exception e) {
		return new BeeException(e.getMessage(), e);
	}
}
