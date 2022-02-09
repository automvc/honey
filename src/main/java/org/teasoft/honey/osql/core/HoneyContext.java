package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.honey.distribution.ds.RouteStruct;
import org.teasoft.honey.util.ObjectUtils;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyContext {

	private static ConcurrentMap<String, String> beanMap;
	
	//since 1.11
	private static ConcurrentMap<String, String> beanCustomPKey; //Custom Primary Key
	
	//	since v1.7.0
	//	private static ConcurrentMap<String, MoreTableStruct[]> moreTableStructMap;

	private static ThreadLocal<Map<String, List<PreparedValue>>> sqlPreValueLocal;
	//	private static ThreadLocal<Map<String, String>> sqlValueLocal;

	private static ThreadLocal<Map<String, CacheSuidStruct>> cacheLocal;

	private static ThreadLocal<RouteStruct> currentRoute;

	private static ThreadLocal<Connection> currentConnection; //当前事务的Conn

	private static ThreadLocal<String> sameConnctionDoing; //当前多个ORM操作使用同一个connection.
	
	private static ThreadLocal<String> appointDS; 

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
		//		moreTableStructMap= new ConcurrentHashMap<>();

		sqlPreValueLocal = new ThreadLocal<>();
		//		sqlValueLocal = new ThreadLocal<>();
		cacheLocal = new ThreadLocal<>();

		currentConnection = new ThreadLocal<>();
		//		transactionLocal = new ThreadLocal<>();
		sameConnctionDoing = new ThreadLocal<>();
		appointDS = new ThreadLocal<>();

		currentRoute = new ThreadLocal<>();

		entity2table = new ConcurrentHashMap<>();
		//		table2entity=new ConcurrentHashMap<>();
		initEntity2Table();

		parseEntityListToMap();
		
		modifiedFlagMapForCache2 = new ConcurrentHashMap<>();
	}

	private HoneyContext() {}

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
	
	static String addBeanField(String key, String value) {
		return beanMap.put(key, value);
	}

	public static String getBeanField(String key) {
		if(key==null) key="";
		return beanMap.get(key);
	}

	static void clearFieldNameCache() {
		beanMap.clear();
	}
	
	static String addBeanCustomPKey(String key, String value) {
		return beanCustomPKey.put(key, value);
	}
	
	public static String getBeanCustomPKey(String key) {
		if(key==null) key=null;
		return beanCustomPKey.get(key);
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

		List<PreparedValue> list = map.get(sqlStr);
		return list;
	}

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
		CacheSuidStruct struct = map.get(sqlStr);
		return struct;
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

	private static String getSameConnctionDoing() {
		return sameConnctionDoing.get();
	}

	private static void setSameConnctionDoing() {
		sameConnctionDoing.set(StringConst.tRue);
	}

	private static void removeSameConnctionDoing() {
		sameConnctionDoing.remove();
	}
	
	
	public static String getAppointDS() {
		System.out.println("------------------getAppointDS");
		return appointDS.get();
	}

	public static void setAppointDS(String dsName) {
		System.out.println("------------------setAppointDS");
		appointDS.set(dsName);
	}

	public static void removeAppointDS() {
		System.out.println("------------------removeAppointDS");
		appointDS.remove();
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
		}else {
			
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
		RouteStruct routeStruct = currentRoute.get();
		//		 currentRoute.remove();  //需要多次,不能拿了就删
		return routeStruct;
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
		OneTimeParameter.setAttribute("_SYS_Bee_ROUTE_EC", clazz); //EC:Entity Class
	}

	static Connection getConn() throws SQLException {
		Connection conn = null;

		conn = getCurrentConnection(); //获取已开启事务或同一Connection的连接
		if (conn == null) {
			conn = SessionFactory.getConnection(); //不开启事务时

			//如果设置了同一Connection
			if (OneTimeParameter.isTrue("_SYS_Bee_SAME_CONN_BEGIN")) {
				setCurrentConnection(conn); //存入上下文
				setSameConnctionDoing();
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
			boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
			boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
//			if (enableMultiDs && multiDsType == 2) {//仅分库,有多个数据源时
			if (enableMultiDs && (multiDsType ==2 || (multiDsType ==1 && differentDbType) )) {//仅分库,有多个数据源时
				removeCurrentRoute();
			}
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
			boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
			boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
//			if (enableMultiDs && multiDsType == 2) {//仅分库,有多个数据源时
			if (enableMultiDs && (multiDsType ==2 || (multiDsType ==1 && differentDbType) )) {
				removeCurrentRoute();
			}
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
			clazz = (Class) OneTimeParameter.getAttribute("_SYS_Bee_ROUTE_EC");
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

	@SuppressWarnings("rawtypes")
	static void initRouteWhenParseSql(SuidType suidType, Class clazz, String tableNames) {

		if (clazz == null) {
			clazz = (Class) OneTimeParameter.getAttribute("_SYS_Bee_ROUTE_EC");
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

	//仅分库,有多个数据源时,且支持同时使用多种类型数据库时,
	//才可能需要实时确认是什么数据库,没有分页的不需要
	static boolean isNeedRealTimeDb() {
		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
		if (enableMultiDs) {
			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
			boolean supportDifferentDbType = HoneyConfig.getHoneyConfig().multiDS_differentDbType;
//			if (multiDsType == 2 && supportDifferentDbType) {//仅分库,有多个数据源时,且支持同时使用多种类型数据库时
			if ((multiDsType ==2 || multiDsType == 1) && supportDifferentDbType) {  //不同数据库才要实时获取数据库类型
				return true;
			}
		}

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

}
