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
import org.teasoft.bee.osql.annotation.PrimaryKey;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.7
 */
public class _MoreObjectToSQLHelper {
	
	private static final String COMMA=",";
	private static final String ONE_SPACE = " ";
	private static final String DOT=".";
	
	private _MoreObjectToSQLHelper(){}
	
	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}
	
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
		Set<String> whereFields=null;
		if(condition!=null) whereFields=condition.getWhereFields();
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlBuffer0 = new StringBuffer();//放主表的where条件
		StringBuffer sqlBuffer2 = new StringBuffer();
		boolean firstWhere = true; 
		
		try {
			
			String tableName = _toTableName(entity); 
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			OneTimeParameter.setTrueForKey(StringConst.MoreStruct_to_SqlLib);
			
			//不能到分页时才设置,因多表时,有重名字段,也需要用到dbName判断使用不同的语法,而动态获取dbName要用到路由.
			if(HoneyContext.isNeedRealTimeDb()) { //V1.9
				HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(),tableName);  //main table confirm the Datasource.
				OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
			}
			
			MoreTableStruct moreTableStruct[]=HoneyUtil.getMoreTableStructAndCheckBefore(entity);
			
			if (moreTableStruct[1] == null) { //v1.9
				throw new BeeErrorGrammarException(
						"MoreTable select on " + entity.getClass().getName() + " must own at least one JoinTable annotation!");
			}
			
			boolean twoTablesWithJoinOnStyle=HoneyConfig.getHoneyConfig().moreTable_twoTablesWithJoinOnStyle;
			
			boolean moreTable_columnListWithStar=HoneyConfig.getHoneyConfig().moreTable_columnListWithStar;
			String columnNames;
			columnNames=moreTableStruct[0].columnsFull;
			
			List<PreparedValue> list = new ArrayList<>();
			List<PreparedValue> mainList = new ArrayList<>();
			boolean needAdjustPageForList=false;
			String sqlStrForList="";
			
			//解析主表实体的where条件       分页要调整的,顺序早于opOn,  但where部分却迟于opOn
//			firstWhere=parseMainObject(entity, tableName, sqlBuffer0, mainList, firstWhere, includeType);  //顺序有问题
			
