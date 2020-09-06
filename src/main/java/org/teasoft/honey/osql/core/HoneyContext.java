package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.honey.distribution.ds.RouteStruct;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyContext {

	private static ConcurrentMap<String, String> beanMap;
//	since v1.7.0
	private static ConcurrentMap<String, MoreTableStruct[]> moreTableStructMap;

	private static ThreadLocal<Map<String, List<PreparedValue>>> sqlPreValueLocal;
//	private static ThreadLocal<Map<String, String>> sqlValueLocal;
	
	private static ThreadLocal<Map<String, CacheSuidStruct>> cacheLocal;
	
//	private static ThreadLocal<Map<String, RouteStruct>> routeLocal;
	private static ThreadLocal<RouteStruct> currentRoute; 
	

	private static ThreadLocal<Connection> currentConnection;  //当前事务的
	
	private static ConcurrentMap<String,String> entity2table;
	private static ConcurrentMap<String,String> table2entity=null; //for creat Javabean (just one to one can work well)
	
	private static Map<String, String> entityList_includes_Map = new ConcurrentHashMap<>();
	private static Map<String, String> entityList_excludes_Map = new ConcurrentHashMap<>();
	
	private static List<String> entityListWithStar_in = new CopyOnWriteArrayList<>();
	private static List<String> entityListWithStar_ex = new CopyOnWriteArrayList<>();
	
/*	private static void _checkSize(ThreadLocal local,String name){
		if(local==null)
			System.err.println("==============="+name+"  is null");
		else
			System.err.println("==============="+name+"  size is : "+ local.get());
		
	}
	
	public static void checkSize(){
		System.err.println("==============checkSize============");
		_checkSize(sqlPreValueLocal,"sqlPreValueLocal");
		_checkSize(cacheLocal,"cacheLocal");
		_checkSize(currentConnection,"currentConnection");
		_checkSize(currentRoute,"currentRoute");
	}*/
	
	static {
		beanMap = new ConcurrentHashMap<>();
		moreTableStructMap= new ConcurrentHashMap<>();
		
		sqlPreValueLocal = new ThreadLocal<>();
//		sqlValueLocal = new ThreadLocal<>();
		cacheLocal = new ThreadLocal<>();

		currentConnection = new ThreadLocal<>();
		currentRoute = new ThreadLocal<>();
		
		entity2table=new ConcurrentHashMap<>();
//		table2entity=new ConcurrentHashMap<>();
		initEntity2Table();
		
		parseEntityListToMap();
	}

	private HoneyContext() {}
	
	static ConcurrentMap<String,String> getEntity2tableMap(){
		return entity2table;
	}
	
	static ConcurrentMap<String,String> getTable2entityMap(){ //just create the Javabean files would use
		if(table2entity==null){
			table2entity=new ConcurrentHashMap<>();
			initTable2Entity();
		}
		
		return table2entity;
	}
	
	
	
	private static void initEntity2Table(){
		String entity2tableMappingList=HoneyConfig.getHoneyConfig().entity2tableMappingList;
		if(entity2tableMappingList!=null){
			String entity2table_array[]=entity2tableMappingList.split(",");
			String item[];
			for (int i = 0; i < entity2table_array.length; i++) {
				item=entity2table_array[i].trim().split(":");  //User2:temp_user,com.abc.user.User:temp_user
				if(item.length!=2){
					Logger.error("["+entity2table_array[i].trim()+"] wrong formatter,separate option is not colon(:). (in bee.properties file, key: bee.osql.name.mapping.entity2table)");
				}else{
					entity2table.put(item[0].trim(), item[1].trim());
					
//					if(table2entity.containsKey(item[1].trim())){ //check
//						Logger.warn(table2entity.get(item[1].trim()) +" and "+ item[0].trim() +" mapping same table: "+item[1].trim());
//					}
//					table2entity.put(item[1].trim(), item[0].trim());
				}
			}
		}
	}
	
	private static void initTable2Entity(){
		String entity2tableMappingList=HoneyConfig.getHoneyConfig().entity2tableMappingList;
		if(entity2tableMappingList!=null){
			String entity2table_array[]=entity2tableMappingList.split(",");
			String item[];
			for (int i = 0; i < entity2table_array.length; i++) {
				item=entity2table_array[i].trim().split(":");  //User2:temp_user,com.abc.user.User:temp_user
				if(item.length!=2){
					Logger.error("["+entity2table_array[i].trim()+"] wrong formatter,separate option is not colon(:). (in bee.properties file, key: bee.osql.name.mapping.entity2table)");
				}else{
//					entity2table.put(item[0].trim(), item[1].trim());
					
					if(table2entity.containsKey(item[1].trim())){ //check   只是生成javabean时会用到,SqlLib不会用到.因会传入T entity   所以不会引起混淆
						Logger.warn(table2entity.get(item[1].trim()) +" and "+ item[0].trim() +" mapping same table: "+item[1].trim());
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
		return beanMap.get(key);
	}
	
	static MoreTableStruct[] addMoreTableStructs(String key, MoreTableStruct[] value) {
		return moreTableStructMap.put(key, value);
	}

	public static MoreTableStruct[] getMoreTableStructs(String key) {
		return moreTableStructMap.get(key);
	}

	static void setPreparedValue(String sqlStr, List<PreparedValue> list) {
		if (list == null || list.size() == 0) return;
		if(sqlStr==null || "".equals(sqlStr.trim())) return;
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (null == map) map = new HashMap<>();
		map.put(sqlStr, list);
		sqlPreValueLocal.set(map);
	}
	
  static List<PreparedValue> _justGetPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (null == map) return null;

		List<PreparedValue> list = map.get(sqlStr);
//		if (list != null) map.remove(sqlStr);   don't delete
		return list;
	}

	public static List<PreparedValue> getPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (null == map) return null;

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
		if(sqlStr==null || "".equals(sqlStr.trim())) return;
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		if (null == map) map = new HashMap<>();
		map.put(sqlStr, cacheInfo); 
		cacheLocal.set(map);
	}

	public static CacheSuidStruct getCacheInfo(String sqlStr) {
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		if (null == map) return null;
		CacheSuidStruct struct=map.get(sqlStr);
		return  struct;
	}
	
	static void deleteCacheInfo(String sqlStr){
		Map<String, CacheSuidStruct> map = cacheLocal.get();
//		map.remove(sqlStr); //bug
		if(map!=null) map.remove(sqlStr); 
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
	
//	static void setRouteInfo(String sqlStr, RouteStruct routeStruct) {
//		if (routeStruct == null) return;
//		if(sqlStr==null || "".equals(sqlStr.trim())) return;
//		Map<String, RouteStruct> map = routeLocal.get();
//		if (null == map) map = new HashMap<>();  //TODO 
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
		 RouteStruct routeStruct=currentRoute.get();
//		 currentRoute.remove();  //需要多次,不能拿了就删
		 return routeStruct;
	}

	public static void setCurrentRoute(RouteStruct routeStruct) {
		currentRoute.set(routeStruct);
	}
	
	public static void removeCurrentRoute() {
		currentRoute.remove();
	}
	
	static void setContext(String sql,List<PreparedValue> list,String tableName){
		HoneyContext.setPreparedValue(sql, list);
		addInContextForCache(sql, tableName);
	}
	
    static void addInContextForCache(String sql, String tableName){
		CacheSuidStruct struct=new CacheSuidStruct();
		struct.setSql(sql);
		struct.setTableNames(tableName);
		HoneyContext.setCacheInfo(sql, struct);
	}
    
	static void regEntityClass(Class clazz) {
		OneTimeParameter.setAttribute("_SYS_Bee_ROUTE_EC", clazz); //EC:Entity Class
	}
	
	static Connection getConn() throws SQLException {
		Connection conn = null;

		conn = HoneyContext.getCurrentConnection(); //获取已开启事务的连接
		if (conn == null) {
			conn = SessionFactory.getConnection(); //不开启事务时
		}

		return conn;
	}
	
	static void checkClose(Statement stmt, Connection conn) {

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				//				e.printStackTrace();
				throw ExceptionHelper.convert(e);
			}
		}
		try {
			if (conn != null && conn.getAutoCommit()) {//自动提交时才关闭.如果开启事务,则由事务负责
				conn.close();
			}
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			boolean enableMultiDs = HoneyConfig.getHoneyConfig().enableMultiDs;
			int multiDsType = HoneyConfig.getHoneyConfig().multiDsType;
			if (enableMultiDs && multiDsType == 2) {//仅分库,有多个数据源时
				HoneyContext.removeCurrentRoute();
			}
		}
	}
	
	//for SqlLib
	static boolean updateInfoInCache(String sql, String returnType, SuidType suidType) {
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
			struct.setReturnType(returnType);
			struct.setSuidType(suidType.getType());
			HoneyContext.setCacheInfo(sql, struct);
			return true;
		}
		//要是没有更新缓存,证明之前还没有登记过缓存,就不能去查缓存.
		return false;
	}
	
	//for SqlLib
	static void initRoute(SuidType suidType, Class clazz,String sql) {

		if (clazz == null) {
			clazz = (Class) OneTimeParameter.getAttribute("_SYS_Bee_ROUTE_EC");
		}

		RouteStruct routeStruct = new RouteStruct();
		routeStruct.setSuidType(suidType);
		routeStruct.setEntityClass(clazz);
		
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
			routeStruct.setTableNames(struct.getTableNames());
		}

		HoneyContext.setCurrentRoute(routeStruct);
	}
	
//	private static Map<String, String> entityList_includes_Map = new ConcurrentHashMap<>();
//	private static Map<String, String> entityList_excludes_Map = new ConcurrentHashMap<>();
	
	private static void parseEntityListToMap() {
		String entityList_includes = HoneyConfig.getHoneyConfig().entityList_includes; //in
		_parseListToMap(entityList_includes, entityList_includes_Map, entityListWithStar_in);

		String entityList_excludes = HoneyConfig.getHoneyConfig().entityList_excludes; //ex
		_parseListToMap(entityList_excludes, entityList_excludes_Map, entityListWithStar_ex);

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

	private static boolean isConfigForEntityIN(Class clazz) {
		return _isConfig(clazz, entityList_includes_Map, entityListWithStar_in);
	}

	private static boolean isConfigForEntityEX(Class clazz) {
		return _isConfig(clazz, entityList_excludes_Map, entityListWithStar_ex);
	}

	private static boolean _isConfig(Class clazz, Map<String, String> map, List<String> starList) {

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
	
}
