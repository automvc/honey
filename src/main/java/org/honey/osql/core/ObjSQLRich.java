/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.core;

import java.util.List;

import org.bee.osql.FunctionType;
import org.bee.osql.IncludeType;
import org.bee.osql.ObjSQLException;
import org.bee.osql.ObjToSQLRich;
import org.bee.osql.OrderType;
import org.bee.osql.SQL;
import org.bee.osql.SuidRich;

/**
 * @author KingStar
 * @since  1.0
 */
public class ObjSQLRich extends ObjSQL implements SuidRich {

	private ObjToSQLRich objToSQLRich = BeeFactory.getHoneyFactory().getObjToSQLRich();
	private SQL sqlLib = BeeFactory.getHoneyFactory().getSQL();

	@Override
	public <T> List<T> select(T entity, int size) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectSQL(entity, size);
		return sqlLib.select(sql, entity);
	}

	@Override
	public <T> List<T> select(T entity, int from, int size) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectSQL(entity, from, size);
		return sqlLib.select(sql, entity);
	}

	@Override
	public <T> List<T> select(T entity, String selectField) {//sqlLib.selectSomeField
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = objToSQLRich.toSelectSQL(entity, selectField);
			list = sqlLib.selectSomeField(sql, entity);
		} catch (ObjSQLException e) {
			System.err.println(e.getMessage());
		}

		return list;
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFieldList) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = objToSQLRich.toSelectOrderBySQL(entity, orderFieldList);
			Logger.logSQL("selectOrderBy SQL: ", sql);
			list = sqlLib.select(sql, entity);
		} catch (ObjSQLException e) {
			System.err.println(e.getMessage());
		}

		return list;
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFieldList, OrderType[] orderTypes) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = objToSQLRich.toSelectOrderBySQL(entity, orderFieldList, orderTypes);
			Logger.logSQL("selectOrderBy SQL: ", sql);
			list = sqlLib.select(sql, entity);
		} catch (ObjSQLException e) {
			System.err.println(e.getMessage());
		}

		return list;
	}

	@Override
	public <T> int[] insert(T entity[]) {
		if (entity == null) return null;
		int len = entity.length;
		String insertSql[] = new String[len];
		insertSql = objToSQLRich.toInsertSQL(entity);

		return sqlLib.batch(insertSql);
	}

	@Override
	public <T> int[] insert(T entity[], String excludeFieldList) {
		if (entity == null) return null;
		int len = entity.length;
		String insertSql[] = new String[len];
		insertSql = objToSQLRich.toInsertSQL(entity, excludeFieldList);

		return sqlLib.batch(insertSql);
	}

	@Override
	public <T> int[] insert(T entity[], int batchSize) {
		if (entity == null) return null;
		int len = entity.length;
		String insertSql[] = new String[len];
		insertSql = objToSQLRich.toInsertSQL(entity);

		return sqlLib.batch(insertSql, batchSize);
	}

	@Override
	public <T> int[] insert(T entity[], int batchSize, String excludeFieldList) {
		if (entity == null) return null;
		int len = entity.length;
		String insertSql[] = new String[len];
		insertSql = objToSQLRich.toInsertSQL(entity, excludeFieldList);

		return sqlLib.batch(insertSql, batchSize);
	}

	@Override
	public <T> int update(T entity, String updateFieldList) {
		if (entity == null) return 0;
		int r = 0;
		try {
			String sql = objToSQLRich.toUpdateSQL(entity, updateFieldList);
			Logger.logSQL("update SQL(updateFieldList) :", sql);
			r = sqlLib.modify(sql);
		} catch (ObjSQLException e) {
			System.err.println(e.getMessage());
		}

		return r;
	}

	@Override
	public <T> T selectOne(T entity) {
		if (entity == null) return null;
		List<T> list = select(entity);
		if (list == null || list.size() != 1) return null;

		return list.get(0);
	}

	@Override
	public <T> String selectWithFun(T entity, String fieldForFun, FunctionType functionType) {
		if (entity == null) return null;
		String s = null;
		try {
			String sql = objToSQLRich.toSelectFunSQL(entity, fieldForFun, functionType);
			return sqlLib.selectFun(sql);
		} catch (ObjSQLException e) {
			System.err.println(e.getMessage());
		}

		return s;
	}

	@Override
	public <T> int update(T entity, String updateFieldList, IncludeType includeType) {
		if (entity == null) return 0;
		int r = 0;
		try {
			String sql = objToSQLRich.toUpdateSQL(entity, updateFieldList, includeType);
			Logger.logSQL("update SQL(updateFieldList) :", sql);
			r = sqlLib.modify(sql);
		} catch (ObjSQLException e) {
			System.err.println(e.getMessage());
		}

		return r;
	}

	@Override
	public <T> List<T> select(T entity, IncludeType includeType) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectSQL(entity, includeType);
		Logger.logSQL("select SQL: ", sql);
		return sqlLib.select(sql, entity);
	}

	@Override
	public <T> int update(T entity, IncludeType includeType) {
		if (entity == null) return 0;
		String sql = objToSQLRich.toUpdateSQL(entity, includeType);
		Logger.logSQL("update SQL: ", sql);
		return sqlLib.modify(sql);
	}

	@Override
	public <T> int insert(T entity, IncludeType includeType) {
		if (entity == null) return 0;
		String sql = objToSQLRich.toInsertSQL(entity, includeType);
		Logger.logSQL("insert SQL: ", sql);
		return sqlLib.modify(sql);
	}

	@Override
	public <T> int delete(T entity, IncludeType includeType) {
		if (entity == null) return 0;
		String sql = objToSQLRich.toDeleteSQL(entity, includeType);
		Logger.logSQL("delete SQL: ", sql);
		return sqlLib.modify(sql);
	}

	@Override
	public <T> List<String[]> selectString(T entity) {

		if (entity == null) return null;

		List<String[]> list = null;
		String sql = objToSQLRich.toSelectSQL(entity);

		Logger.logSQL("List<String[]> select SQL: ", sql);
		list = sqlLib.select(sql);

		return list;
	}

	@Override
	public <T> List<String[]> selectString(T entity, String selectFields) {

		if (entity == null) return null;

		List<String[]> list = null;
		try {
			String sql = objToSQLRich.toSelectSQL(entity, selectFields);
			list = sqlLib.select(sql);
		} catch (ObjSQLException e) {
			System.err.println(e.getMessage());
		}

		return list;
	}

	@Override
	public <T> String selectJson(T entity) {
		
		if (entity == null) return null;
		
		String sql = objToSQLRich.toSelectSQL(entity);
		Logger.logSQL("selectJson SQL: ", sql);
		
		return sqlLib.selectJson(sql);
	}

	@Override
	public <T> String selectJson(T entity, IncludeType includeType) {
		if (entity == null) return null;
		
		String sql = objToSQLRich.toSelectSQL(entity, includeType);
		Logger.logSQL("selectJson SQL: ", sql);
		
		return sqlLib.selectJson(sql);
	}

}