//			Integer pageStart=ConditionHelper.getPageStart(condition); //不需要判断
			Integer pageSize=ConditionHelper.getPageSize(condition);
			
			//多表查询不同时传   start!=-1 && size!=-1 ,   condition
			if(moreTableStruct[0].subOneIsList) {  //从表1是List,且需要分页
					
//				从表有一条记录 已包含在condition!=null里,也是不会转换的
				if(start==-1 && size==-1 && condition==null) {
					//do nothing
				}else if(start==-1 && size==-1 && pageSize==null) {
					//do nothing
//				}else if(start!=-1 && size!=-1 && condition==null){ 从表有值, 有可能不正确,所以不改写.	
				}else if(start!=-1 && size!=-1 && condition==null && moreTableStruct[1].subObject==null){ 
					
//					若condition!=null, 要判断不包括从表的字段.  todo
					
//					主表id不为空的,也不用.  因主表最多能查一条记录
					
					parseMainObject(entity, tableName, sqlBuffer0, mainList, firstWhere, includeType); //因顺序原因,调整时,需要多解析一次
					Boolean idHasValue=OneTimeParameter.isTrue("idHasValue");
					
					if(! idHasValue) {  //right join也不管用.     List类型,不允许用right join
					
						needAdjustPageForList=true; //List类型子表,调整sql的情型
						StringBuffer sqlForList = new StringBuffer();
						
//						String mainColumnsForListType=moreTableStruct[0].mainColumnsForListType;
						sqlForList.append(K.select).append(" ")
//						.append(mainColumnsForListType)
						.append("*") //用于调整(改写)sql的
						.append(" ").append(K.from).append(" ");
						sqlForList.append(tableName);
						sqlForList.append(sqlBuffer0); //添加解析主表实体的where条件
						
//						HoneyUtil.regPagePlaceholder();
						sqlStrForList=getDbFeature().toPageSql(sqlForList.toString(), start, size);
//						HoneyUtil.setPageNum(list);
						
					    //后面不用再分页.
					    start = -1;
					    size = -1;
				
					}
				}else if( (start!=-1 && size!=-1) || pageSize!=null) {
					//因分页,从表是List类型的,得到的数据条数未必准确
					Logger.warn("MoreTable subTable's type is List, paging maybe not accurate!");
				}
			}
			
			if (condition != null) {
				condition.setSuidType(SuidType.SELECT);
				
//				ConditionHelper.processOnExpression(condition,moreTableStruct,list);  // on expression    因顺序原因,不放在这
				
				String selectField = ConditionHelper.processSelectField(columnNames, condition,moreTableStruct[0].subDulFieldMap);
				
				//v1.9.8  给声明要查的字段自动加上 表名.
				selectField=_addMaintableForSelectField(selectField,tableName);
				
				//v1.9
				String fun=ConditionHelper.processFunction(columnNames, condition);  //字段相同,要取不一样的别名,才要传subDulFieldMap
				
				if (selectField != null && StringUtils.isEmpty(fun)) {
					columnNames = selectField;
				}else if (selectField != null && StringUtils.isNotEmpty(fun)) {
					columnNames = selectField + "," + fun;
				}else if (selectField == null && StringUtils.isNotEmpty(fun)) {
					columnNames = fun;
				}else {
				    //若指定了字段,则测试也不用*代替
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
//			String tableName = moreTableStruct[0].tableName;
			
//			String tableNamesForCache=tableName;//V1.9
			StringBuffer tableNamesForCache=new StringBuffer(tableName);//V1.9
			sqlBuffer.append(K.select).append(" ").append(columnNames).append(" ").append(K.from).append(" ");
			
			if(needAdjustPageForList) {
				sqlBuffer.append("(");
				sqlBuffer.append(sqlStrForList);
				sqlBuffer.append(")");
				sqlBuffer.append(" ");
				list.addAll(mainList);
			}
			
			sqlBuffer.append(tableName);
			
//			PreparedValue preparedValue = null;
			
//			String useSubTableNames[]=new String[2];
			String useSubTableNames[]=new String[3];  //v1.9.8 useSubTableNames[2] add main tableName 放主表实际表名
			
			//closed on v1.9.8
			//v1.7.1 当有两个子表时,即使配置了用join..on,也会解析成where m1=sub1f1 and m2=sub2f1
//			if(moreTableStruct[0].joinTableNum>1 && twoTablesWithJoinOnStyle && moreTableStruct[1].joinType==JoinType.JOIN){
//				Logger.warn("SQL grammar type will use 'where ... =' replace 'join .. on' !");
//			}
			
			if (condition != null) {
				OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
				ConditionHelper.processOnExpression(condition,moreTableStruct,list);  // on expression
			}
			
			// 2 left join, rith join ...  
			if (moreTableStruct[0].joinTableNum == 2
				&& StringUtils.isNotBlank(moreTableStruct[1].joinExpression)
				&& StringUtils.isNotBlank(moreTableStruct[2].joinExpression)) {

				addJoinPart(sqlBuffer, moreTableStruct[1], tableNamesForCache);
				sqlBuffer.append(ONE_SPACE);
				addJoinPart(sqlBuffer, moreTableStruct[2], tableNamesForCache);
			
			//只有一个子表关联,且选用join type
			//排除以下情况: where m1=sub1f1 and m2=sub2f1 (放到else处理)
		   }else if( (moreTableStruct[1].joinType!=JoinType.JOIN || (twoTablesWithJoinOnStyle && moreTableStruct[0].joinTableNum==1) )
			 &&(StringUtils.isNotBlank(moreTableStruct[1].joinExpression) ) ){ //需要有表达式
			   
				addJoinPart(sqlBuffer, moreTableStruct[1], tableNamesForCache);
				
			}else{//where写法
			  //从表 最多两个
			  for (int s = 1; s <= 2; s++) { // 从表在数组下标是1和2. 0是主表
				if (moreTableStruct[s] != null) {
					
					useSubTableNames[s-1]=moreTableStruct[s].useSubTableName; //for conditon parse
					
					sqlBuffer.append(COMMA);
					sqlBuffer.append(moreTableStruct[s].tableName);
//					tableNamesForCache+="##"+moreTableStruct[s].tableName; //V1.9
					tableNamesForCache.append("##").append(moreTableStruct[s].tableName);//v1.9.8
					if(moreTableStruct[s].hasSubAlias){//从表定义有别名
						sqlBuffer.append(ONE_SPACE);
						sqlBuffer.append(moreTableStruct[s].subAlias);
					}

					if (StringUtils.isNotBlank(moreTableStruct[s].joinExpression)) {
						if (firstWhere) {
							sqlBuffer2.append(" ").append(K.where).append(" ");
							firstWhere = false;
						} else {
							sqlBuffer2.append(" ").append(K.and).append(" ");
						}
						sqlBuffer2.append(moreTableStruct[s].joinExpression);
					}
				}
			 }//for end
		    }
			
			//添加解析主表实体的where条件
//			sqlBuffer2.append(sqlBuffer0);
//			list.addAll(mainList);
			
			firstWhere=parseMainObject(entity, tableName, sqlBuffer2, list, firstWhere, includeType);   //sqlBuffer2
			
			sqlBuffer.append(sqlBuffer2);
			
			
			InterceptorChain chain=null;
			//处理子表相应字段到where条件
			for (int index = 1; index <= 2; index++) { // 从表在数组下标是1和2. 0是主表   sub table index is :1 ,2 
				if(index==1) chain=(InterceptorChain)OneTimeParameter.getAttribute(StringConst.InterceptorChainForMoreTable);
				if (moreTableStruct[index] != null) {
//					parseSubObject(sqlBuffer, valueBuffer, list, conditionFieldSet, firstWhere, includeType, moreTableStruct, index);
//					bug: firstWhere需要返回,传给condition才是最新的
//					firstWhere=parseSubObject(sqlBuffer, valueBuffer, list, conditionFieldSet, firstWhere, includeType, moreTableStruct, index);
					doBeforePasreSubEntity(moreTableStruct[index].subObject, chain);//V1.11
					firstWhere=parseSubObject(sqlBuffer, list, whereFields, firstWhere, includeType, moreTableStruct, index);
				}
			}
			
			if (HoneyContext.isNeedRealTimeDb()) {
				HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), tableName);
				OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
			}
			
			if(condition!=null){
				 condition.setSuidType(SuidType.SELECT);
				 useSubTableNames[2]=tableName;   //v1.9.8 useSubTableNames[2] add main tableName 放主表实际表名
				 
				 OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
//			     ConditionHelper.processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere,useSubTableNames);
			     ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere,useSubTableNames);
			}
			
			if(start!=-1 && size!=-1){ //若传参及Condition都有分页,转出来的sql可能语法不对.
				HoneyUtil.regPagePlaceholder();
				sql=getDbFeature().toPageSql(sqlBuffer.toString(), start, size);
				HoneyUtil.setPageNum(list);
			}else{
				sql=sqlBuffer.toString();
			}
			
			HoneyContext.setPreparedValue(sql, list);
			addInContextForCache(sql, tableNamesForCache.toString());//tableName还要加上多表的.
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}
	private static void doBeforePasreSubEntity(Object entity,InterceptorChain chain) {
		if(entity!=null && chain!=null) chain.beforePasreEntity(entity, SuidType.SELECT);
	}
	
	private static boolean parseSubObject(StringBuffer sqlBuffer2, 
			List<PreparedValue> list,  Set<String> conditionFieldSet, boolean firstWhere,
			 int includeType,MoreTableStruct moreTableStruct[],int index) throws IllegalAccessException{
		
		Object entity=moreTableStruct[index].subObject;
		
		if(entity==null) return firstWhere;
		
		PreparedValue preparedValue = null;
		
//		String tableName = moreTableStruct[index].tableName;
		String useSubTableName = moreTableStruct[index].useSubTableName;
		
		Field fields[] = null;
		if (index == 1 && moreTableStruct[0].subOneIsList) {
			fields = moreTableStruct[index].subClass.getDeclaredFields();
		} else if (index == 2 && moreTableStruct[0].subTwoIsList) {
			fields = moreTableStruct[index].subClass.getDeclaredFields();
		} else {
			fields = moreTableStruct[index].subEntityField.getType().getDeclaredFields();
		}
		
		
		int len = fields.length;
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
				
//				if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
//					continue; //id=null不作为过滤条件
				if(isNullPkOrId(fields[i], entity)) continue; //主键=null不作为过滤条件
				
//				if(conditionFieldSet!=null && conditionFieldSet.contains(fields[i].getName()))   //closed in V1.9
//					continue; //Condition已包含的,不再遍历

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
				sqlBuffer2.append(_toColumnName(fields[i].getName(),entity.getClass()));
				
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
	
	private static String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	@SuppressWarnings("rawtypes")
	private static String _toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
	}

