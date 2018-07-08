/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.core;

import org.bee.osql.ObjSQLException;
import org.bee.osql.ObjToSQL;

/**
 * 对象到SQL语句的转化 (标准sql)
 * @author KingStar
 * @since  1.0
 */
public class ObjectToSQL implements ObjToSQL {
	// copy from ConverString
	// fields[i].setAccessible(true); 看是否可以用private
	// ////////////////test Object -- > SQL

	@Override
	public <T> String toInsertSQL(T entity) {
		// return _toInsertSQL(entity,false);
		String sql = null;
		try {
			sql = _ObjectToSQLHelper._toInsertSQL(entity, -1);
		} catch (IllegalAccessException e) {
			System.err.println("In ObjectToSQL  -----------IllegalAccessException:  "+ e.getMessage());
		}
		return sql;
	}

	@Override
	public <T> String toSelectSQL(T entity) {
		return _ObjectToSQLHelper._toSelectSQL(entity, -1); // 过滤NULL和空字符串
	}

	@Override
	public <T> String toDeleteSQL(T entity) {
		return _ObjectToSQLHelper._toDeleteSQL(entity, -1);
	}

	@Override
	public <T> String toUpdateSQL(T entity) throws ObjSQLException {
		String sql = null;
		try {
			sql = _ObjectToSQLHelper._toUpdateSQL(entity, "id", -1);
			// Logger.logSQL("update SQL : ", sql);
		} catch (IllegalAccessException e) {
			System.err.println("In ObjectToSQL  -----------IllegalAccessException:  "+ e.getMessage());
		}
		return sql;
	}

}
