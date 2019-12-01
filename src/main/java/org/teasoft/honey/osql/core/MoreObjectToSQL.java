/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.MoreObjToSQL;

/**
 * @author Kingstar
 * @since  1.7
 */
public class MoreObjectToSQL implements MoreObjToSQL{

	@Override
	public <T> String toSelectSQL(T entity) {
		return _MoreObjectToSQLHelper._toSelectSQL(entity); // 默认过滤NULL和空字符串
	}

	@Override
	public <T> String toSelectSQL(T entity, int start, int size) {
		return _MoreObjectToSQLHelper._toSelectSQL(entity,start,size); // 默认过滤NULL和空字符串
	}

	@Override
	public <T> String toSelectSQL(T entity, Condition condition) {
		return _MoreObjectToSQLHelper._toSelectSQL(entity,condition); // 若condition没有设置IncludeType,默认过滤NULL和空字符串
	}
	
}
