package org.honey.osql.core;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author KingStar
 * @since  1.0
 */
public final class HoneyContext {

	private static ConcurrentMap<String, String> beanMap;

	private static ThreadLocal<Map<String, List<PreparedValue>>> sqlLocal;
	private static ThreadLocal<Map<String, String>> sqlValueLocal;

	private static ThreadLocal<Connection> currentConnection;  //当前事务

	static {
		beanMap = new ConcurrentHashMap<>();
		sqlLocal = new ThreadLocal<>();
		sqlValueLocal = new ThreadLocal<>();

		currentConnection = new ThreadLocal<>();
	}

	private HoneyContext() {}

	public static String addBeanField(String key, String value) {
		return beanMap.put(key, value);
	}

	public static String getBeanField(String key) {
		return beanMap.get(key);
	}

	public static void setPreparedValue(String sqlStr, List<PreparedValue> list) {
		if (list == null || list.size() == 0) return;
		Map<String, List<PreparedValue>> map = sqlLocal.get();
		if (null == map) map = new HashMap<>();
		map.put(sqlStr, list);//TODO 覆盖??
		sqlLocal.set(map);
	}

	public static List<PreparedValue> getPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlLocal.get();
		if (null == map) return null;

		List<PreparedValue> list = map.get(sqlStr);
		if (list != null) map.remove(sqlStr);
		return list;
	}

	public static void setSqlValue(String sqlStr, String value) {
		if (value == null || "".equals(value.trim())) return;
		Map<String, String> map = sqlValueLocal.get();
		if (null == map) map = new HashMap<>();
		map.put(sqlStr, value); //TODO 覆盖??
		sqlValueLocal.set(map);
	}

	public static String getSqlValue(String sqlStr) {
		Map<String, String> map = sqlValueLocal.get();
		if (null == map) return null;

		String s = map.get(sqlStr);
		if (s != null) map.remove(sqlStr);
		return s;
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
