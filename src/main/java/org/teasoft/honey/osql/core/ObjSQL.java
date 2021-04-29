/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.ObjToSQL;
import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.exception.NotSupportedException;

/**
 * 通过对象来操作数据库，并返回结果
 * @author Kingstar
 * Create on 2013-6-30 下午10:19:27
 * @since  1.0
 */
public class ObjSQL implements Suid {
	

	private BeeSql beeSql;
	private ObjToSQL objToSQL;

	public ObjSQL() {}

	public BeeSql getBeeSql() {
		if(beeSql==null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	public ObjToSQL getObjToSQL() {
		if(objToSQL==null) objToSQL=BeeFactory.getHoneyFactory().getObjToSQL();
		return objToSQL;
	}

	public void setObjToSQL(ObjToSQL objToSQL) {
		this.objToSQL = objToSQL;
	}

	@Override
	public <T> List<T> select(T entity) {

		if (entity == null) return null;

		List<T> list = null;
		String sql = getObjToSQL().toSelectSQL(entity);
		Logger.logSQL("select SQL: ", sql);
		list = getBeeSql().select(sql, entity); // 返回值用到泛型
		return list;
	}
	
	@Override
	public <T> int update(T entity) {
		// 当id为null时抛出异常  在转sql时抛出

		if (entity == null) return -1;

		String sql = "";
		int updateNum = -1;
		sql = getObjToSQL().toUpdateSQL(entity);
		Logger.logSQL("update SQL: ", sql);
		_regEntityClass(entity);
		updateNum = getBeeSql().modify(sql);

		return updateNum;
	}

	@Override
	public <T> int insert(T entity){

		if (entity == null) return -1;

		String sql = getObjToSQL().toInsertSQL(entity);
		int insertNum = -1;
		Logger.logSQL("insert SQL: ", sql);
		_regEntityClass(entity);
		HoneyUtil.revertId(entity); //v1.9
		insertNum = getBeeSql().modify(sql);
		return insertNum;
	}
	
	@Override
	public <T> long insertAndReturnId(T entity) {
		if (entity == null) return -1L;

		if (!HoneyContext.isNeedGenId(entity.getClass())
				&& !(HoneyUtil.isMysql() || HoneyUtil.isOracle() || HoneyUtil.isSQLite())) {
			throw new NotSupportedException("The current database don't support return the id after insert."
					+ " You can use the distribute id via set config information,eg: bee.distribution.genid.forAllTableLongId=true");
		}

		String sql = getObjToSQL().toInsertSQL(entity);
		Logger.logSQL("insert SQL: ", sql);
		_regEntityClass(entity);

		Object obj = HoneyUtil.getIdValue(entity);
		HoneyUtil.revertId(entity);
		
		long returnId = -1;
		if (obj != null) {
//			returnId = (long) obj;
			returnId = Long.parseLong(obj.toString());
			if (returnId > 1) {
				int insertNum = getBeeSql().modify(sql);
				if (insertNum == 1) {
					return returnId;
				} else {
					return insertNum;
				}
			} else {
				if (HoneyUtil.isOracle()) {
					Logger.debug("Need create Sequence and Trigger for auto increment id. "
							+ "By the way,maybe use distribute id is better!");
				}
			}
		}

		//id will gen by db
		returnId = getBeeSql().insertAndReturnId(sql);

		return returnId;
	}
	

	@Override
	public int delete(Object entity) {

		if (entity == null) return -1;

		String sql = getObjToSQL().toDeleteSQL(entity);
		int deleteNum = -1;
		Logger.logSQL("delete SQL: ", sql);
		_regEntityClass(entity);
		deleteNum = getBeeSql().modify(sql);
		return deleteNum;
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;

		List<T> list = null;
		String sql = getObjToSQL().toSelectSQL(entity,condition);
		Logger.logSQL("select SQL: ", sql);
		list = getBeeSql().select(sql, entity); 
		return list;
	}

	@Override
	public <T> int delete(T entity, Condition condition) {
		if (entity == null) return -1;

		String sql = getObjToSQL().toDeleteSQL(entity,condition);
		int deleteNum = -1;
		if (!"".equals(sql)) {
			Logger.logSQL("delete SQL: ", sql);
		}
		_regEntityClass(entity);
		deleteNum = getBeeSql().modify(sql);
		return deleteNum;
	}

	@Override
	public Suid setDynamicParameter(String para, String value) {
		OneTimeParameter.setAttribute(para, value);
		return this;
	}
	
	private <T> void _regEntityClass(T entity){
		HoneyContext.regEntityClass(entity.getClass());
	}

	@Override
	public void beginSameConnection() {
		OneTimeParameter.setTrueForKey("_SYS_Bee_SAME_CONN_BEGIN"); 
		if(OneTimeParameter.isTrue("_SYS_Bee_SAME_CONN_EXCEPTION")) {//获取后,该key不会再存在
			Logger.warn("Last SameConnection do not have endSameConnection() or do not run endSameConnection() after having exception.");
		}
	}

	@Override
	public void endSameConnection() {
		HoneyContext.endSameConnection(); 
	}
	
}
