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
 * @author Kingstar
 * @since  2.4.0
 */
//TODO 是否需要使用本地线程, 让自定义组装器只是在当前线程有效????
//现在是全局范围有效; 且是用实体的Class绑定的Result组装器
//自定义的结果器能满足所有情况了吗?即可以替代默认的组装器了吗? 若可以,则用全局也行.
public class ResultAssemblerHandler {

	private static DefaultResultAssembler<?> defaultResultAssembler = new DefaultResultAssembler<>();

	public static <T> T rowToEntity(ResultSet rs, Class<T> clazz)
			throws SQLException, IllegalAccessException, InstantiationException {
		return getResultAssembler(clazz).rowToEntity(rs, clazz);
	}

	private static <T> ResultAssembler<T> getResultAssembler(Class<T> entityClass) {
		ResultAssembler<T> t = ResultAssemblerRegistry.getConverter(entityClass);
		if (t != null)
			return t;
		else
			return (ResultAssembler<T>) defaultResultAssembler;
	}

}
