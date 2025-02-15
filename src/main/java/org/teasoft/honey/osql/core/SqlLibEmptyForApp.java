/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;
import java.util.Map;

import org.teasoft.bee.app.BeeSqlForApp;

/**
 * 用于Android或Harmony
 * @author Kingstar
 * @since  1.17
 */
class SqlLibEmptyForApp implements BeeSqlForApp {

	private static final String MSG = "You are using SqlLibEmptyForApp. It is empty!";

	@Override
	public <T> List<T> select(String sql, Class<T> entityClass, String[] selectionArgs) {
		Logger.warn(MSG);
		return null;
	}

	@Override
	public String selectFun(String sql, String[] selectionArgs) {
		Logger.warn(MSG);
		return null;
	}

	@Override
	public int modify(String sql, Object[] bindArgs) {
		Logger.warn(MSG);
		return 0;
	}

	@Override
	public List<String[]> select(String sql, String[] selectionArgs) {
		Logger.warn(MSG);
		return null;
	}

	@Override
	public List<Map<String, Object>> selectMapList(String sql, String[] selectionArgs) {
		Logger.warn(MSG);
		return null;
	}

	@Override
	public List<Map<String, String>> selectMapListWithColumnName(String sql, String[] selectionArgs) {
		Logger.warn(MSG);
		return null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String selectJson(String sql, String[] selectionArgs, Class entityClass) {
		Logger.warn(MSG);
		return null;
	}

	@Override
	public long insertAndReturnId(String sql, Object[] arg1) {
		Logger.warn(MSG);
		return 0;
	}

	@Override
	public int batchInsert(String sql0, List<Object[]> listBindArgs) {
		Logger.warn(MSG);
		return 0;
	}

}
