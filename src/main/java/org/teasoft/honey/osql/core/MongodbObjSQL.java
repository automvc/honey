/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Jade
 * @since  2.0
 */
public class MongodbObjSQL extends AbstractCommOperate implements Suid {
	
	private MongodbBeeSql mongodbBeeSql;
	
	@Override
	public <T> List<T> select(T entity) {
		
		checkPackage(entity);
		
		
		if (entity == null) return null;
		
		doBeforePasreEntity(entity,SuidType.SELECT);

		List<T> list = null;
		
//		String sql = getObjToSQL().toSelectSQL(entity);
//		
//		sql=doAfterCompleteSql(sql);
//		
//		Logger.logSQL("select SQL: ", sql);
//		list = getBeeSql().select(sql, entity); // 返回值用到泛型
		
		//Sharding,还要更改表名; 库名
		
		list=getMongodbBeeSql().select(entity);
		
		doBeforeReturn(list);
		
		return list;
		
		
		
		
		
		//1.
		// entity -> map<key,value>
		//1.5 是String要过滤 " , ' -- # 注释符     参考日志的.
		
		

		//2. call mongodb api, return Document
//		至少要传表名,  T或者map
//		FindIterable<Document> findIterable = cols.find();  
//        MongoCursor<Document> mongoCursor = findIterable.iterator();  
//        while(mongoCursor.hasNext()){  
//           System.out.println(mongoCursor.next());  
//        }  
		
		
		// Document -> entity List
		
	}

//	@Override
//	public <T> int update(T entity) {
//		
//		int a=getMongodbBeeSql().update(entity);
//		
//		
//		return a;
//	}
//
//	@Override
//	public <T> int insert(T entity) {
//		int a=getMongodbBeeSql().insert(entity);
//		return a;
//	}
	
	@Override
	public <T> int update(T entity) {
		// 当id为null时抛出异常  在转sql时抛出

		if (entity == null) return 0;
		
		doBeforePasreEntity(entity,SuidType.UPDATE);
		
		
//		String sql = "";
//		int updateNum = -1;
//		sql = getObjToSQL().toUpdateSQL(entity);
//		_regEntityClass(entity);
//		sql=doAfterCompleteSql(sql);
//		
//		Logger.logSQL("update SQL: ", sql);
//		updateNum = getBeeSql().modify(sql);
		
		int updateNum =getMongodbBeeSql().update(entity);
		
		doBeforeReturn();
		
		return updateNum;
	}

	@Override
	public <T> int insert(T entity){

		if (entity == null) return -1;
		doBeforePasreEntity(entity,SuidType.INSERT);
		_ObjectToSQLHelper.setInitIdByAuto(entity); // 更改了原来的对象
		
		int insertNum =0;
		insertNum=getMongodbBeeSql().insert(entity);
		HoneyUtil.revertId(entity); //v1.9
		
		doBeforeReturn();
		
		return insertNum;
	}
	
	

	@Override
	public <T> long insertAndReturnId(T entity) {
		if (entity == null) return -1;
		doBeforePasreEntity(entity,SuidType.INSERT);
		_ObjectToSQLHelper.setInitIdByAuto(entity); // 更改了原来的对象
		
		long insertNum =0;
		
		insertNum=getMongodbBeeSql().insertAndReturnId(entity, null);
		
		HoneyUtil.revertId(entity); 
		
		doBeforeReturn();
		
		return insertNum;
	}

	@Override
	public int delete(Object entity) {

		if (entity == null) return -1;
		
		doBeforePasreEntity(entity,SuidType.DELETE);
		
		int deleteNum =0;
		
		deleteNum=getMongodbBeeSql().delete(entity);
		
		doBeforeReturn();
		
		return deleteNum;
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;
		regCondition(condition);
		doBeforePasreEntity(entity, SuidType.SELECT);
		if (condition != null) condition.setSuidType(SuidType.SELECT);

		List<T> list = null;

		if (condition != null) {
			ConditionImpl conditionImpl = (ConditionImpl) condition;
			String[] selectFields = conditionImpl.getSelectField();
			if (selectFields != null && selectFields.length == 1
					&& StringUtils.isNotBlank(selectFields[0])) {
				selectFields = selectFields[0].split(",");
			} else {
				if (condition.getSelectField() == null)
					condition.selectField(HoneyUtil.getColumnNames(entity));
			}
		}

		list = getMongodbBeeSql().select(entity, condition);

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> int delete(T entity, Condition condition) {
		if (entity == null) return -1;
		regCondition(condition);
		doBeforePasreEntity(entity,SuidType.DELETE);
		int deleteNum =0;
		
		deleteNum=getMongodbBeeSql().delete(entity);
		
		doBeforeReturn();
		return deleteNum;
	}

	@Override
	public Suid setDynamicParameter(String para, String value) {
		OneTimeParameter.setAttribute(para, value);
		return this;
	}
	
//	private <T> void _regEntityClass(T entity){
//		HoneyContext.regEntityClass(entity.getClass());
//	}

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
	
	private static <T> void checkPackage(T entity) {
		HoneyUtil.checkPackage(entity);
	}
	
	
	public MongodbBeeSql getMongodbBeeSql() {
		if(mongodbBeeSql==null) return BeeFactory.getHoneyFactory().getMongodbBeeSql();
		return mongodbBeeSql;
	}

	public void setMongodbBeeSql(MongodbBeeSql mongodbBeeSql) {
		this.mongodbBeeSql = mongodbBeeSql;
	}
	
}
