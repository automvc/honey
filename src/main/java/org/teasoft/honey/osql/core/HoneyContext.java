package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.spi.PreLoad;
import org.teasoft.honey.distribution.ds.RouteStruct;
import org.teasoft.honey.util.ObjectUtils;

/**
 * Bee框架上下文.Context for Bee.
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyContext {

	private static ConcurrentMap<String, String> beanMap;
	
	//since 1.11
	private static ConcurrentMap<String, String> beanCustomPKey; //Custom Primary Key
	private static ConcurrentMap<String, Map<String, String>> customMap;
	
	private static ConcurrentMap<String, String> sysCommStr;
	
	
	//	since v1.7.0
	//	private static ConcurrentMap<String, MoreTableStruct[]> moreTableStructMap;

	private static ThreadLocal<Map<String, List<PreparedValue>>> sqlPreValueLocal;
	//	private static ThreadLocal<Map<String, String>> sqlValueLocal;

	private static ThreadLocal<Map<String, CacheSuidStruct>> cacheLocal;
	
	private static ThreadLocal<Map<String, Map<String, String>>> customMapLocal;
	
	private static ThreadLocal<Map<String,String>> sysCommStrLocal;

	private static ThreadLocal<RouteStruct> currentRoute;

	private static ThreadLocal<Connection> currentConnection; //当前事务的Conn
	
	private static ThreadLocal<NameTranslate> currentNameTranslate;

	private static ThreadLocal<String> sameConnctionDoing; //当前多个ORM操作使用同一个connection.
	private static ThreadLocal<String> jdbcTranWriterDs; 
	
	private static ThreadLocal<String> appointDS; 
	private static ThreadLocal<String> tempDS;  //for Suid.setDataSourceName(String dsName) and so on
	
	private static ThreadLocal<String> appointTab; 
	private static ThreadLocal<String> tabSuffix; 
	
	private static ThreadLocal<String> tempLang; 
	
	private static String lang; 

	//	private static ThreadLocal<Transaction> transactionLocal;  

	private static ConcurrentMap<String, String> entity2table;
//	private static volatile ConcurrentMap<String, String> table2entity = null; //for creat Javabean (just one to one can work well)
//	private static final byte lock[] = new byte[0];
	private static ConcurrentMap<String, String> table2entity = null; //for creat Javabean (just one to one can work well)
	
	private static Map<String, String> entityList_includes_Map = new ConcurrentHashMap<>();
	private static Map<String, String> entityList_excludes_Map = new ConcurrentHashMap<>();

	private static List<String> entityListWithStar_in = new CopyOnWriteArrayList<>();
	private static List<String> entityListWithStar_ex = new CopyOnWriteArrayList<>();
	
	//V1.11
	private static Map<String, String> entityList_levelTwo_Map = new ConcurrentHashMap<>();
	private static List<String> entityListWithStar_levelTwo = new CopyOnWriteArrayList<>();

	private static Map<String, String> dsName2DbName;
	
	private static ConcurrentMap<String, Boolean> modifiedFlagMapForCache2;
	
	private static ConcurrentMap<String, Boolean> entityInterceptorFlag;
	
	private static ConcurrentMap<String, Boolean> customFlagMap;

	/*	private static void _checkSize(ThreadLocal local,String name){
			if(local==null)
				err.println("==============="+name+"  is null");
			else
				err.println("==============="+name+"  size is : "+ local.get());
			
		}
		
		public static void checkSize(){
			err.println("==============checkSize============");
			_checkSize(sqlPreValueLocal,"sqlPreValueLocal");
			_checkSize(cacheLocal,"cacheLocal");
			_checkSize(currentConnection,"currentConnection");
			_checkSize(currentRoute,"currentRoute");
		}*/

	static {
		beanMap = new ConcurrentHashMap<>();
		beanCustomPKey = new ConcurrentHashMap<>();
		sysCommStr = new ConcurrentHashMap<>();
		customMap = new ConcurrentHashMap<>();
		//		moreTableStructMap= new ConcurrentHashMap<>();

		sqlPreValueLocal = new ThreadLocal<>();
		//		sqlValueLocal = new ThreadLocal<>();
		cacheLocal = new ThreadLocal<>();
		customMapLocal = new ThreadLocal<>();
		sysCommStrLocal = new ThreadLocal<>();

		currentConnection = new ThreadLocal<>();
		currentNameTranslate = new ThreadLocal<>();
		//		transactionLocal = new ThreadLocal<>();
		
		sameConnctionDoing = new ThreadLocal<>();
		jdbcTranWriterDs = new ThreadLocal<>();
		appointDS = new ThreadLocal<>();
		tempDS = new ThreadLocal<>();
		appointTab = new ThreadLocal<>();
		tabSuffix = new ThreadLocal<>();
		
		tempLang = new ThreadLocal<>();

		currentRoute = new ThreadLocal<>();

		entity2table = new ConcurrentHashMap<>();
		//		table2entity=new ConcurrentHashMap<>();
		initEntity2Table();

		parseEntityListToMap();
		
		modifiedFlagMapForCache2 = new ConcurrentHashMap<>();
		entityInterceptorFlag = new ConcurrentHashMap<>();
		customFlagMap = new ConcurrentHashMap<>();
	
		initLoad();
	}

	private HoneyContext() {}

	static void initLoad() {
		BeeInit.initLoad();
	}

	static ConcurrentMap<String, String> getEntity2tableMap() {
		return entity2table;
	}

