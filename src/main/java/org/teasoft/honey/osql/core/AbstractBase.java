/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.BatchUpdateException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.teasoft.bee.osql.Cache;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.type.TypeHandler;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.sharding.ShardingUtil;

/**
 * @author Kingstar
 * @since  2.0
 */
public abstract class AbstractBase {
	
	private int cacheWorkResultSetSize=HoneyConfig.getHoneyConfig().cache_workResultSetSize;
	private boolean showSQL = getShowSQL();
	private boolean showShardingSQL = getShowShardingSQL();
	
	protected static boolean openFieldTypeHandler = HoneyConfig.getHoneyConfig().openFieldTypeHandler;
	
	private Cache cache;
	public Cache getCache() {
		if(cache==null) {
			cache=BeeFactory.getHoneyFactory().getCache();
		}
		return cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}
	
	@SuppressWarnings("rawtypes")
	protected static String _toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
	}

	protected void addInCache(String sql, Object rs, int resultSetSize) {
		if(HoneyContext.getSqlIndexLocal()!=null) return ; //子查询不放缓存
//		如果结果集超过一定的值则不放缓存
		if(resultSetSize>cacheWorkResultSetSize){
		   HoneyContext.deleteCacheInfo(sql);
		   return;
		}
//		returnType, suidType  已不使用; 因一进来updateInfoInCache时,已添加有
		getCache().add(sql, rs);
	}
	
//	查缓存前需要先更新缓存信息,才能去查看是否在缓存
	@SuppressWarnings("rawtypes")
	protected boolean updateInfoInCache(String sql, String returnType, SuidType suidType,Class entityClass) {
		if(HoneyContext.getSqlIndexLocal()!=null) return false; //子查询不放缓存
		return HoneyContext.updateInfoInCache(sql, returnType, suidType, entityClass);
	}
	
	//清空缓存不需要entityClass;   原始sql对应的table相关的缓存都会删除。
	protected void clearInCache(String sql, String returnType, SuidType suidType, int affectRow) {
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
			struct.setReturnType(returnType);
			struct.setSuidType(suidType.getType());
			HoneyContext.setCacheInfo(sql, struct);
		}
		clearContext(sql);
		if (affectRow > 0) { //INSERT、UPDATE 或 DELETE成功,才清除结果缓存
			getCache().clear(sql);
		}
	}
	
	protected void clearContext(String sql) {
		HoneyContext.clearPreparedValue(sql); // close in 2.0  ???
//		if(HoneyContext.isNeedRealTimeDb() && HoneyContext.isAlreadySetRoute()) { //当可以从缓存拿时，需要清除为分页已设置的路由
//			HoneyContext.removeCurrentRoute(); //放到拦截器中
//		}
	}
	
	@SuppressWarnings("rawtypes")
	protected void initRoute(SuidType suidType, Class clazz, String sql) {
		boolean enableMultiDs=HoneyConfig.getHoneyConfig().multiDS_enable;
//		if (!enableMultiDs) return;  //close in 1.17
		if (!enableMultiDs && !HoneyContext.useStructForLevel2()) return; //1.17 fixed
		if(HoneyContext.isNeedRealTimeDb() && HoneyContext.isAlreadySetRoute()) return; // already set in parse entity to sql.
		//enableMultiDs=true,且还没设置的,都要设置   因此,清除时,也是这样清除.
		HoneyContext.initRoute(suidType, clazz, sql);
	}
	
	protected boolean isConfuseDuplicateFieldDB(){
		return HoneyUtil.isConfuseDuplicateFieldDB();
	}
	
	protected void logSelectRows(int size) {
		if (ShardingUtil.isSharding()&& !showShardingSQL) return ;
		Logger.logSQL(" | <--  select rows: ", size + "" + shardingIndex());
	}
	
	protected void logAffectRow(int num) {
		if (ShardingUtil.isSharding()&& !showShardingSQL) return ;		
		Logger.logSQL(" | <--  Affected rows: ", num+""+shardingIndex());
	}
	
	protected void logDsTab() {
		if (! showShardingSQL) return ;
		List<String> dsNameListLocal=HoneyContext.getListLocal(StringConst.DsNameListLocal);
		List<String> tabNameList=HoneyContext.getListLocal(StringConst.TabNameListLocal);
		Logger.logSQL("========= Involved DataSource: "+dsNameListLocal+"  ,Involved Table: "+tabNameList);
	}
	
	//主线程才打印（不需要分片或不是子线程）  colse 2.1
