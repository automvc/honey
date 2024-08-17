/*
 * Copyright 2020-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */
package org.teasoft.honey.osql.core;

import java.util.List;
import java.util.Map;

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.bee.mongodb.MongodbRawSql;
import org.teasoft.bee.osql.SuidType;

/**
 * @author Kingstar
 * @since  2.1
 */
public class MongodbRawSqlLib extends AbstractCommOperate implements MongodbRawSql {

	private MongodbBeeSql mongodbBeeSql;

	@Override
	public <T> List<T> select(String commandStr, Class<T> returnTypeClass) {
		List<T> list = null;
		try {
			doBeforePasreEntity(returnTypeClass, SuidType.SELECT);// returnType的值,虽然不用作占位参数的值,但可以用作拦截器的业务逻辑判断
			commandStr = doAfterCompleteSql(commandStr);
			Logger.logSQL("MongodbRawSql select SQL: \n", commandStr);
			list = getMongodbBeeSql().select(commandStr, returnTypeClass);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public String selectJson(String sql) {
		String r = "";
		try {
			doBeforePasreEntity();
			sql = doAfterCompleteSql(sql);
			Logger.logSQL("MongodbRawSql selectJson SQL: \n", sql);
			r = getMongodbBeeSql().selectJson(sql);
		} finally {
			doBeforeReturn();
		}
		return r;
	}

	@Override
	public int modify(String sql) {
		int r = 0;
		try {
			doBeforePasreEntity2();
			sql = doAfterCompleteSql(sql);
			Logger.logSQL("MongodbRawSql modify SQL: \n", sql);
			r = getMongodbBeeSql().modify(sql);
		} finally {
			doBeforeReturn();
		}
		return r;
	}

	@Override
	public List<Map<String, Object>> selectMapList(String sql) {
		List<Map<String, Object>> list = null;
		try {
			doBeforePasreEntity();
			sql = doAfterCompleteSql(sql);
			Logger.logSQL("MongodbRawSql selectMapList SQL: \n", sql);
			list = getMongodbBeeSql().selectMapList(sql);
		} finally {
			doBeforeReturn();
		}
		return list;
	}

	public MongodbBeeSql getMongodbBeeSql() {
		if (mongodbBeeSql == null) return BeeFactory.getHoneyFactory().getMongodbBeeSql();
		return mongodbBeeSql;
	}

	public void setMongodbBeeSql(MongodbBeeSql mongodbBeeSql) {
		this.mongodbBeeSql = mongodbBeeSql;
	}

	private void doBeforePasreEntity() {
		Object entity = null;
		super.doBeforePasreEntity(entity, SuidType.SELECT);
	}

	private void doBeforePasreEntity2() {
		Object entity = null;
		super.doBeforePasreEntity(entity, SuidType.MODIFY);
	}

}