//	private static String _toTableNameByEntityName(String entityName){
//		return NameTranslateHandle.toTableName(entityName);
//	}
	
	//为指定字段,没有带表名的,自动填上主表表名.
	private static String _addMaintableForSelectField(String selectField, String mainTableName) {

		if (StringUtils.isBlank(selectField)) return selectField;

		//String居然没有检测包含某字符总数的api 
		
		String newStr = "";
		String str[] = selectField.split(",");
		int len = str.length;
		for (int i = 0; i < len; i++) {
			if (!str[i].contains(".")) {
				str[i] = mainTableName + "." + str[i].trim();
			}
			newStr += str[i];
			if (i != len - 1) newStr += ",";
		}
		return newStr;
	}
	
	private static void addJoinPart(StringBuffer sqlBuffer,MoreTableStruct moreTableStruct,StringBuffer tableNamesForCache) {
		
		if(moreTableStruct.joinType==JoinType.FULL_JOIN){
			Logger.warn("Pleae confirm the Database supports 'full join' type!");
		}
		
		if(HoneyUtil.isSqlKeyWordUpper())sqlBuffer.append(moreTableStruct.joinType.getType().toUpperCase());
		else                             sqlBuffer.append(moreTableStruct.joinType.getType());
		sqlBuffer.append(moreTableStruct.tableName);
		tableNamesForCache.append("##").append(moreTableStruct.tableName);//v1.9.8
		if(moreTableStruct.hasSubAlias){//从表定义有别名
			sqlBuffer.append(ONE_SPACE);
			sqlBuffer.append(moreTableStruct.subAlias);
		}
		sqlBuffer.append(ONE_SPACE);
		sqlBuffer.append(K.on);
		sqlBuffer.append(ONE_SPACE);
		
		sqlBuffer.append(moreTableStruct.joinExpression);  //sqlBuffer  not sqlBuffer2
		
		if(StringUtils.isNotBlank(moreTableStruct.onExpression)) { //v1.9.8 on expression
			sqlBuffer.append(ONE_SPACE);
			sqlBuffer.append(K.and);  //and
			sqlBuffer.append(ONE_SPACE);
			sqlBuffer.append(moreTableStruct.onExpression);
		}
	}
	
	private static <T> boolean parseMainObject(T entity,String tableName,StringBuffer sqlBuffer0, 
				List<PreparedValue> list, boolean firstWhere, int includeType) throws IllegalAccessException{
		
		Field fields[] = entity.getClass().getDeclaredFields(); 
		PreparedValue preparedValue=null;
		
		int len = fields.length;
		for (int i = 0; i < len; i++) {
			fields[i].setAccessible(true);
//			if (fields[i].isAnnotationPresent(JoinTable.class)) {
//				continue;  //JoinTable已在上面另外处理
//			}
//			if (HoneyUtil.isContinueForMoreTable(includeType, fields[i].get(entity),fields[i].getName())) {
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {  //包含了fields[i].isAnnotationPresent(JoinTable.class)的判断
				continue;
			} else {
				
//				if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
//					continue; //id=null不作为过滤条件
				if(isNullPkOrId(fields[i], entity)) continue; //主键=null不作为过滤条件
				
				if (fields[i].get(entity) != null && isPrimaryKey(fields[i])) {
					OneTimeParameter.setTrueForKey("idHasValue");
				}
					
//				if(whereFields!=null && whereFields.contains(fields[i].getName()))   //closed in V1.9
//					continue; //Condition已包含的,不再遍历

				if (firstWhere) {
					sqlBuffer0.append(" ").append(K.where).append(" ");
					firstWhere = false;
				} else {
					sqlBuffer0.append(" ").append(K.and).append(" ");
				}
				sqlBuffer0.append(tableName);
				sqlBuffer0.append(DOT);
				sqlBuffer0.append(_toColumnName(fields[i].getName(),entity.getClass()));
				
				if (fields[i].get(entity) == null) {
					sqlBuffer0.append(" ").append(K.isNull);
				} else {
					sqlBuffer0.append("=");
					sqlBuffer0.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					list.add(preparedValue);
				}
			}
		}//end for
		
		return firstWhere;
	}
	
	//V1.11
	private static boolean isNullPkOrId(Field field, Object entity) {
		try {
//			if (field.get(entity) == null && "id".equalsIgnoreCase(field.getName())) return true;
//			if (field.get(entity) == null && field.isAnnotationPresent(PrimaryKey.class)) return true;
			if (field.get(entity) == null && isPrimaryKey(field)) return true;
		} catch (Exception e) {
			//ignroe
		}
		return false;
	}
	
	//V1.11
	private static boolean isPrimaryKey(Field field) {
		if ("id".equalsIgnoreCase(field.getName())) return true;
		if (field.isAnnotationPresent(PrimaryKey.class)) return true;
		return false;
	}
	
}
