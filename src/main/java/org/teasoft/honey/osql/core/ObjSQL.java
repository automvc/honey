/*
 * Copyright 2013-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.ObjToSQL;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.Suid;
import org.teasoft.bee.osql.exception.NotSupportedException;
import org.teasoft.honey.logging.Logger;

/**
 * 通过对象来操作数据库，并返回结果.
 * 数据库操作接口Suid实现类,包括查,改,增,删 Suid (select,update,insert,delete),
 * 默认不处理null和空字符串.
 * Operate the database through objects and return results.
 * Suid implementation class for database operation interface, including Suid(select, update, insert, delete),
 * Default does not handle null and empty strings.
 * @author Kingstar
 * Create on 2013-6-30 下午10:19:27
 * @since  1.0
 */
public class ObjSQL extends AbstractCommOperate implements Suid {

	private BeeSql beeSql;
	private ObjToSQL objToSQL;

	@Override
	public <T> List<T> select(T entity) {

		if (entity == null) return null;

		List<T> list = null;
		try {
			doBeforePasreEntity(entity, SuidType.SELECT);

			String sql = getObjToSQL().toSelectSQL(entity);

			sql = doAfterCompleteSql(sql);

			Logger.logSQL(LogSqlParse.parseSql("select SQL: ", sql));
			list = getBeeSql().select(sql, toClassT(entity)); // 返回值用到泛型
		} finally {
			doBeforeReturn(list);
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	protected <T> Class<T> toClassT(T entity) {
		return (Class<T>) entity.getClass();
	}

	@Override
	public <T> int update(T entity) {
		// 当id为null时抛出异常 在转sql时抛出

		if (entity == null) return -1;
		try {
			doBeforePasreEntity(entity, SuidType.UPDATE);

			String sql = "";
			int updateNum = -1;
			sql = getObjToSQL().toUpdateSQL(entity);
			_regEntityClass(entity);
			sql = doAfterCompleteSql(sql);

			Logger.logSQL(LogSqlParse.parseSql("update SQL: ", sql));
			updateNum = getBeeSql().modify(sql);

			return updateNum;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public <T> int insert(T entity) {

		if (entity == null) return -1;
		try {
			_ObjectToSQLHelper.setInitIdByAuto(entity); // 2.4.0 setInitIdByAuto > doBeforePasreEntity
			doBeforePasreEntity(entity, SuidType.INSERT);
			String sql = getObjToSQL().toInsertSQL(entity);
			_regEntityClass(entity);
			sql = doAfterCompleteSql(sql);
			int insertNum = -1;
			Logger.logSQL(LogSqlParse.parseSql("insert SQL: ", sql));

			HoneyUtil.revertId(entity); // v1.9
			if (OneTimeParameter.isTrue("_SYS_Bee_NullObjectInsert")) {
				Logger.warn("All fields in object is null, would ignroe it!");
				insertNum = 0;
			} else {
				insertNum = getBeeSql().modify(sql);
			}
			return insertNum;
		} finally {
			doBeforeReturn();
		}
	}

	private <T> void checkGenPk(T entity) {

//		Object pk = HoneyUtil.getIdValue(entity); // 原始id
//		boolean hasGenPkAnno = HoneyUtil.hasGenPkAnno(entity); // V1.17可以使用注解.

		boolean isOk = ((HoneyUtil.isMysql() || HoneyUtil.isOracle() || HoneyUtil.isSQLite()) // 即使java端没生成,数据库也可以生成
				|| HoneyContext.isNeedGenId(entity.getClass()) || HoneyUtil.getIdValue(entity) != null
				|| HoneyUtil.hasGenPkAnno(entity));
		if (!isOk) {
			throw new NotSupportedException(
					"The current database don't support insert NULL to 'id' column or return the id after insert."
							+ "\nYou can use the distribute id via set config information,eg: bee.distribution.genid.forAllTableLongId=true");
		}
	}

	private <T> String _toInsertAndReturnSql(T entity) {

		// 2.4.0 setInitIdByAuto > doBeforePasreEntity
		_ObjectToSQLHelper.setInitIdByAuto(entity); // 更改了原来的对象 //这里会生成id,如果需要
		// 要先进行id自动设置,再到分片等拦截器,否则分片键的值没有会出错. 20.4.0
		doBeforePasreEntity(entity, SuidType.INSERT);
		String sql = getObjToSQL().toInsertSQL(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(LogSqlParse.parseSql("insert SQL: ", sql));

		return sql;
	}

	@Override
	public <T> long insertAndReturnId(T entity) {
		if (entity == null) return -1L;
		checkGenPk(entity);
		try {
			String sql = _toInsertAndReturnSql(entity);

			return _insertAndReturnId(entity, sql);
		} finally {
			doBeforeReturn();
		}
	}

	protected <T> long _insertAndReturnId(T entity, String sql) {

		_regEntityClass(entity);

		Object obj = HoneyUtil.getIdValue(entity); // 没有自动生成部分的?? 在上层调用方法. 到这里,上面setInitIdByAuto(entity),已有生成id的逻辑.
		HoneyUtil.revertId(entity); // 获取后就还原.

		long returnId = -1;
		if (obj != null) {
//			returnId = (long) obj;
			returnId = Long.parseLong(obj.toString());
			if (returnId > 1) {// entity实体id设置有大于1的值,使用实体的
				int insertNum = getBeeSql().modify(sql);
				if (insertNum == 1) {// 插入成功
					return returnId;
				} else {// 插入失败,返回modify(sql)的值
					return insertNum;
				}
			} else {
				if (HoneyUtil.isOracle()) {
					Logger.debug("Need create Sequence and Trigger for auto increment id. "
							+ "By the way,maybe use distribute id is better!");
				}
			}
		}

		String pkName = HoneyUtil.getPkFieldName(entity);
		if ("".equals(pkName) || pkName.contains(",")) pkName = "id"; // insertAndReturnId 返回id,只支持一个主键,没有或超过一个改为用id.
//		fixed 2.4.0   use column name
		OneTimeParameter.setAttribute(StringConst.PK_Column_For_ReturnId,
				HoneyUtil.toCloumnNameForPks(pkName, entity.getClass()));
		// id will gen by db
		returnId = getBeeSql().insertAndReturnId(sql);
//		doBeforeReturn();
		return returnId;
	}

	@Override
	public int delete(Object entity) {

		if (entity == null) return -1;
		try {
			doBeforePasreEntity(entity, SuidType.DELETE);
			String sql = getObjToSQL().toDeleteSQL(entity);
			_regEntityClass(entity);
			sql = doAfterCompleteSql(sql);
			int deleteNum = -1;
			Logger.logSQL(LogSqlParse.parseSql("delete SQL: ", sql));
			deleteNum = getBeeSql().modify(sql);
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
			if (condition != null) condition = condition.clone();
			regCondition(condition);
			doBeforePasreEntity(entity, SuidType.SELECT);
			// 传递要判断是否有group
			OneTimeParameter.setTrueForKey(StringConst.Check_Group_ForSharding);
			String sql = getObjToSQL().toSelectSQL(entity, condition);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(LogSqlParse.parseSql("select SQL: ", sql));
			list = getBeeSql().select(sql, toClassT(entity));
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> int delete(T entity, Condition condition) {
		if (entity == null) return -1;
		try {
			if (condition != null) condition = condition.clone();
			regCondition(condition);
			doBeforePasreEntity(entity, SuidType.DELETE);
			String sql = getObjToSQL().toDeleteSQL(entity, condition);
			_regEntityClass(entity);
			sql = doAfterCompleteSql(sql);
			int deleteNum = -1;
			if (!"".equals(sql)) {
				Logger.logSQL(LogSqlParse.parseSql("delete SQL: ", sql));
			}
			deleteNum = getBeeSql().modify(sql);
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

	private <T> void _regEntityClass(T entity) {
		HoneyContext.regEntityClass(entity.getClass());
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

	public BeeSql getBeeSql() {
		if (beeSql == null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	public ObjToSQL getObjToSQL() {
		if (objToSQL == null) objToSQL = BeeFactory.getHoneyFactory().getObjToSQL();
		return objToSQL;
	}

	public void setObjToSQL(ObjToSQL objToSQL) {
		this.objToSQL = objToSQL;
	}

}
