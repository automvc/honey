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
import org.teasoft.bee.osql.annotation.JoinType;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;

/**
 * @author Kingstar
 * @since  1.7
 */
public class _MoreObjectToSQLHelper {
	
	private static final DbFeature dbFeature = BeeFactory.getHoneyFactory().getDbFeature();
	private static final String COMMA=",";
	private static final String ONE_SPACE = " ";
	private static final String DOT=".";
	
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
		String sql="";
		Set<String> conditionFieldSet=null;
		if(condition!=null) conditionFieldSet=condition.getFieldSet();
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlBuffer2 = new StringBuffer();
//		StringBuffer valueBuffer = new StringBuffer();
		try {
			
			Field fields[] = entity.getClass().getDeclaredFields(); 
			
			MoreTableStruct moreTableStruct[]=HoneyUtil.getMoreTableStructAndCheckBefore(entity);
			
			if (moreTableStruct[1] == null) { //v1.9
				throw new BeeErrorGrammarException(
						"MoreObject select on " + entity.getClass().getName() + " must own at least one JoinTable annotation!");
			}
			
			boolean twoTablesWithJoinOnStyle=HoneyConfig.getHoneyConfig().isTablesWithJoinOnStyle();
			
			boolean moreTable_columnListWithStar=HoneyConfig.getHoneyConfig().isMoreTable_columnListWithStar();
			String columnNames;
			columnNames=moreTableStruct[0].columnsFull;
			
			if (condition != null) {
				condition.setSuidType(SuidType.SELECT);
				String selectField = ConditionHelper.processSelectField(columnNames, condition,moreTableStruct[0].subDulFieldMap);
				if (selectField != null){
					columnNames = selectField;  //若指定了字段,则测试也不用*代替
				}else{
					if(moreTable_columnListWithStar){
						columnNames="*";
					}
				}
			}else { //V1.9
				if(moreTable_columnListWithStar){
					columnNames="*";
				}
			}
			
//			String tableName = _toTableName(entity);
			String tableName = moreTableStruct[0].tableName;
			
			String tableNamesForCache=tableName;//V1.9
					
//			sqlBuffer.append("select " + columnNames + " from ");
			sqlBuffer.append(K.select).append(" ").append(columnNames).append(" ").append(K.from).append(" ");
			sqlBuffer.append(tableName);
			boolean firstWhere = true;

			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			
			String useSubTableNames[]=new String[2];
			
			//v1.7.1 当有两个子表时,即使配置了用join..on,也会解析成where m1=sub1f1 and m2=sub2f1
			if(moreTableStruct[0].joinTableNum>1 && twoTablesWithJoinOnStyle && moreTableStruct[1].joinType==JoinType.JOIN){
				Logger.warn("SQL grammar type will use 'where ... =' replace 'join .. on' !");
			}
			
			//只有一个子表关联,且选用join type
			//排除以下情况: where m1=sub1f1 and m2=sub2f1 (放到else处理)
			if( (moreTableStruct[1].joinType!=JoinType.JOIN || (twoTablesWithJoinOnStyle && moreTableStruct[0].joinTableNum==1) )
			 &&(moreTableStruct[1].joinExpression != null && !"".equals(moreTableStruct[1].joinExpression))){ //需要有表达式
			
				if(moreTableStruct[1].joinType==JoinType.FULL_JOIN){
					Logger.warn("Pleae confirm the Database supports 'full join' type!");
				}
//				sqlBuffer.append(ONE_SPACE);
//				sqlBuffer.append("join");
				if(HoneyUtil.isSqlKeyWordUpper())sqlBuffer.append(moreTableStruct[1].joinType.getType().toUpperCase());
				else                             sqlBuffer.append(moreTableStruct[1].joinType.getType());
//				sqlBuffer.append(ONE_SPACE);
				sqlBuffer.append(moreTableStruct[1].tableName);
				tableNamesForCache+="##"+moreTableStruct[1].tableName;//V1.9
				if(moreTableStruct[1].hasSubAlias){//从表定义有别名
					sqlBuffer.append(ONE_SPACE);
					sqlBuffer.append(moreTableStruct[1].subAlias);
				}
				sqlBuffer.append(ONE_SPACE);
				sqlBuffer.append(K.on);
				sqlBuffer.append(ONE_SPACE);
//				if (moreTableStruct[1].joinExpression != null && !"".equals(moreTableStruct[1].joinExpression)) {
//					if (firstWhere) {
//						sqlBuffer2.append(" where ");
//						firstWhere = false;
//					} else {
//						sqlBuffer2.append(" and ");
//					}
					sqlBuffer.append(moreTableStruct[1].joinExpression);  //sqlBuffer  not sqlBuffer2
//				}
				
			}else{
			  //从表 最多两个
			  for (int s = 1; s <= 2; s++) { // 从表在数组下标是1和2. 0是主表
				if (moreTableStruct[s] != null) {
					
					useSubTableNames[s-1]=moreTableStruct[s].useSubTableName; //for conditon parse
					
					sqlBuffer.append(COMMA);
					sqlBuffer.append(moreTableStruct[s].tableName);
					tableNamesForCache+="##"+moreTableStruct[s].tableName; //V1.9
					if(moreTableStruct[s].hasSubAlias){//从表定义有别名
						sqlBuffer.append(ONE_SPACE);
						sqlBuffer.append(moreTableStruct[s].subAlias);
					}

					if (moreTableStruct[s].joinExpression != null && !"".equals(moreTableStruct[s].joinExpression)) {
						if (firstWhere) {
//							sqlBuffer2.append(" where ");
							sqlBuffer2.append(" ").append(K.where).append(" ");
							firstWhere = false;
						} else {
//							sqlBuffer2.append(" and ");
							sqlBuffer2.append(" ").append(K.and).append(" ");
						}
						sqlBuffer2.append(moreTableStruct[s].joinExpression);
					}
				}
			 }//for end
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
//						sqlBuffer2.append(" where ");
						sqlBuffer2.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
//						sqlBuffer2.append(" and ");
						sqlBuffer2.append(" ").append(K.and).append(" ");
					}
					sqlBuffer2.append(tableName);
					sqlBuffer2.append(DOT);
					sqlBuffer2.append(_toColumnName(fields[i].getName()));
					
					if (fields[i].get(entity) == null) {
//						sqlBuffer2.append(" is null");
						sqlBuffer2.append(" ").append(K.isNull);
					} else {
						sqlBuffer2.append("=");
						sqlBuffer2.append("?");

//						valueBuffer.append(",");
//						valueBuffer.append(fields[i].get(entity));

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
//					parseSubObject(sqlBuffer, valueBuffer, list, conditionFieldSet, firstWhere, includeType, moreTableStruct, index);
//					bug: firstWhere需要返回,传给condition才是最新的
//					firstWhere=parseSubObject(sqlBuffer, valueBuffer, list, conditionFieldSet, firstWhere, includeType, moreTableStruct, index);
					firstWhere=parseSubObject(sqlBuffer, list, conditionFieldSet, firstWhere, includeType, moreTableStruct, index);
				}
			}
			