//	protected static void logSQLForMain(String hardStr) {
//		if (!ShardingUtil.hadSharding() || HoneyContext.getSqlIndexLocal() == null)
//			Logger.logSQL(hardStr);
//	}
	//@since 2.1
	protected static void logSQL(String hardStr) {
		Logger.logSQL(hardStr);
	}
	
	protected static final String INDEX1 = "_SYS[index";
	protected static final String INDEX2 = "]_End ";
	
	protected void clearContext(String sql_0, int batchSize, int len) {
		
		for (int i = 0; i < len; i++) {
			String sql_i = INDEX1 + i + INDEX2 +shardingIndex()+ sql_0; //fixed bug V2.2
			clearContext(sql_i);
		}
	}
	
	//检测是否有Json注解
	protected boolean isJoson(Field field) {
		return AnnoUtil.isJson(field);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object jsonHandlerProcess(Field field, Object obj, TypeHandler jsonHandler) {
		if (List.class.isAssignableFrom(field.getType())) {
			Object newObj[] = new Object[2];
			newObj[0] = obj;
			newObj[1] = field;
			obj = jsonHandler.process(field.getType(), newObj);
		} else {
			obj = jsonHandler.process(field.getType(), obj);
		}
		return obj;
	}
	
	@SuppressWarnings("rawtypes")
	protected Object createObject(Class c) throws IllegalAccessException,InstantiationException{
		return c.newInstance();
	}
	
	protected String shardingIndex() {
		Integer subThreadIndex = HoneyContext.getSqlIndexLocal();
		String index = "";
		if (subThreadIndex != null) {
			index = " (sharding " + subThreadIndex + ")";
		}
		return index;
	}
	
	protected boolean getShowSQL() {
		return HoneyConfig.getHoneyConfig().showSQL;
	}
	
	protected boolean getShowShardingSQL() {
		return showSQL && HoneyConfig.getHoneyConfig().showShardingSQL;
	}
	
	//JDBC,主要是SQLException
	protected boolean isConstraint(Exception e) {
		if (e == null) return false;
		String className = e.getClass().getSimpleName();
		String fullClassName = e.getClass().getName();
		
		boolean f = "MySQLIntegrityConstraintViolationException".equals(className) // mysql
				|| (e instanceof SQLIntegrityConstraintViolationException)
				|| (e instanceof BatchUpdateException) // PostgreSQL,...
				|| "org.h2.jdbc.JdbcBatchUpdateException".equals(fullClassName) // h2
		;
		if (f) return true;
		if (e.getMessage() == null) return false;
		return ( e.getMessage().startsWith("Duplicate entry ") //mysql   
				|| e.getMessage().contains("ORA-00001:")    //Oracle 
				|| e.getMessage().contains("duplicate key") || e.getMessage().contains("DUPLICATE KEY")  //PostgreSQL
				|| e.getMessage().contains("primary key violation")  //h2
				|| e.getMessage().contains("SQLITE_CONSTRAINT_PRIMARYKEY") || e.getMessage().contains("PRIMARY KEY constraint") //SQLite
				|| e.getMessage().contains("Duplicate entry")|| e.getMessage().contains("Duplicate Entry")
				|| e.getMessage().contains("Duplicate key") || e.getMessage().contains("Duplicate Key")
				);
	}

}
