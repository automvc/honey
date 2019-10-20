package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.teasoft.honey.osql.cache.CacheSuidStruct;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyContext {

	private static ConcurrentMap<String, String> beanMap;

	private static ThreadLocal<Map<String, List<PreparedValue>>> sqlPreValueLocal;
	private static ThreadLocal<Map<String, String>> sqlValueLocal;
	
	private static ThreadLocal<Map<String, CacheSuidStruct>> cacheLocal;

	private static ThreadLocal<Connection> currentConnection;  //当前事务

	static {
		beanMap = new ConcurrentHashMap<>();
		sqlPreValueLocal = new ThreadLocal<>();
		sqlValueLocal = new ThreadLocal<>();
		cacheLocal = new ThreadLocal<>();

		currentConnection = new ThreadLocal<>();
	}

	private HoneyContext() {}

	static String addBeanField(String key, String value) {
		return beanMap.put(key, value);
	}

	public static String getBeanField(String key) {
		return beanMap.get(key);
	}

	static void setPreparedValue(String sqlStr, List<PreparedValue> list) {
		if (list == null || list.size() == 0) return;
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (null == map) map = new HashMap<>();
		map.put(sqlStr, list);
		sqlPreValueLocal.set(map);
	}

	public static List<PreparedValue> getPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (null == map) return null;

		List<PreparedValue> list = map.get(sqlStr);
		if (list != null) map.remove(sqlStr);
		return list;
	}

    static void setSqlValue(String sqlStr, String value) {
		if (value == null || "".equals(value.trim())) return;
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
	}
	
	
	static void setCacheInfo(String sqlStr, CacheSuidStruct cacheInfo) {
		if (cacheInfo == null) return;
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		if (null == map) map = new HashMap<>();
		map.put(sqlStr, cacheInfo); 
		cacheLocal.set(map);
	}

	public static CacheSuidStruct getCacheInfo(String sqlStr,boolean isDelete) {
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		if (null == map) return null;
		CacheSuidStruct struct=map.get(sqlStr);
		if (isDelete && struct != null) map.remove(sqlStr);  
		
//		Map<String, CacheSuidStruct> map2 = cacheLocal.get();
//		CacheSuidStruct struct2=map2.get(sqlStr);
//		YES  已被删
//		if(struct2==null)  System.out.println("--------  CacheSuidStruct  was already  deleted!"); 
		
		return  struct;
	}
	
	static void deleteCacheInfo(String sqlStr){
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		map.remove(sqlStr);
	}
	
	public static CacheSuidStruct getCacheInfo(String sqlStr) {
		return getCacheInfo(sqlStr,false);
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

}
