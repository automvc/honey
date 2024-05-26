/*
 * Copyright 2019-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.teasoft.bee.osql.ResultAssembler;

/**
 * @author Kingstar
 * @since  2.4.0
 */
public class DefaultResultAssembler<T> implements ResultAssembler<T> {

	@Override
	public T rowToEntity(ResultSet rs, Class<T> entityClass)
			throws SQLException, IllegalAccessException, InstantiationException {
		return TransformResultSet.rowToEntity(rs, entityClass);
	}
}
