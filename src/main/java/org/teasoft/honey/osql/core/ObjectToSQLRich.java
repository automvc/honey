/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.ObjSQLIllegalSQLStringException;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeIllegalEntityException;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerPagingStruct;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * 对象到SQL的转换(对应SuidRich).Object to SQL string for SuidRich. 
 * @author Kingstar
 * @since  1.0
 */
public class ObjectToSQLRich extends ObjectToSQL implements ObjToSQLRich {

	private static final String ASC = K.asc;
	
	private DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}
	
	private void regPagePlaceholder(){
		HoneyUtil.regPagePlaceholder();
	}

	@Override
	public <T> String toSelectSQL(T entity, int start, int size) {
		
		String tableName="";
		if(isNeedRealTimeDb()) {
			tableName= _toTableName(entity);  //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(),tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}

		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
		regPagePlaceholder();
		adjustSqlServerPagingPkIfNeed(sql, entity.getClass());
		String beforeSql=sql;
		if (start == -1)
			sql = getDbFeature().toPageSql(sql, size);
		else
			sql = getDbFeature().toPageSql(sql, start, size);
		ShardingReg.regShadingPage(beforeSql, sql, start, size);
		
		HoneyUtil.setPageNum(wrap.getList());
		
		if(isNeedRealTimeDb()) {
			setContext(sql, wrap.getList(), tableName);
		}else {
			setContext(sql, wrap.getList(), wrap.getTableNames());
		}

		if (start == -1)
			Logger.logSQL("select SQL(entity, size): ", sql);
		else
			Logger.logSQL("select SQL(entity, start, size): ", sql);
		return sql;
	}
	
	@Override
	public <T> String toSelectSQL(T entity, int start, int size, String... selectFields) {

		String tableName="";
		if(isNeedRealTimeDb()) {
			tableName= _toTableName(entity);  //这里,取过了参数, 到解析sql的,就不能再取 
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(),tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}
		
		SqlValueWrap wrap = toSelectSQL_0(entity, selectFields);
		String sql = wrap.getSql();
		regPagePlaceholder();
		adjustSqlServerPagingPkIfNeed(sql, entity.getClass());
		String beforeSql=sql;
		sql = getDbFeature().toPageSql(sql, start, size);
		ShardingReg.regShadingPage(beforeSql, sql, start, size);
		HoneyUtil.setPageNum(wrap.getList());
		
		if(isNeedRealTimeDb()) {
			setContext(sql, wrap.getList(), tableName);
		}else {
			setContext(sql, wrap.getList(), wrap.getTableNames());
		}

		Logger.logSQL("select SQL(entity, start, size, selectFields): ", sql);
		return sql;
	}
	
	private void adjustSqlServerPagingPkIfNeed(String sql, Class entityClass) {
		
		if (!HoneyUtil.isSqlServer()) return ;
		
		String pkName = HoneyUtil.getPkFieldNameByClass(entityClass);
		
		if ("".equals(pkName)) return; //自定义主键为空,则不需要替换
		
		pkName = pkName.split(",")[0]; // 有多个,只取第一个
		pkName=_toColumnName(pkName, entityClass);
		
		SqlServerPagingStruct struct=new SqlServerPagingStruct();
		struct.setJustChangeOrderColumn(true);
		struct.setOrderColumn(pkName);
		HoneyContext.setSqlServerPagingStruct(sql, struct);
	}
	
	@Override
	public <T> String toSelectSQL(T entity, String... fields) {
		
		String newSelectFields=HoneyUtil.checkAndProcessSelectField(entity,fields);
		
		String sql = _ObjectToSQLHelper._toSelectSQL(entity, newSelectFields);

//		sql=sql.replace("#fieldNames#", fieldList);
//		sql=sql.replace("#fieldNames#", newSelectFields);  //打印值会有问题

		Logger.logSQL("select SQL(entity, selectFields): ", sql);

		return sql;
	}

	@Override
	public <T> String toSelectOrderBySQL(T entity, String orderFieldList) {

		String orderFields[] = orderFieldList.split(",");
		StringUtils.trim(orderFields);
		int lenA = orderFields.length;

		String orderBy = "";
		for (int i = 0; i < lenA; i++) {
			orderBy += _toColumnName(orderFields[i],entity.getClass()) + " " + ASC;
			if (i < lenA - 1) orderBy += ",";
		}
		ShardingReg.regShardingSort(orderBy, orderFields, null); //2.0
		
		SqlValueWrap wrap=toSelectSQL_0(entity);
		String sql=wrap.getSql();
		sql+=" "+K.orderBy+" "+orderBy;
		setContext(sql, wrap.getList(), wrap.getTableNames());
		
		return sql;
	}

	@Override
	public <T> String toSelectOrderBySQL(T entity, String orderFieldList, OrderType[] orderTypes) {
		
		String orderFields[] = orderFieldList.split(",");
		StringUtils.trim(orderFields);
		int lenA = orderFields.length;

		if (lenA != orderTypes.length) throw new ObjSQLException("ObjSQLException :The length of orderField is not equal orderTypes'.");

		String orderBy = "";
		for (int i = 0; i < lenA; i++) {
			orderBy += _toColumnName(orderFields[i],entity.getClass()) + " " + orderTypes[i].getName();
			if (i < lenA - 1) orderBy += ",";
		}

		ShardingReg.regShardingSort(orderBy, orderFields, orderTypes); //2.0
		
		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
		sql += " "+K.orderBy+" " + orderBy;
		setContext(sql, wrap.getList(), wrap.getTableNames());

		return sql;
	}
	
	private String[] adjustVariableString(String... fieldList) {

		if (fieldList == null) return new String[] { "" };

		String fields[];

		if (fieldList.length == 1) { // 变长参数,只有一个时,才允许用逗号隔开
			fields = fieldList[0].split(",");
		} else {
			fields = fieldList;
		}
		StringUtils.trim(fields);
		return fields;
	}

	@Override
	public <T> String toUpdateSQL(T entity, String... updateFieldList) {
		if (updateFieldList == null) return null;

		String sql = "";
//		String updateFields[] = updateFieldList.split(",");
		
		String updateFields[]=adjustVariableString(updateFieldList);

		if (updateFields.length == 0 || "".equals(updateFieldList[0].trim())) throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

		sql = _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, -1);
		return sql;
	}

	@Override
	public <T> String toUpdateSQL(T entity, IncludeType includeType, String... updateFieldList) {
		if (updateFieldList == null) return null;

		String sql = "";
//		String updateFields[] = updateFieldList.split(",");
		String updateFields[]=adjustVariableString(updateFieldList);

		if (updateFields.length == 0 || "".equals(updateFieldList[0].trim())) throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

		sql = _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, includeType.getValue());
		return sql;
	}

	@Override
	public <T> String toSelectFunSQL(T entity, FunctionType functionType,String fieldForFun,Condition condition){
		return _toSelectFunSQL(entity,functionType.getName(),fieldForFun,condition);
	}

	private <T> String _toSelectFunSQL(T entity, String funType,String fieldForFun,Condition condition){
		
		checkPackage(entity);
		
		if (fieldForFun == null || funType == null) return null;
		boolean isContainField = false;
		StringBuffer sqlBuffer = new StringBuffer();
		String sql = null;
		try {
			String tableName =_toTableName(entity);
			String selectAndFun;
			funType=FunAndOrderTypeMap.transfer(funType); //v1.9
			if ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun)) {
//		        selectAndFun = " select " + funType + "(" + fieldForFun + ") from ";  //  count(*)
//				selectAndFun = "select count(*) from ";
				selectAndFun = K.select+" "+K.count+"(*) "+K.from+" ";
			}else {
//				selectAndFun = "select " + funType + "(" + _toColumnName(fieldForFun) + ") from ";
				selectAndFun = K.select+" " + funType + "(" + _toColumnName(fieldForFun,entity.getClass()) + ") "+K.from+" ";   // funType要能转大小写风格
			}
			sqlBuffer.append(selectAndFun);
			tableName=ShardingUtil.appendTableIndexIfNeed(tableName);
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			Field fields[] = HoneyUtil.getFields(entity.getClass());
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0; i < len; i++) {
			  HoneyUtil.setAccessibleTrue(fields[i]);
				
//			  if (fields[i]!= null && fields[i].isAnnotationPresent(JoinTable.class)){//v1.7.0 排除多表的实体字段
//				continue;
//			  }
			  //bug , default can not filter the empty string.
//			  if (fields[i].get(entity) == null|| "serialVersionUID".equals(fields[i].getName()) || fields[i].isSynthetic()) {// 要排除没有设值的情况
////				if (fields[i].getName().equals(fieldForFun)) {
//				if ( (fields[i].getName().equals(fieldForFun))
//			     || ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun)) ) {  //排除count(*)
//					isContainField = true;
//				}
//				continue;
			  
				if ((fields[i].getName().equals(fieldForFun))
						|| ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun)) ) { //排除count(*)
					isContainField = true;
				}
					
				if (HoneyUtil.isContinue(-1, fields[i].get(entity),fields[i])) {
						continue;
				} else {
					if (fields[i].getName().equals(fieldForFun)) {
						isContainField = true;
					}

					if (firstWhere) {
						sqlBuffer.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" ").append(K.and).append(" ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName(),entity.getClass()));

					sqlBuffer.append("=");
					sqlBuffer.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);  //检测是否有影响？
					list.add(preparedValue);
				}
			}

			if (condition != null) {
				condition.setSuidType(SuidType.SELECT);
				OneTimeParameter.setTrueForKey(StringConst.Select_Fun);
				OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
				ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
			}
			
			sql = sqlBuffer.toString();
			
			setContext(sql, list, tableName);

			if (SqlStrFilter.checkFunSql(sql, funType)) {
				throw new ObjSQLIllegalSQLStringException("ObjSQLIllegalSQLStringException:sql statement with function is illegal. " + sql);
			}
			Logger.logSQL("select fun SQL : ", sql);
			if (!isContainField) throw new ObjSQLException("ObjSQLException:Miss The Field! The entity(" + tableName + ") don't contain the field:" + fieldForFun);

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}
	
	@Override
	public <T> String toSelectSQL(T entity, IncludeType includeType) {
		return _ObjectToSQLHelper._toSelectSQL(entity, includeType.getValue());
	}

	@Override
	public <T> String toDeleteSQL(T entity, IncludeType includeType) {
		return _ObjectToSQLHelper._toDeleteSQL(entity, includeType.getValue());
	}

	@Override
	public <T> String toInsertSQL(T entity, IncludeType includeType) {
		String sql = null;
		try {
//			_ObjectToSQLHelper.setInitIdByAuto(entity);   //move to last layer.
			sql = _ObjectToSQLHelper._toInsertSQL0(entity, includeType.getValue(),"");
//			HoneyUtil.revertId(entity); //v1.9  bug
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;

	}

	@Override
	public <T> String toUpdateSQL(T entity, IncludeType includeType) {
		String sql = "";
		sql = _ObjectToSQLHelper._toUpdateSQL(entity, includeType.getValue());
		return sql;
	}

	private static final String INDEX1 = "_SYS[index";
	private static final String INDEX2 = "]_End ";
	private static final String INDEX3 = "]";
	
	@Override
	public <T> String[] toInsertSQL(T entity[],int batchSize, String excludeFieldList) {
		
		if(HoneyUtil.isMysql()) return toInsertSQLForMysql(entity, batchSize, excludeFieldList);
		
		String sql[] = null;  
		try {
			int len = entity.length;
			
//			HoneyUtil.setInitArrayIdByAuto(entity);  //移到上游 2.1
			
			sql = new String[len];  //只用sql[0]
			String t_sql = "";

			t_sql = _ObjectToSQLHelper._toInsertSQL0(entity[0], 2, excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
			sql[0] = t_sql;
//			t_sql = t_sql + "[index0]";  //index0 不带,与单条共用.
			
			t_sql=shardingIndex() +t_sql; //fixed bug V2.2
			
			for (int i = 0; i < len; i++) { // i=1
				String sql_i=INDEX1 + i + INDEX2+t_sql;
				if (i == 0) {
					HoneyContext.setPreparedValue(sql_i, HoneyContext.getAndClearPreparedValue(sql[0]));   //i=0
//					HoneyContext.deleteCacheInfo(sql[0]); //fixed bug. V2.1.6  cache还会用到
				}else {
				  _ObjectToSQLHelper._toInsertSQL_for_ValueList(sql_i,entity[i], excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
//				  t_sql = wrap.getSql(); //  每个sql不一定一样,因为设值不一样,有些字段不用转换. 不采用;因为不利于批处理
				}
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}
	
	private String shardingIndex() {
		Integer subThreadIndex = HoneyContext.getSqlIndexLocal();
		String index = "";
		if (subThreadIndex != null) {
			index = " (sharding " + subThreadIndex + ")";
		}
		return index;
	}

	private <T> String[] toInsertSQLForMysql(T entity[],int batchSize, String excludeFieldList) {
		String sql[] = null;  
		try {
			int len = entity.length;
			
//			HoneyUtil.setInitArrayIdByAuto(entity); //移到上游 2.1
			
			sql = new String[len];  //只用sql[0]
			
			String t_sql = "";
 
			OneTimeParameter.setTrueForKey("_SYS_Bee_Return_PlaceholderValue");
			t_sql = _ObjectToSQLHelper._toInsertSQL0(entity[0], 2, excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
			sql[0] = t_sql;
			
			List<PreparedValue> preparedValueList = new ArrayList<>();
			
//			if (showSQL) {
//				preparedValueList.addAll(HoneyContext._justGetPreparedValue(sql[0]));  //打印后要手动清除
//			} else {
//				preparedValueList.addAll(HoneyContext.getPreparedValue(sql[0])); //会删了,打印日志时不能用.  批处理,在v1.8开始,不会用于占位设值.
//			}
			
			
//			preparedValueList.addAll(HoneyContext.justGetPreparedValue(sql[0]));  //统一使用这个.
			
//			if(len==1 || batchSize==1) {
//				HoneyContext.setPreparedValue(t_sql+ "  [Batch:"+ 0 + index3, preparedValueList); //[Batch:0]
//				preparedValueList = new ArrayList<>();
////				HoneyContext.clearPreparedValue(sql[0]);
//			}
			List<PreparedValue> oneRecoreList;
			t_sql=shardingIndex() +t_sql; //fixed bug V2.2
			for (int i = 0; i < len; i++) { // i=1
				String sql_i=INDEX1 + i + INDEX2+t_sql; //mysql批操作时,仅用于打印日志
				
				if (i == 0) {
					oneRecoreList = HoneyContext.getAndClearPreparedValue(sql[0]);
					HoneyContext.setPreparedValue(sql_i, oneRecoreList);   //i=0
//					HoneyContext.deleteCacheInfo(sql[0]); //fixed bug. V2.1.6  cache还会用到
				} else {
					//不需要打印时,不会放上下文,在方法内判断
					oneRecoreList = _ObjectToSQLHelper._toInsertSQL_for_ValueList(sql_i, entity[i],excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
					//t_sql = wrap.getSql(); //  每个sql不一定一样,因为设值不一样,有些字段不用转换. 不采用;因为不利于批处理
				}
				
				preparedValueList.addAll(oneRecoreList); //用于批量插入时设置值
				if((i+1)%batchSize==0){ //i+1
//					t_sql +"  [Batch:"+ (i/batchSize) + index3  // 用于批量插入时设置值
					HoneyContext.setPreparedValue(t_sql +"  [Batch:"+ (i/batchSize) + INDEX3, preparedValueList); //i
					preparedValueList = new ArrayList<>();
				}else if(i==(len-1)){
					HoneyContext.setPreparedValue(t_sql +"  [Batch:"+ (i/batchSize) + INDEX3, preparedValueList); //i
				}
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public String toDeleteByIdSQL(Class c, Integer id) {
		if(id==null) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		String pkName=getPkName(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Integer", pkName, c);
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public String toDeleteByIdSQL(Class c, Long id) {
		if(id==null) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		String pkName=getPkName(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Long", pkName, c);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String toDeleteByIdSQL(Class c, String ids) {
		if(ids==null || "".equals(ids.trim())) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		String pkName=getPkName(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, ids, getIdTypeByClass(c, pkName), pkName, c);
	}

	@SuppressWarnings("rawtypes")
	private  SqlValueWrap toDeleteByIdSQL0(Class c){
		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		
		String tableName =_toTableNameByClass(c);
		
		sqlBuffer.append(K.delete).append(" ").append(K.from).append(" ")
		.append(tableName)  // 下一版本将支持分片
		.append(" ").append(K.where).append(" ");
		;
		
		wrap.setValueBuffer(sqlBuffer); //sqlBuffer
		wrap.setTableNames(tableName);
		
		return wrap;
	}
	
	private <T> String getPkName(T entity) {
		return getPkName(entity.getClass());
	}
	
	@SuppressWarnings("rawtypes")
	private String getPkName(Class c) {
		try {
			HoneyUtil.getField(c, "id"); //V1.11 因主键可以不是默认id,多了此步检测
			return "id";
		} catch (NoSuchFieldException e) {
			String pkName = HoneyUtil.getPkFieldNameByClass(c);
			if ("".equals(pkName))
				throw new ObjSQLException("No primary key in " + c.getName());
			if (pkName.contains(",")) throw new ObjSQLException(
					"method of selectById just need one primary key, but more than one primary key in "
							+ c.getName());
			return pkName;
		}
	}
	
	@Override
	public <T> String toSelectByIdSQL(Class<T> entityClazz, Integer id) {
		SqlValueWrap sqlBuffer = toSelectByIdSQL0(entityClazz);
		String pkName=getPkName(entityClazz);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Integer",pkName,entityClazz);
	}

	@Override
	public <T> String toSelectByIdSQL(Class<T> entityClazz, Long id) {
		SqlValueWrap sqlBuffer = toSelectByIdSQL0(entityClazz);
		String pkName=getPkName(entityClazz);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Long",pkName,entityClazz);
	}

	@Override
	public <T> String toSelectByIdSQL(Class<T> entityClazz, String ids) {
		if(ids==null || "".equals(ids.trim())) return null;
		SqlValueWrap sqlValueWrap=toSelectByIdSQL0(entityClazz);
		String pkName=getPkName(entityClazz);
		return _toSelectAndDeleteByIdSQL(sqlValueWrap,ids,getIdType(entityClazz,pkName),pkName,entityClazz);
	}
	
	private <T> String getIdType(Class<T> entityClazz,String pkName) {
		Field field = null;
		String type=null;
		try {
			field = HoneyUtil.getField(entityClazz, pkName);
			type=field.getType().getSimpleName();
		} catch (Exception e) {
			//ignore
		}
		
		return type;
	}
	
	@SuppressWarnings("rawtypes")
	private String getIdTypeByClass(Class c,String pkName) {
		Field field = null;
		String type=null;
		try {
			field = HoneyUtil.getField(c, pkName);
			type=field.getType().getSimpleName();
		} catch (Exception e) {
			//ignore
		}
		
		return type;
	}

	private <T> String _toUpdateBySQL(T entity, int includeType, String... whereFieldList) {
		if (whereFieldList == null) return null;
		if (whereFieldList.length == 0 || "".equals(whereFieldList[0].trim())) throw new ObjSQLException("ObjSQLException:whereFieldList at least include one field.");

		String sql = "";
//		String whereFields[] = whereFieldList.split(",");
		String whereFields[] = adjustVariableString(whereFieldList);


		sql = _ObjectToSQLHelper._toUpdateBySQL(entity, whereFields, includeType);
		return sql;
	}
	
	@Override
	public <T> String toUpdateBySQL(T entity, String... whereFieldList) {
	    return _toUpdateBySQL(entity, -1, whereFieldList);
	}

	@Override
	public <T> String toUpdateBySQL(T entity, IncludeType includeType, String... whereFieldList) {
		return _toUpdateBySQL(entity, includeType.getValue(), whereFieldList);
	}

	@Override
	public <T> String toUpdateBySQL(T entity, Condition condition, String... whereFieldList) {

		if (whereFieldList.length == 0 || "".equals(whereFieldList[0].trim()))
			throw new ObjSQLException("ObjSQLException:whereFieldList at least include one field.");
		
//		String whereFields[] = whereFieldList.split(",");
		String whereFields[] = adjustVariableString(whereFieldList);

		if (condition == null || condition.getIncludeType() == null) {
			return _ObjectToSQLHelper._toUpdateBySQL(entity, whereFields, -1, condition); //includeType=-1
		} else {
			return _ObjectToSQLHelper._toUpdateBySQL(entity, whereFields, condition.getIncludeType().getValue(), condition);
		}
	}

	@Override
	public <T> String toUpdateSQL(T entity, Condition condition, String... updateFieldList) {
		
//		if(updateFieldList==null) updateFieldList="";
//		String updateFields[] = updateFieldList.split(","); //setColmns
		String updateFields[] = adjustVariableString(updateFieldList); //setColmns
		
		
		
//		if (updateFields.length == 0 || "".equals(updateFieldList.trim()))  //close in v1.8    because: set can define in condition
//			throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

		if (condition == null || condition.getIncludeType() == null) {
			return _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, -1, condition);//includeType=-1
		} else {
			return _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, condition.getIncludeType().getValue(), condition);
		}
	}

	private String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, Number id,String numType,String pkName,Class entityClass) {
		if(id==null) return null;
		
		StringBuffer sqlBuffer=wrap.getValueBuffer();  //sqlBuffer
		sqlBuffer.append(_id(pkName, entityClass) + "=").append("?");

		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = new PreparedValue();
		preparedValue.setType(numType);
		preparedValue.setValue(id);
		list.add(preparedValue);
		
		setContext(sqlBuffer.toString(), list, wrap.getTableNames());
		
		return sqlBuffer.toString();
	}
	
	private String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, String ids, String idType,String pkName,Class entityClass) {

		StringBuffer sqlBuffer=wrap.getValueBuffer();
		List<PreparedValue> list=new ArrayList<>();
		PreparedValue preparedValue=null;

		String idArray[]=ids.split(",");
		StringUtils.trim(idArray);
		String id0=_id(pkName,entityClass) + "=?";
		String t_ids="";

//		preparedValue=new PreparedValue();
//		if (idType != null) {
//			preparedValue.setType(idType);
//			if ("Long".equals(idType) || "long".equals(idType)) {
//				preparedValue.setValue(Long.parseLong(idArray[0]));
//			} else if ("Integer".equals(idType) || "int".equals(idType)) {
//				preparedValue.setValue(Integer.parseInt(idArray[0]));
//			} else if ("Short".equals(idType) || "short".equals(idType)) {
//				preparedValue.setValue(Short.parseShort(idArray[0]));
//			} else {
//				preparedValue.setValue(idArray[0]);
//			}
//		} else {
//			preparedValue.setValue(idArray[0]);
//		}
//		list.add(preparedValue);

		for (int i = 0; i < idArray.length; i++) {
			preparedValue = new PreparedValue();
			if (i == 0) {
				t_ids = id0;
			} else {
				t_ids += " " + K.or + " " + id0;
			}
			if (idType != null) {
				preparedValue.setType(idType);

				if ("Long".equals(idType) || "long".equals(idType)) {
					preparedValue.setValue(Long.parseLong(idArray[i]));
				} else if ("Integer".equals(idType) || "int".equals(idType)) {
					preparedValue.setValue(Integer.parseInt(idArray[i]));
				} else if ("Short".equals(idType) || "short".equals(idType)) {
					preparedValue.setValue(Short.parseShort(idArray[0]));
				} else {
					preparedValue.setValue(idArray[i]);
				}
			} else {
				preparedValue.setValue(idArray[i]);
			}
			list.add(preparedValue);
		}

		sqlBuffer.append(t_ids);
		setContext(sqlBuffer.toString(), list, wrap.getTableNames());

		return sqlBuffer.toString();
	}
	
	private <T> SqlValueWrap toSelectByIdSQL0(Class<T> entityClazz) {
		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();

		String tableName = _toTableNameByClass(entityClazz);

		String packageAndClassName = entityClazz.getName();
		String columnNames = HoneyContext.getBeanField(packageAndClassName);
		if (columnNames == null) {
			Field fields[] = HoneyUtil.getFields(entityClazz);
			columnNames = HoneyUtil.getBeanField(fields,entityClazz);
			HoneyContext.addBeanField(packageAndClassName, columnNames);
		}

		sqlBuffer.append(K.select).append(" ").append(columnNames).append(" ").append(K.from).append(" ");
		sqlBuffer.append(tableName) //下一版本将支持分片
		.append(" ").append(K.where).append(" ");

		wrap.setValueBuffer(sqlBuffer);
		wrap.setTableNames(tableName);

		return wrap;
	}

	private <T> SqlValueWrap toSelectSQL_0(T entity) {
		String[] arg=null;
		return toSelectSQL_0(entity,arg);
	}
	
	private <T> SqlValueWrap toSelectSQL_0(T entity, String... selectField) {

		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		try {
			String tableName ="";
			
			if (isNeedRealTimeDb()) {
				tableName = (String) OneTimeParameter.getAttribute(StringConst.TABLE_NAME);
				if (tableName == null) {
					tableName = _toTableName(entity);
				}
			}else {
				tableName = _toTableName(entity);
			}
			
			Field fields[] = HoneyUtil.getFields(entity.getClass()); //返回所有字段,包括公有和私有   
			
			String columnNames ="";
			if (selectField != null && !"".equals(selectField[0].trim())) {
				columnNames = HoneyUtil.checkAndProcessSelectField(entity, selectField);
			} else {
				String packageAndClassName = entity.getClass().getName();
				columnNames = HoneyContext.getBeanField(packageAndClassName);
				if (columnNames == null) {
					columnNames = HoneyUtil.getBeanField(fields,entity.getClass());
					HoneyContext.addBeanField(packageAndClassName, columnNames);
				}
			}
			sqlBuffer.append(K.select).append(" ").append(columnNames).append(" ").append(K.from).append(" ");
			tableName=ShardingUtil.appendTableIndexIfNeed(tableName);
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0; i < len; i++) {
				HoneyUtil.setAccessibleTrue(fields[i]);
				
				if (HoneyUtil.isContinue(-1, fields[i].get(entity),fields[i])) {
					continue;	
				}else {
					if (firstWhere) {
						sqlBuffer.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" ").append(K.and).append(" ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName(),entity.getClass()));
					
					sqlBuffer.append("=");
					sqlBuffer.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
					list.add(preparedValue);
				}
			}

			wrap.setTableNames(tableName);
			wrap.setSql(sqlBuffer.toString());
			wrap.setList(list);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return wrap;
	}

	
	private static void setContext(String sql,List<PreparedValue> list,String tableName){
		HoneyContext.setContext(sql, list, tableName);
	}
	
	private static <T> void checkPackage(T entity) {
//		传入的实体可以过滤掉常用的包开头的,如:java., javax. ; 但spring开头不能过滤,否则spring想用bee就不行了.
		HoneyUtil.checkPackage(entity);
	}
	
	@SuppressWarnings("rawtypes")
	public static void checkPackageByClass(Class c){
		if(c==null) return;
//		String packageName=c.getPackage().getName();  //bug
		String classFullName=c.getName();
		if(classFullName.startsWith("java.") || classFullName.startsWith("javax.") || classFullName.startsWith("jakarta.")){
			throw new BeeIllegalEntityException("BeeIllegalEntityException: Illegal Entity, "+c.getName());
		}
	}
	
	private String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
		//TODO
//		String tableName= NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
//		return ShardingUtil.appendTableIndexIfNeed(tableName); // 2.4.2
	}
	
	@SuppressWarnings("rawtypes")
	private String _toTableNameByClass(Class c){
		return NameTranslateHandle.toTableName(c.getName());
	}
	
	@SuppressWarnings("rawtypes")
	private static String _toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
	}
	
	private static String _id(String pkName,Class entityClass){ 
		return NameTranslateHandle.toColumnName(pkName,entityClass);
	}
	
	private boolean isNeedRealTimeDb() {
		return HoneyContext.isNeedRealTimeDb();
	}
}
