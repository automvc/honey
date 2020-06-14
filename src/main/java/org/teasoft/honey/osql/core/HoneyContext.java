package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyContext {

	private static ConcurrentMap<String, String> beanMap;
//	since v1.7.0
	private static ConcurrentMap<String, MoreTableStruct[]> moreTableStructMap;

	private static ThreadLocal<Map<String, List<PreparedValue>>> sqlPreValueLocal;
	private static ThreadLocal<Map<String, String>> sqlValueLocal;
	
	private static ThreadLocal<Map<String, CacheSuidStruct>> cacheLocal;
	
	

	private static ThreadLocal<Connection> currentConnection;  //当前事务的
	
	private static ConcurrentMap<String,String> entity2table;
	private static ConcurrentMap<String,String> table2entity=null; //for creat Javabean (just one to one can work well)

	static {
		beanMap = new ConcurrentHashMap<>();
		moreTableStructMap= new ConcurrentHashMap<>();
		
		sqlPreValueLocal = new ThreadLocal<>();
		sqlValueLocal = new ThreadLocal<>();
		cacheLocal = new ThreadLocal<>();

		currentConnection = new ThreadLocal<>();
		
		entity2table=new ConcurrentHashMap<>();
//		table2entity=new ConcurrentHashMap<>();
		initEntity2Table();
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
		String entity2tableMappingList=HoneyConfig.getHoneyConfig().getEntity2tableMappingList();
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
		String entity2tableMappingList=HoneyConfig.getHoneyConfig().getEntity2tableMappingList();
		if(entity2tableMappingList!=null){
			String entity2table_array[]=entity2tableMappingList.split(",");
			String item[];
			for (int i = 0; i < entity2table_array.length; i++) {
				item=entity2table_array[i].trim().split(":");  //User2:temp_user,com.abc.user.User:temp_user
				if(item.length!=2){
					Logger.error("["+entity2table_array[i].trim()+"] wrong formatter,separate option is not colon(:). (in bee.properties file, key: bee.osql.name.mapping.entity2table)");
				}else{
//					entity2table.put(item[0].trim(), item[1].trim());
					
					if(table2entity.containsKey(item[1].trim())){ //check
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

    static void setSqlValue(String sqlStr, String value) {
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
	}
	
	
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

}