			if(condition!=null){
				 condition.setSuidType(SuidType.SELECT);
//			     ConditionHelper.processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere,useSubTableNames);
			     ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere,useSubTableNames);
			}
			
			if(start!=-1 && size!=-1){ //若传参及Condition都有分页,转出来的sql可能语法不对.
				HoneyUtil.regPagePlaceholder();
				sql=dbFeature.toPageSql(sqlBuffer.toString(), start, size);
				HoneyUtil.setPageNum(list);
			}else{
				sql=sqlBuffer.toString();
			}
			
//			sqlBuffer.append(";");

//			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
//			HoneyContext.setSqlValue(sql, valueBuffer.toString()); //用于log显示
//			addInContextForCache(sql, tableName);// tableName还要加上多表的.
			addInContextForCache(sql, tableNamesForCache);//tableName还要加上多表的.
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

//		return sqlBuffer.toString();//bug
		return sql;
	}
	
	private static boolean parseSubObject(StringBuffer sqlBuffer2, 
			List<PreparedValue> list,  Set<String> conditionFieldSet, boolean firstWhere,
			 int includeType,MoreTableStruct moreTableStruct[],int index) {
		
		Object entity=moreTableStruct[index].subObject;
		
		if(entity==null) return firstWhere;
		
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
//					sqlBuffer2.append(" where ");
					sqlBuffer2.append(" ").append(K.where).append(" ");
					firstWhere = false;
				} else {
//					sqlBuffer2.append(" and ");
					sqlBuffer2.append(" ").append(K.and).append(" ");
				}
				sqlBuffer2.append(useSubTableName);
				sqlBuffer2.append(DOT);
				sqlBuffer2.append(_toColumnName(fields[i].getName()));
				
				if (fields[i].get(entity) == null) {
//					sqlBuffer2.append(" is null");
					sqlBuffer2.append(" ").append(K.isNull);
					
				} else {
					sqlBuffer2.append("=");
					sqlBuffer2.append("?");

//					valueBuffer.append(",");
//					valueBuffer.append(fields[i].get(entity));

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
		
		return firstWhere;
	}
	
//	static void addInContextForCache(String sql,String sqlValue, String tableName){
    static void addInContextForCache(String sql, String tableName){
		CacheSuidStruct struct=new CacheSuidStruct();
		struct.setSql(sql);
//		struct.setSqlValue(sqlValue);
		struct.setTableNames(tableName);
		
		HoneyContext.setCacheInfo(sql, struct);
	}
    
	private static <T> void checkPackage(T entity) {
		HoneyUtil.checkPackage(entity);
	}
	
//	private static String _toTableName(Object entity){
//		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
//	}
	
	private static String _toColumnName(String fieldName){
		return NameTranslateHandle.toColumnName(fieldName);
	}

//	private static String _toTableNameByEntityName(String entityName){
//		return NameTranslateHandle.toTableName(entityName);
//	}

}
