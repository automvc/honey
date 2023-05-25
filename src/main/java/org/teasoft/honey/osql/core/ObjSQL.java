/*
 * Copyright 2013-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.ObjToSQL;
import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.exception.NotSupportedException;

/**
 * 通过对象来操作数据库，并返回结果.
 * 数据库操作接口Suid实现类,包括查,改,增,删 Suid (select,update,insert,delete),
 * 默认不处理null和空字符串
 * @author Kingstar
 * Create on 2013-6-30 下午10:19:27
 * @since  1.0
 * @since  1.17.21 add AbstractCommOperate
 */
public class ObjSQL extends AbstractCommOperate implements Suid {
	
	private BeeSql beeSql;
	private ObjToSQL objToSQL;

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
	
	@Override
	public <T> List<T> select(T entity) {

		if (entity == null) return null;
		
		doBeforePasreEntity(entity,SuidType.SELECT);

		List<T> list = null;
		String sql = getObjToSQL().toSelectSQL(entity);
		
		sql=doAfterCompleteSql(sql);
		
		Logger.logSQL("select SQL: ", sql);
		list = getBeeSql().select(sql, entity); // 返回值用到泛型
		doBeforeReturn(list);
		
		return list;
	}
	
	@Override
	public <T> int update(T entity) {
		// 当id为null时抛出异常  在转sql时抛出

		if (entity == null) return -1;
		
		doBeforePasreEntity(entity,SuidType.UPDATE);
		
		String sql = "";
		int updateNum = -1;
		sql = getObjToSQL().toUpdateSQL(entity);
		_regEntityClass(entity);
		sql=doAfterCompleteSql(sql);
		
		Logger.logSQL("update SQL: ", sql);
		updateNum = getBeeSql().modify(sql);
		
		doBeforeReturn();
		
		return updateNum;
	}

	@Override
	public <T> int insert(T entity){

		if (entity == null) return -1;
		doBeforePasreEntity(entity,SuidType.INSERT);
		_ObjectToSQLHelper.setInitIdByAuto(entity); // 更改了原来的对象
		String sql = getObjToSQL().toInsertSQL(entity);
		_regEntityClass(entity);
		sql=doAfterCompleteSql(sql);
		int insertNum = -1;
		Logger.logSQL("insert SQL: ", sql);
		HoneyUtil.revertId(entity); //v1.9
		insertNum = getBeeSql().modify(sql);
		
		doBeforeReturn();
		
		return insertNum;
	}
	
	private <T> void checkGenPk(T entity) {

//		Object pk = HoneyUtil.getIdValue(entity); // 原始id
//		boolean hasGenPkAnno = HoneyUtil.hasGenPkAnno(entity); // V1.17可以使用注解.

		boolean isOk = (
				(HoneyUtil.isMysql() || HoneyUtil.isOracle() || HoneyUtil.isSQLite())  //即可java端没生成,数据库也可以生成
				|| HoneyContext.isNeedGenId(entity.getClass())
				|| HoneyUtil.getIdValue(entity) != null || HoneyUtil.hasGenPkAnno(entity)
				);
		if (!isOk) {
			throw new NotSupportedException(
					"The current database don't support insert NULL to 'id' column or return the id after insert."
							+ "\nYou can use the distribute id via set config information,eg: bee.distribution.genid.forAllTableLongId=true");
		}
	}
	
	private <T> String insertAndReturn(T entity) {
		doBeforePasreEntity(entity,SuidType.INSERT);
		_ObjectToSQLHelper.setInitIdByAuto(entity); // 更改了原来的对象  //这里会生成id,如果需要
		String sql = getObjToSQL().toInsertSQL(entity); 
		sql=doAfterCompleteSql(sql);
		Logger.logSQL("insert SQL: ", sql);
		
		return sql;
	}
	
	@Override
	public <T> long insertAndReturnId(T entity) {
		if (entity == null) return -1L;
		checkGenPk(entity);
		
		String sql=insertAndReturn(entity);

		return _insertAndReturnId(entity, sql);
	}
	
	 <T> long _insertAndReturnId(T entity,String sql) {
		
		_regEntityClass(entity);

		Object obj = HoneyUtil.getIdValue(entity); //没有自动生成部分的?? 在上层调用方法.  到这里,上面setInitIdByAuto(entity),已有生成id的逻辑.
		HoneyUtil.revertId(entity); //获取后就还原.
		
		long returnId = -1;
		if (obj != null) {
//			returnId = (long) obj;
			returnId = Long.parseLong(obj.toString());
			if (returnId > 1) {//entity实体id设置有大于1的值,使用实体的
				int insertNum = getBeeSql().modify(sql);
				if (insertNum == 1) {//插入成功
					return returnId;
				} else {//插入失败,返回modify(sql)的值
					return insertNum;
				}
			} else {
				if (HoneyUtil.isOracle()) {
					Logger.debug("Need create Sequence and Trigger for auto increment id. "
							+ "By the way,maybe use distribute id is better!");
				}
			}
		}

		String pkName=HoneyUtil.getPkFieldName(entity);
		if("".equals(pkName) || pkName.contains(",")) pkName="id";
		OneTimeParameter.setAttribute(StringConst.PK_Name_For_ReturnId, pkName);
		//id will gen by db
		returnId = getBeeSql().insertAndReturnId(sql);
		doBeforeReturn();
		return returnId;
	}
	

	@Override
	public int delete(Object entity) {

		if (entity == null) return -1;
		doBeforePasreEntity(entity,SuidType.DELETE);
		String sql = getObjToSQL().toDeleteSQL(entity);
		_regEntityClass(entity);
		sql=doAfterCompleteSql(sql);
		int deleteNum = -1;
		Logger.logSQL("delete SQL: ", sql);
		deleteNum = getBeeSql().modify(sql);
		doBeforeReturn();
		return deleteNum;
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;
		doBeforePasreEntity(entity,SuidType.SELECT);
		List<T> list = null;
		String sql = getObjToSQL().toSelectSQL(entity,condition);
		sql=doAfterCompleteSql(sql);
		Logger.logSQL("select SQL: ", sql);
		list = getBeeSql().select(sql, entity); 
		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> int delete(T entity, Condition condition) {
		if (entity == null) return -1;
		doBeforePasreEntity(entity,SuidType.DELETE);
		String sql = getObjToSQL().toDeleteSQL(entity,condition);
		_regEntityClass(entity);
		sql=doAfterCompleteSql(sql);
		int deleteNum = -1;
		if (!"".equals(sql)) {
			Logger.logSQL("delete SQL: ", sql);
		}
		deleteNum = getBeeSql().modify(sql);
		doBeforeReturn();
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
