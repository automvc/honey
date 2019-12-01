/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.name.NameUtil;

/**
 * @author Kingstar
 * @since  1.7
 */
public class _MoreObjectToSQLHelper {
	
	private static DbFeature dbFeature = BeeFactory.getHoneyFactory().getDbFeature();
	private static String COMMA=",";
	private static String ONE_SPACE = " ";
	private static String DOT=".";
	
	static <T> String _toSelectSQL(T entity) {
        return _toSelectSQL(entity, -1, null,-1,-1);
	}
	
	static <T> String _toSelectSQL(T entity, int start, int size) {
        return _toSelectSQL(entity, -1, null,start,size);
	}
	
	static <T> String _toSelectSQL(T entity,Condition condition) {
		int includeType;
		if(condition==null || condition.getIncludeType()==null)
			includeType=-1;
		else includeType=condition.getIncludeType().getValue();
		
		return _toSelectSQL(entity, includeType, condition,-1,-1);
	}
	
	private static <T> String _toSelectSQL(T entity, int includeType,Condition condition, int start, int size) {
			
		checkPackage(entity);
		
		Set<String> conditionFieldSet=null;
		if(condition!=null) conditionFieldSet=condition.getFieldSet();
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlBuffer2 = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		try {
			
			Field fields[] = entity.getClass().getDeclaredFields(); 
			
			MoreTableStruct moreTableStruct[]=HoneyUtil.getMoreTableStructAndCheckBefore(entity);
			
//			2tablesWithJoinOnStyle
			
			boolean moreTable_columnListWithStar=HoneyConfig.getHoneyConfig().isMoreTable_columnListWithStar();
			String columnNames;
			if(moreTable_columnListWithStar){
				columnNames="*";
			}else{
			    columnNames=moreTableStruct[0].columnsFull;
			}
//			String tableName = _toTableName(entity);
			String tableName = moreTableStruct[0].tableName;
					
			sqlBuffer.append("select " + columnNames + " from ");
			sqlBuffer.append(tableName);
			boolean firstWhere = true;

			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			
			//从表 最多两个
			for (int s = 1; s <= 2; s++) { // 从表在数组下标是1和2. 0是主表
				if (moreTableStruct[s] != null) {
					sqlBuffer.append(COMMA);
					sqlBuffer.append(moreTableStruct[s].tableName);
					if(moreTableStruct[s].hasSubAlias){//从表定义有别名
						sqlBuffer.append(ONE_SPACE);
						sqlBuffer.append(moreTableStruct[s].subAlias);
					}
					if (firstWhere) {
						sqlBuffer2.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer2.append(" and ");
					}
					sqlBuffer2.append(moreTableStruct[s].joinExpression);
				}
			}
			
			int len = fields.length;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);
//				if (fields[i].isAnnotationPresent(JoinTable.class)) {
//					continue;  //JoinTable已在上面另外处理
//				}
//				if (HoneyUtil.isContinueForMoreTable(includeType, fields[i].get(entity),fields[i].getName())) {
				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {  //包含了fields[i].isAnnotationPresent(JoinTable.class)的判断
					continue;
				} else {
					
					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
						continue; //id=null不作为过滤条件
					
					if(conditionFieldSet!=null && conditionFieldSet.contains(fields[i].getName())) 
						continue; //Condition已包含的,不再遍历

					if (firstWhere) {
						sqlBuffer2.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer2.append(" and ");
					}
					sqlBuffer2.append(tableName);
					sqlBuffer2.append(DOT);
					sqlBuffer2.append(_toColumnName(fields[i].getName()));
					
					if (fields[i].get(entity) == null) {
						sqlBuffer2.append(" is null");
					} else {
						sqlBuffer2.append("=");
						sqlBuffer2.append("?");

						valueBuffer.append(",");
						valueBuffer.append(fields[i].get(entity));

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						list.add(k++, preparedValue);
					}
				}
			}//end for
			
			sqlBuffer.append(sqlBuffer2);
			
