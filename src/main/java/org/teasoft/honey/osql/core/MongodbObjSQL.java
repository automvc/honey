/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.Suid;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Jade
 * @since  2.0
 */
public class MongodbObjSQL extends AbstractCommOperate implements Suid {

	private MongodbBeeSql mongodbBeeSql;

	@Override
	public <T> List<T> select(T entity) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			checkPackage(entity);
			doBeforePasreEntity(entity, SuidType.SELECT);

			list = getMongodbBeeSql().select(entity);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> int update(T entity) {
		// 当id为null时抛出异常 在转sql时抛出

		if (entity == null) return 0;
		try {
			doBeforePasreEntity(entity, SuidType.UPDATE);

			int updateNum = getMongodbBeeSql().update(entity);

			return updateNum;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public <T> int insert(T entity) {

		if (entity == null) return -1;
		try {
			doBeforePasreEntity(entity, SuidType.INSERT);
			_ObjectToSQLHelper.setInitIdByAuto(entity); // 更改了原来的对象

			int insertNum = 0;
			insertNum = getMongodbBeeSql().insert(entity);
			HoneyUtil.revertId(entity); // v1.9

			return insertNum;
		} finally {
			doBeforeReturn();
		}

	}

	@Override
	public <T> long insertAndReturnId(T entity) {
		if (entity == null) return -1;
		try {
			doBeforePasreEntity(entity, SuidType.INSERT);
			_ObjectToSQLHelper.setInitIdByAuto(entity); // 更改了原来的对象

			long insertNum = 0;

			insertNum = getMongodbBeeSql().insertAndReturnId(entity, null);

			HoneyUtil.revertId(entity);

			return insertNum;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public int delete(Object entity) {

		if (entity == null) return -1;
		try {
			doBeforePasreEntity(entity, SuidType.DELETE);

			int deleteNum = 0;

			deleteNum = getMongodbBeeSql().delete(entity);

			return deleteNum;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			regCondition(condition);
			doBeforePasreEntity(entity, SuidType.SELECT);
			if (condition != null) condition.setSuidType(SuidType.SELECT);

			if (condition != null) {
				ConditionImpl conditionImpl = (ConditionImpl) condition;
				String[] selectFields = conditionImpl.getSelectField();
				if (selectFields != null && selectFields.length == 1 && StringUtils.isNotBlank(selectFields[0])) {
					selectFields = selectFields[0].split(",");
				} else {
					if (condition.getSelectField() == null) {
						condition.selectField(HoneyUtil.getColumnNames(entity));
						// V2.1.8
						HoneyContext.setTrueInSysCommStrLocal(StringConst.MongoDB_SelectAllFields);
					}
				}
			}

			list = getMongodbBeeSql().select(entity, condition);
		} finally {
			HoneyContext.removeSysCommStrLocal(StringConst.MongoDB_SelectAllFields);

			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> int delete(T entity, Condition condition) {
		if (entity == null) return -1;
		try {
			regCondition(condition);
			doBeforePasreEntity(entity, SuidType.DELETE);
			int deleteNum = 0;

			deleteNum = getMongodbBeeSql().delete(entity);

			return deleteNum;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public Suid setDynamicParameter(String para, String value) {
		OneTimeParameter.setAttribute(para, value);
		return this;
	}

	@Override
	public void beginSameConnection() {
		OneTimeParameter.setTrueForKey(StringConst.SAME_CONN_BEGIN);
		if (OneTimeParameter.isTrue(StringConst.SAME_CONN_EXCEPTION)) {// 获取后,该key不会再存在
			Logger.warn(
					"Last SameConnection do not have endSameConnection() or do not run endSameConnection() after having exception.");
		}
	}

	@Override
	public void endSameConnection() {
		HoneyContext.endSameConnection();
	}

	private <T> void checkPackage(T entity) {
		HoneyUtil.checkPackage(entity);
	}

	public MongodbBeeSql getMongodbBeeSql() {
		if (mongodbBeeSql == null) return BeeFactory.getHoneyFactory().getMongodbBeeSql();
		return mongodbBeeSql;
	}

	public void setMongodbBeeSql(MongodbBeeSql mongodbBeeSql) {
		this.mongodbBeeSql = mongodbBeeSql;
	}

}
