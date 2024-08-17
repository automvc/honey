/*
 * Copyright 2019-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.teasoft.bee.osql.ResultAssembler;
import org.teasoft.bee.osql.ResultAssemblerRegistry;

/**
 * Select ResultSet Assembler Handler.
 * @author Kingstar
 * @since  2.4.0
 */
public class ResultAssemblerHandler {

	private static DefaultResultAssembler<?> defaultResultAssembler = new DefaultResultAssembler<>();

	public static <T> T rowToEntity(ResultSet rs, Class<T> clazz)
			throws SQLException, IllegalAccessException, InstantiationException {
		return getResultAssembler(clazz).rowToEntity(rs, clazz);
	}

	@SuppressWarnings("unchecked")
	private static <T> ResultAssembler<T> getResultAssembler(Class<T> entityClass) {
		ResultAssembler<T> t = ResultAssemblerRegistry.getResultAssembler(entityClass);
		if (t != null)
			return t;
		else
			return (ResultAssembler<T>) defaultResultAssembler;
	}

}