//	static ConcurrentMap<String, String> getTable2entityMap() { //just create the Javabean files would use
//		if (table2entity == null) {
////			synchronized (HoneyContext.class) {
//			synchronized (lock) {
//				if (table2entity == null) {
//					table2entity = new ConcurrentHashMap<>();
//					initTable2Entity();
//				}
//			}
//		}
//		return table2entity;
//	}
	
	synchronized static ConcurrentMap<String, String> getTable2entityMap() { //just create the Javabean files would use
		if (table2entity == null) {
			table2entity = new ConcurrentHashMap<>();
			initTable2Entity();
		}
		return table2entity;
	}

	private static void initEntity2Table() {
		String entity2tableMappingList = HoneyConfig.getHoneyConfig().naming_entity2tableMappingList;
		if (entity2tableMappingList != null) {
			String entity2table_array[] = entity2tableMappingList.split(",");
			String item[];
			for (int i = 0; i < entity2table_array.length; i++) {
				item = entity2table_array[i].trim().split(":"); //User2:temp_user,com.abc.user.User:temp_user
				if (item.length != 2) {
					Logger.error("[" + entity2table_array[i].trim()
							+ "] wrong formatter,separate option is not colon(:). (in bee.properties file, key: bee.osql.name.mapping.entity2table)");
				} else {
					entity2table.put(item[0].trim(), item[1].trim());

					//					if(table2entity.containsKey(item[1].trim())){ //check
					//						Logger.warn(table2entity.get(item[1].trim()) +" and "+ item[0].trim() +" mapping same table: "+item[1].trim());
					//					}
					//					table2entity.put(item[1].trim(), item[0].trim());
				}
			}
		}
	}

	private static void initTable2Entity() {
		String entity2tableMappingList = HoneyConfig.getHoneyConfig().naming_entity2tableMappingList;
		if (entity2tableMappingList != null) {
			String entity2table_array[] = entity2tableMappingList.split(",");
			String item[];
			for (int i = 0; i < entity2table_array.length; i++) {
				item = entity2table_array[i].trim().split(":"); //User2:temp_user,com.abc.user.User:temp_user
				if (item.length != 2) {
					Logger.error("[" + entity2table_array[i].trim()
							+ "] wrong formatter,separate option is not colon(:). (in bee.properties file, key: bee.osql.name.mapping.entity2table)");
				} else {
					//					entity2table.put(item[0].trim(), item[1].trim());

					if (table2entity.containsKey(item[1].trim())) { //check   只是生成javabean时会用到,SqlLib不会用到.因会传入T entity   所以不会引起混淆
						Logger.warn(table2entity.get(item[1].trim()) + " and " + item[0].trim() + " mapping same table: "
								+ item[1].trim());
					}
					table2entity.put(item[1].trim(), item[0].trim());
				}
			}
		}
	}
	
	static void addBeanField(String key, String value) {
		beanMap.put(key, value);
	}

	public static String getBeanField(String key) {
		if (key == null) key = "";
		return beanMap.get(key);
	}

	static void clearFieldNameCache() {
		beanMap.clear();
	}

	static void addBeanCustomPKey(String key, String value) {
		beanCustomPKey.put(key, value);
	}

	public static String getBeanCustomPKey(String key) {
		if (key == null) return null;
		return beanCustomPKey.get(key);
	}
	
	static void addSysCommStr(String key, String value) {
		sysCommStr.put(key, value);
	}

	public static String getSysCommStr(String key) {
		if (key == null) return null;
		return sysCommStr.get(key);
	}

	public static void addCustomMap(String key, Map<String, String> mapValue) {
		customMap.put(key, mapValue);
	}

	public static Map<String, String> getCustomMap(String key) {
		if (key == null) return null;
		return customMap.get(key);
	}
	
	public static String getCustomMapValue(String key1,String key2) {
		if (key1 == null || key2==null) return null;
		Map<String, String> map= customMap.get(key1);
		if(map!=null) return map.get(key2);
		
		return null;
	}
	
	public static void setCustomMapLocal(String key, Map<String, String> mapValue) {
		if (mapValue == null) return;
		if (key == null || "".equals(key.trim())) return;
		Map<String, Map<String, String>> map = customMapLocal.get();
		if (null == map) map = new ConcurrentHashMap<>();
		map.put(key, mapValue);
		customMapLocal.set(map);
	}

	public static Map<String,String> getCustomMapLocal(String key) {
		Map<String, Map<String, String>> map = customMapLocal.get();
		if (null == map || key==null) return null;
		return map.get(key);
	}
	
	static void setSysCommStrLocal(String key, String sysCommStr) {
		if (sysCommStr == null) return;
		if (key == null || "".equals(key.trim())) return;
		Map<String, String> map = sysCommStrLocal.get();
		if (null == map) map = new ConcurrentHashMap<>();
		map.put(key, sysCommStr);
		sysCommStrLocal.set(map);
	}

	public static String getSysCommStrLocal(String key) {
		Map<String, String> map = sysCommStrLocal.get();
		if (map == null || key == null) return null;
		return map.get(key);
	}

	public static void removeSysCommStrLocal(String key) {
		Map<String, String> map = sysCommStrLocal.get();
		if (map != null) map.remove(key);
	}

	//	static MoreTableStruct[] addMoreTableStructs(String key, MoreTableStruct[] value) {
	//		return moreTableStructMap.put(key, value);
	//	}
	//
	//	public static MoreTableStruct[] getMoreTableStructs(String key) {
	//		return moreTableStructMap.get(key);
	//	}
	
	private static boolean isShowExecutableSql() {
		return HoneyConfig.getHoneyConfig().showSql_showExecutableSql;
	}
	
	static void setPreparedValue(String sqlStr, List<PreparedValue> list) {
		if (list == null ) return;
		if (sqlStr == null || "".equals(sqlStr.trim())) return;
		if (list.size() == 0) {
			if (!isShowExecutableSql()) return;
		}
		
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		//		if (null == map) map = new HashMap<>();
		if (null == map) map = new ConcurrentHashMap<>();
		map.put(sqlStr, list);
		sqlPreValueLocal.set(map);
	}

	static List<PreparedValue> justGetPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (null == map || sqlStr==null) return null;

		return map.get(sqlStr);
	}