			//处理子表相应字段到where条件
			for (int index = 1; index <= 2; index++) { // 从表在数组下标是1和2. 0是主表
				if (moreTableStruct[index] != null) {
					parseSubObject(sqlBuffer, valueBuffer, list, conditionFieldSet, firstWhere, includeType, moreTableStruct, index);
				}
			}
			
			if(condition!=null){
				 condition.setSuidType(SuidType.SELECT);
			     ConditionHelper.processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere);
			}
			
			String sql;
			if(start!=-1 && size!=-1){
				sql=dbFeature.toPageSql(sqlBuffer.toString(), start, size);
			}else{
				sql=sqlBuffer.toString();
			}
			
//			sqlBuffer.append(";");

			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
			HoneyContext.setSqlValue(sqlBuffer.toString(), valueBuffer.toString()); //用于log显示
			addInContextForCache(sql, valueBuffer.toString(), tableName);//TODO tableName还要加上多表的.
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sqlBuffer.toString();
	}
	
	private static void parseSubObject(StringBuffer sqlBuffer2, StringBuffer valueBuffer, 
			List<PreparedValue> list,  Set<String> conditionFieldSet, boolean firstWhere,
			 int includeType,MoreTableStruct moreTableStruct[],int index) {
		
		Object entity=moreTableStruct[index].subObject;
		
		if(entity==null) return ;
		
		PreparedValue preparedValue = null;
		
//		String tableName = moreTableStruct[index].tableName;
		String useSubTableName = moreTableStruct[index].useSubTableName;
		
//		moreTableStruct[i].subEntityField;
//		Field fields[] = subEntityField.getType().getDeclaredFields();
		Field fields[] = moreTableStruct[index].subEntityField.getType().getDeclaredFields();
		int len = fields.length;
		try{
//		for (int i = 0, k = 0; i < len; i++) { //bug
		for (int i = 0; i < len; i++) {
			fields[i].setAccessible(true);
//			if (fields[i].isAnnotationPresent(JoinTable.class)) {
//				continue;  //JoinTable已在上面另外处理
//			}
//			if (HoneyUtil.isContinueForMoreTable(includeType, fields[i].get(entity),fields[i].getName())) {
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {  //包含了fields[i].isAnnotationPresent(JoinTable.class)的判断
				continue;
			} else {
				
				if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
					continue; //id=null不作为过滤条件
				
				if(conditionFieldSet!=null && conditionFieldSet.contains(fields[i].getName())) 
					continue; //Condition已包含的,不再遍历

				if (firstWhere) {
					sqlBuffer2.append(" where ");
					firstWhere = false;
				} else {
					sqlBuffer2.append(" and ");
				}
				sqlBuffer2.append(useSubTableName);
				sqlBuffer2.append(DOT);
				sqlBuffer2.append(_toColumnName(fields[i].getName()));
				
				if (fields[i].get(entity) == null) {
					sqlBuffer2.append(" is null");
				} else {
					sqlBuffer2.append("=");
					sqlBuffer2.append("?");

					valueBuffer.append(",");
					valueBuffer.append(fields[i].get(entity));

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
//					list.add(k++, preparedValue);  //bug
					list.add(preparedValue);
				}
			}
		}//end for
	} catch (IllegalAccessException e) {
		throw ExceptionHelper.convert(e);
	}
	}
	
    static void addInContextForCache(String sql,String sqlValue, String tableName){
		CacheSuidStruct struct=new CacheSuidStruct();
		struct.setSql(sql);
		struct.setSqlValue(sqlValue);
		struct.setTableNames(tableName);
		
		HoneyContext.setCacheInfo(sql, struct);
	}
    
	private static <T> void checkPackage(T entity) {
		HoneyUtil.checkPackage(entity);
	}
	
	private static String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	private static String _toColumnName(String fieldName){
		return NameTranslateHandle.toColumnName(fieldName);
	}

//	private static String _toTableNameByEntityName(String entityName){
//		return NameTranslateHandle.toTableName(entityName);
//	}

}