//	public static void test() {
//		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
//		System.err.println(map);
//	}

	static void clearPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (null == map || sqlStr==null) return;
		if (map.get(sqlStr) != null) map.remove(sqlStr);
	}

	static List<PreparedValue> getAndClearPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (null == map || sqlStr==null) return null;
		List<PreparedValue> list = map.get(sqlStr);
		if (list != null) map.remove(sqlStr);

		return list;
	}

	/*    static void setSqlValue(String sqlStr, String value) {
			if (value == null || "".equals(value.trim())) return;
			if(sqlStr==null || "".equals(sqlStr.trim())) return;
			Map<String, String> map = sqlValueLocal.get();
			if (null == map) map = new HashMap<>();
			map.put(sqlStr, value); 
			sqlValueLocal.set(map);
		}
	
		public static String getSqlValue(String sqlStr) {
			Map<String, String> map = sqlValueLocal.get();
			if (null == map) return null;
			String s = map.get(sqlStr);
			if (s != null) map.remove(sqlStr);
			return s;
		}*/

	static void setCacheInfo(String sqlStr, CacheSuidStruct cacheInfo) {
		if (cacheInfo == null) return;
		if (sqlStr == null || "".equals(sqlStr.trim())) return;
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		if (null == map) map = new ConcurrentHashMap<>();
		map.put(sqlStr, cacheInfo);
		cacheLocal.set(map);
	}

	public static CacheSuidStruct getCacheInfo(String sqlStr) {
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		if (null == map || sqlStr==null) return null;
		return map.get(sqlStr);
	}

	static void deleteCacheInfo(String sqlStr) {
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		//		map.remove(sqlStr); //bug
		if (map != null) map.remove(sqlStr);
	}

	public static String getDbDialect() {
		return HoneyConfig.getHoneyConfig().getDbName();
	}

	public static Connection getCurrentConnection() {
		return currentConnection.get();
	}

	public static void setCurrentConnection(Connection conn) {
		currentConnection.set(conn);
	}

	public static void removeCurrentConnection() {
		currentConnection.remove();
	}
	
	public static NameTranslate getCurrentNameTranslate() {
		return currentNameTranslate.get();
	}

	public static void setCurrentNameTranslate(NameTranslate nameTranslate) {
		currentNameTranslate.set(nameTranslate);
	}

	public static void removeCurrentNameTranslate() {
		currentNameTranslate.remove();
	}

	public static String getSameConnctionDoing() {
		return sameConnctionDoing.get();
	}

	private static void setSameConnctionDoing() {
		sameConnctionDoing.set(StringConst.tRue);
	}

	private static void removeSameConnctionDoing() {
		sameConnctionDoing.remove();
	}
	
	public static String getJdbcTranWriterDs() {
		String s= jdbcTranWriterDs.get();
		jdbcTranWriterDs.remove();
		return s;
	}

	public static void setJdbcTranWriterDs() {
		jdbcTranWriterDs.set(StringConst.tRue);
	}
	
	public static String getAppointDS() {
		return appointDS.get();
	}

	public static void setAppointDS(String dsName) {
		if (isMultiDs()) appointDS.set(dsName);
	}

	public static void removeAppointDS() {
		if (isMultiDs()) appointDS.remove();
	}
	
	public static String getTempDS() {
		return tempDS.get();
	}

	static void setTempDS(String dsName) {
		if (isMultiDs()) tempDS.set(dsName);
	}

	static void removeTempDS() {
		if (isMultiDs()) tempDS.remove();
	}
	
	private static boolean isMultiDs() {
		return HoneyConfig.getHoneyConfig().multiDS_enable;
	}
	
	
	public static String getAppointTab() {
		return appointTab.get();
	}

	public static void setAppointTab(String tabName) {
		appointTab.set(tabName);
	}

	public static void removeAppointTab() {
		appointTab.remove();
	}

	public static String getTabSuffix() {
		return tabSuffix.get();
	}

	public static void setTabSuffix(String suffix) {
		tabSuffix.set(suffix);
	}

	public static void removeTabSuffix() {
		tabSuffix.remove();
	}
	
	public static String getTempLang() {
		return tempLang.get();
	}

	public static void setTempLang(String dsName) {
		if (isMultiDs()) tempLang.set(dsName);
	}

	public static void removeTempLang() {
		if (isMultiDs()) tempLang.remove();
	}

	public static String getLang() {

		String lang0 = getTempLang();
		if (lang0 != null) return lang0;

		return lang;
	}

	public static void setLang(String lang) {
		HoneyContext.lang = lang;
	}

	static void endSameConnection() {
		
		if (OneTimeParameter.isTrue("_SYS_Bee_SAME_CONN_BEGIN")) { //all get from cache.
			Logger.warn("Do not get the new Connection in the SameConnection.Maybe all the results get from cache! ");
		} else if (!StringConst.tRue.equals(getSameConnctionDoing())) {
			if (OneTimeParameter.isTrue("_SYS_Bee_SAME_CONN_EXCEPTION")) {//exception,   //异常时,会删除上下文连接 
//				next select will get every conn like normal case.
//				若报异常后到调用endSameConnection()之时, 1)没有新获取连接,则直接到这个方法;  不用特别处理
//				2)有新的连接,用完后,正常关闭,到这里,也是这个提示.
				Logger.warn("Do not use same Connection, because have exception in between the begin and end SameConnection !");
			} else { //miss beginSameConnection          
				Logger.warn("Calling the endSameConnection(), but miss the beginSameConnection()");
			}
		}else if (StringConst.tRue.equals(getSameConnctionDoing())) { // 正常流程
			OneTimeParameter.setTrueForKey("_SYS_Bee_SAME_CONN_END");
			checkClose(null, getCurrentConnection());
		}
		removeCurrentConnection();
	}

	//	public static Transaction getCurrentTransaction() {
	//		return transactionLocal.get();
	//	}
	//
	//	public static void setCurrentTransaction(Transaction transaction) {
	//		transactionLocal.set(transaction);
	//	}

	//	static void setRouteInfo(String sqlStr, RouteStruct routeStruct) {
	//		if (routeStruct == null) return;
	//		if(sqlStr==null || "".equals(sqlStr.trim())) return;
	//		Map<String, RouteStruct> map = routeLocal.get();
	//		if (null == map) map = new HashMap<>();  //
	//		map.put(sqlStr, routeStruct); 
	//		routeLocal.set(map);
	//	}
	//
	//	public static RouteStruct getRouteInfo(String sqlStr) {
	//		Map<String, RouteStruct> map = routeLocal.get();
	//		if (null == map) return null;
	//		RouteStruct struct=map.get(sqlStr);
	//		return  struct;
	//	}

	public static RouteStruct getCurrentRoute() {
		return currentRoute.get();
	}

	public static void setCurrentRoute(RouteStruct routeStruct) {
		currentRoute.set(routeStruct);
	}

	public static void removeCurrentRoute() {
		currentRoute.remove();
	}

	static void setContext(String sql, List<PreparedValue> list, String tableName) {
		setPreparedValue(sql, list);
		addInContextForCache(sql, tableName);
	}

	static void addInContextForCache(String sql, String tableName) {
		CacheSuidStruct struct = new CacheSuidStruct();
		struct.setSql(sql);
		struct.setTableNames(tableName);
		setCacheInfo(sql, struct);
	}

	@SuppressWarnings("rawtypes")
	static void regEntityClass(Class clazz) {
		OneTimeParameter.setAttribute(StringConst.Route_EC, clazz); //EC:Entity Class
	}

	static Connection getConn() throws SQLException {
		Connection conn = null;

		conn = getCurrentConnection(); //获取已开启事务或同一Connection的连接
		
		if (conn == null) {
			boolean isSameConn=OneTimeParameter.isTrue("_SYS_Bee_SAME_CONN_BEGIN");
			if (isSameConn) {
				setSameConnctionDoing(); //提前设置,因RW时,同一连接要改为默认走写库
			}
			
			conn = SessionFactory.getConnection(); 

			//如果设置了同一Connection
			if (isSameConn) {
				setCurrentConnection(conn); //存入上下文
			}
		}

		return conn;
	}
	
	//For exception case. when have exception, must close the conn.
	static void closeConn(Connection conn) {
		try {
//			if (conn != null) conn.close();  //bug   can not be closed before transation rollback
			if (conn != null && conn.getAutoCommit()) conn.close();
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			removeCurrentConnection(); //事务结束时要删除;在事务中间报异常也要删除;同一conn也要删除
			if (StringConst.tRue.equals(getSameConnctionDoing())) {
				removeSameConnctionDoing(); //同一conn
				OneTimeParameter.setTrueForKey("_SYS_Bee_SAME_CONN_EXCEPTION");
			}
//			boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
//			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
//			boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
////			if (enableMultiDs && multiDsType == 2) {//仅分库,有多个数据源时
//			if (enableMultiDs && (multiDsType ==2 || (multiDsType ==1 && differentDbType) )) {//仅分库,有多个数据源时
			
//			if (isNeedDs()) {
//			if(enableMultiDs) { //放到拦截器中
//				removeCurrentRoute();
//			}
		}
	}

	public static void checkClose(Statement stmt, Connection conn) {
		checkClose(null, stmt, conn);
	}

	public static void checkClose(ResultSet rs, Statement stmt, Connection conn) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				//ignore
			}
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				throw ExceptionHelper.convert(e);
			}
		}
		if(conn!=null) {
		try {
			//			如果设置了同一Connection
			//			并且调用了endSameConnection才关闭   
			if (StringConst.tRue.equals(getSameConnctionDoing())) {
				if (OneTimeParameter.isTrue("_SYS_Bee_SAME_CONN_END")) { // 调用suid.endSameConnection();前的SQL操作 不会触发这里的.
					removeSameConnctionDoing();
					if (conn != null) conn.close();
				}
				//else { do not close}
			} else {
				if (conn != null && conn.getAutoCommit()) {//自动提交时才关闭.如果开启事务,则由事务负责
					conn.close();
				}
			}
		} catch (SQLException e) {
			Logger.debug(e.getMessage());
			throw ExceptionHelper.convert(e);
		} finally {
//			boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
//			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
//			boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
////			if (enableMultiDs && multiDsType == 2) {//仅分库,有多个数据源时
//			if (enableMultiDs && (multiDsType ==2 || (multiDsType ==1 && differentDbType) )) {
			
//			if (isNeedDs()) {
//			if(enableMultiDs) {  //放到拦截器中
//				removeCurrentRoute();    //RW不用清???? 
//			}
		}
		}
	}

	//for SqlLib
	static boolean updateInfoInCache(String sql, String returnType, SuidType suidType) {
		CacheSuidStruct struct = getCacheInfo(sql);
		if (struct != null) {
			struct.setReturnType(returnType);
			struct.setSuidType(suidType.getType());
			setCacheInfo(sql, struct);
			return true;
		}
		//要是没有更新缓存,证明之前还没有登记过缓存,就不能去查缓存.
		return false;
	}

	//for SqlLib
	@SuppressWarnings("rawtypes")
	static void initRoute(SuidType suidType, Class clazz, String sql) {

		if (clazz == null) {
			clazz = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		}

		RouteStruct routeStruct = new RouteStruct();
		routeStruct.setSuidType(suidType);
		routeStruct.setEntityClass(clazz);

		CacheSuidStruct struct = getCacheInfo(sql);
		if (struct != null) {
			routeStruct.setTableNames(struct.getTableNames());
		}

		setCurrentRoute(routeStruct);
	}

	//因同时使用不同类型DB,需要实时确认DB,而需要路由
	@SuppressWarnings("rawtypes")
	static void initRouteWhenParseSql(SuidType suidType, Class clazz, String tableNames) {

		if (clazz == null) {
			clazz = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		}

		RouteStruct routeStruct = new RouteStruct();
		routeStruct.setSuidType(suidType);
		routeStruct.setEntityClass(clazz);
		routeStruct.setTableNames(tableNames);

		setCurrentRoute(routeStruct);
	}

	private static void parseEntityListToMap() {
		String entityList_includes = HoneyConfig.getHoneyConfig().genid_includesEntityList; //in
		_parseListToMap(entityList_includes, entityList_includes_Map, entityListWithStar_in);

		String entityList_excludes = HoneyConfig.getHoneyConfig().genid_excludesEntityList; //ex
		_parseListToMap(entityList_excludes, entityList_excludes_Map, entityListWithStar_ex);
		
		String levelTwoEntityList=HoneyConfig.getHoneyConfig().cache_levelTwoEntityList;  //cache level 2
		_parseListToMap(levelTwoEntityList,entityList_levelTwo_Map,entityListWithStar_levelTwo);

	}

	private static void _parseListToMap(String str, Map<String, String> map, List<String> starList) {
		//com.xxx.aa.User,com.xxx.bb.*,com.xxx.cc.**
		if (str == null || "".equals(str.trim())) return;

		String strArray[] = str.trim().split(",");
		for (int k = 0; k < strArray.length; k++) {
			if (strArray[k].trim().endsWith(".**")) starList.add(strArray[k].trim()); // .** 结尾同时存一份到list
			map.put(strArray[k].trim(), "1");
		}
	}

	@SuppressWarnings("rawtypes")
	private static boolean isConfigForEntityIN(Class clazz) {
		return _isConfig(clazz, entityList_includes_Map, entityListWithStar_in);
	}

	@SuppressWarnings("rawtypes")
	private static boolean isConfigForEntityEX(Class clazz) {
		return _isConfig(clazz, entityList_excludes_Map, entityListWithStar_ex);
	}

	@SuppressWarnings("rawtypes")
	private static boolean _isConfig(Class clazz, Map<String, String> map, List<String> starList) {
		
		if(clazz==null) return false;

		String fullName = clazz.getName();
		String ds = null;
		ds = map.get(fullName); //1
		if (ds != null) return true;

		if (clazz.getPackage() != null) {
			String packageName = clazz.getPackage().getName();
			ds = map.get(packageName + ".*"); //2
			if (ds != null) return true;

			//ds=entityClassPathToDs.get(packageName+".**");   //com.xxx.** 省略多级情况下,不适用

			for (int i = 0; i < starList.size(); i++) {
				String s = starList.get(i);
				if (s.endsWith(".**")) {
					String prePath = s.substring(0, s.length() - 2);
					if (fullName.startsWith(prePath)) return true; //3
				}
			}
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	public static boolean isNeedGenId(Class clazz) {
		boolean needGenId = false;
		boolean genAll = HoneyConfig.getHoneyConfig().genid_forAllTableLongId;
		if (genAll) {
			if (isConfigForEntityEX(clazz))
				needGenId = false; //有排除,则  不生成的
			else
				needGenId = true;
		} else {
			if (isConfigForEntityEX(clazz))
				needGenId = false; //有排除,则  不生成的
			else {
				if (isConfigForEntityIN(clazz))
					needGenId = true;
				else
					needGenId = false;
			}
		}

		return needGenId;
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean isConfigLevelTwoCache(Class clazz) {
		return _isConfig(clazz, entityList_levelTwo_Map, entityListWithStar_levelTwo);
	}

//closeed	//仅分库,有多个数据源时,且支持同时使用多种类型数据库时,
//closeed	//才可能需要实时确认是什么数据库,没有分页的不需要
	
//  是多数据源,有同时使用多种不同类型DB
	static boolean isNeedRealTimeDb() {
//		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
//		if (enableMultiDs) {
////			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
//			boolean isDifferentDbType = HoneyConfig.getHoneyConfig().multiDS_differentDbType;
////			if (multiDsType == 2 && isDifferentDbType) {//仅分库,有多个数据源时,且支持同时使用多种类型数据库时
////			if ((multiDsType ==2 || multiDsType == 1) && isDifferentDbType) {  //不同数据库才要实时获取数据库类型
//			if(isDifferentDbType) {
//			  return true;
//			}
//		}

//closeed	//是多数据源, 又不是同种DB类型的只读模式, 则需要动态获取DB类型
//		if (isNeedDs())
//			return true;
//		else
//			return false;
		
//      是多数据源,有同时使用多种不同类型DB
		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
		boolean isDifferentDbType = HoneyConfig.getHoneyConfig().multiDS_differentDbType;
		if (enableMultiDs && isDifferentDbType)
			return true;
		else
			return false;
	}

	//同时使用多种类型数据库时,才会触发.   没有分页时,走原来的流程,到SqlLib,才获取数据源处理Suid操作.
	static String getRealTimeDbName() {
		String dbName = null;
		if (isNeedRealTimeDb()) {
			return HoneyConfig.getHoneyConfig().getDbName();
		}
		return dbName;
	}
	
	public static boolean isNeedDs() {
		if (isMultiDs()) {
			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
			boolean differentDbType = HoneyConfig.getHoneyConfig().multiDS_differentDbType;
			if (!(multiDsType == 1 && !differentDbType)) // 不是(模式1的同种DB) //sameDbType=!differentDbType
				return true;
			else
				return false;
		} else {
			return false;
		}
	}

	static boolean isAlreadySetRoute() {
		return OneTimeParameter.isTrue(StringConst.ALREADY_SET_ROUTE);
	}

	public static Map<String, String> getDsName2DbName() {
		return dsName2DbName;
	}

	public static void setDsName2DbName(Map<String, String> dsName2DbName) {
		HoneyContext.dsName2DbName = dsName2DbName;
	}

	private static boolean configRefresh = false;

	public static boolean isConfigRefresh() {
		return configRefresh;
	}

	public static void setConfigRefresh(boolean configRefresh) {
		HoneyContext.configRefresh = configRefresh;
	}

	public static void updateConfig(Map<String, Object> map) {

		if (ObjectUtils.isEmpty(map)) return;
		Object obj = HoneyConfig.getHoneyConfig();
		Class clazz = obj.getClass();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			try {

				Field field = clazz.getDeclaredField(entry.getKey());
				field.setAccessible(true);
				field.set(obj, entry.getValue());

			} catch (Exception e) {
				throw ExceptionHelper.convert(e);
			}
		}
		
		setConfigRefresh(true);
	}

	public static boolean getModifiedFlagForCache2(String tableName) {
		Boolean f=modifiedFlagMapForCache2.get(tableName);
		return f==null?false:f;
	}

	public static void addModifiedFlagForCache2(String tableName,boolean isModified) {
		modifiedFlagMapForCache2.put(tableName, isModified);
	}
	
	public static Boolean getEntityInterceptorFlag(String fullClassName) {
		return entityInterceptorFlag.get(fullClassName);
	}
	
	public static void addEntityInterceptorFlag(String fullClassName,boolean isHas) {
		entityInterceptorFlag.put(fullClassName, isHas);
	}

	
	public static Boolean getCustomFlagMap(String key) {
		return customFlagMap.get(key);
	}
	
	public static void addCustomFlagMap(String key,boolean flag) {
		customFlagMap.put(key, flag);
	}
	
}
