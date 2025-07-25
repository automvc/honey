package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.dialect.DbFeatureRegistry;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.bee.osql.exception.ShardingErrorException;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.bee.sharding.ShardingPageStruct;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.bee.sharding.algorithm.CalculateRegistry;
import org.teasoft.honey.database.DatabaseClientConnection;
import org.teasoft.honey.distribution.ds.RouteStruct;
import org.teasoft.honey.distribution.ds.Router;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.dialect.LimitOffsetPaging;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerPagingStruct;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.algorithm.DateCalculate;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * Bee框架上下文.Context for Bee.
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyContext {

	private static ConcurrentMap<String, String> beanMap;

	// since 1.11
	private static ConcurrentMap<String, String> beanCustomPKey; // Custom Primary Key
	private static ConcurrentMap<String, Map<String, String>> customMap;

	private static ThreadLocal<Map<String, List<PreparedValue>>> sqlPreValueLocal;
	private static ThreadLocal<Integer> sqlIndexLocal; // 标识子操作线程的下标, 也说明是子线程
	private static ThreadLocal<Condition> conditionLocal;
//	private static ThreadLocal<List<String>> tabNameListLocal; 

	private static ThreadLocal<Map<String, CacheSuidStruct>> cacheLocal;
	private static ThreadLocal<Map<String, SqlServerPagingStruct>> sqlServerPaging;

	private static ThreadLocal<Map<String, Map<String, String>>> customMapLocal;

	private static ThreadLocal<Map<String, String>> sysCommStrLocal;
	private static ThreadLocal<Map<String, String>> sysCommStrInheritableLocal;
	private static ThreadLocal<Map<String, List<String>>> listLocal;

	private static ThreadLocal<RouteStruct> currentRoute;
	private static ThreadLocal<ShardingPageStruct> currentShardingPage;
	private static ThreadLocal<ShardingSortStruct> currentShardingSort;
	private static ThreadLocal<GroupFunStruct> currentGroupFunStruct;

	private static ThreadLocal<Connection> currentConnection; // 当前事务的Conn

//	private static ThreadLocal<List<Connection>> conneForSelectRs;
	private static ConcurrentMap<String, Connection> conneForSelectRs;

	private static ThreadLocal<Object> currentAppDB; // V1.17

	private static ThreadLocal<NameTranslate> currentNameTranslate;

	private static ThreadLocal<String> sameConnectionDoing; // 当前多个ORM操作使用同一个connection.
	private static ThreadLocal<String> jdbcTranWriterDs;

	private static ThreadLocal<String> appointDS; // 1.拦截器等设置的值; 因处理不同的javabean对象,可能有不同的值;比Suid对象的tempDS,动态性更强.所以appointDS优于tempDS
	private static ThreadLocal<String> tempDS; // 2.对象级别设置的值; for Suid.setDataSourceName(String dsName) and so on

	private static ThreadLocal<String> appointTab;
	private static ThreadLocal<String> tabSuffix;

	private static ThreadLocal<String> tempLang;

	private static String lang;

	private static ConcurrentMap<String, String> entity2table;
	private static ConcurrentMap<String, String> table2entity = null; // for creat Javabean (just one to one can work well)

	private static Map<String, String> entityList_includes_Map = new ConcurrentHashMap<>();
	private static Map<String, String> entityList_excludes_Map = new ConcurrentHashMap<>();

	private static List<String> entityListWithStar_in = new CopyOnWriteArrayList<>();
	private static List<String> entityListWithStar_ex = new CopyOnWriteArrayList<>();

	// V1.11
	private static Map<String, String> entityList_levelTwo_Map = new ConcurrentHashMap<>();
	private static List<String> entityListWithStar_levelTwo = new CopyOnWriteArrayList<>();

	private static Map<String, String> dsName2DbName;

	private static ConcurrentMap<String, Boolean> modifiedFlagMapForCache2;

	private static ConcurrentMap<String, Boolean> entityInterceptorFlag;

	private static ConcurrentMap<String, Boolean> customFlagMap;

	static {
		HoneyConfig.getHoneyConfig(); // V2.1.8 与config相互引用时,这句不一定保险 V2.5.2 HoneyConfig不再引用Context

		beanMap = new ConcurrentHashMap<>();
		beanCustomPKey = new ConcurrentHashMap<>();
		customMap = new ConcurrentHashMap<>();

		if (cacheLocal != null) cacheLocal.remove();
		if (sqlServerPaging != null) sqlServerPaging.remove();
		if (customMapLocal != null) customMapLocal.remove();
		if (sysCommStrLocal != null) sysCommStrLocal.remove();
		if (sysCommStrInheritableLocal != null) sysCommStrInheritableLocal.remove();
		if (listLocal != null) listLocal.remove();
		if (sqlPreValueLocal != null) sqlPreValueLocal.remove();
		if (sqlIndexLocal != null) sqlIndexLocal.remove();
		if (conditionLocal != null) conditionLocal.remove();

		initTL();

		sqlIndexLocal = new InheritableThreadLocal<>();
		conditionLocal = new InheritableThreadLocal<>();
//		tabNameListLocal = new ThreadLocal<>(); //每个子线程都有一个具体表名,不需要.
		sysCommStrInheritableLocal = new InheritableThreadLocal<>();

		sqlServerPaging = new ThreadLocal<>();
		customMapLocal = new ThreadLocal<>();
		listLocal = new ThreadLocal<>(); // 子线程没有用到.

		currentConnection = new ThreadLocal<>();

//		conneForSelectRs = new InheritableThreadLocal<>();
//		synchronized (HoneyContext.class) {
//			conneForSelectRs = new InheritableThreadLocal<>();
//			conneForSelectRs.set(new CopyOnWriteArrayList<Connection>()); // 一开始就要设值,在主线程才能处理子线程加入的元素
//		}
		conneForSelectRs = new ConcurrentHashMap<>();

		currentAppDB = new ThreadLocal<>();
		currentNameTranslate = new ThreadLocal<>();

		sameConnectionDoing = new ThreadLocal<>();
		jdbcTranWriterDs = new ThreadLocal<>();
		tempDS = new ThreadLocal<>();
		appointTab = new ThreadLocal<>();
		tabSuffix = new ThreadLocal<>();

		tempLang = new ThreadLocal<>();

		currentShardingPage = new ThreadLocal<>();
		currentShardingSort = new InheritableThreadLocal<>();
		currentGroupFunStruct = new InheritableThreadLocal<>();

		entity2table = new ConcurrentHashMap<>();
		initEntity2Table();

		parseEntityListToMap();

		modifiedFlagMapForCache2 = new ConcurrentHashMap<>();
		entityInterceptorFlag = new ConcurrentHashMap<>();
		customFlagMap = new ConcurrentHashMap<>();
		
		init();

		initLoad();
	}

	private HoneyContext() {}

	static void initLoad() {
		BeeInitPreLoadService.initLoad();
	}

	static void initTL() {
		if (HoneyConfig.getHoneyConfig().multiDS_sharding) { // 当使用分片模式,某些表又没有实行分片时,不宜使用parallelStream()并行操作
			sqlPreValueLocal = new InheritableThreadLocal<>();

			currentRoute = new InheritableThreadLocal<>();
			cacheLocal = new InheritableThreadLocal<>();
			sysCommStrLocal = new InheritableThreadLocal<>();
			appointDS = new InheritableThreadLocal<>();
		} else {// 2.2
//			https://blog.csdn.net/abckingaa/article/details/135408582
			sqlPreValueLocal = new ThreadLocal<>(); // 2.2 fixed bug: parallelStream().map + InheritableThreadLocal 才会出现 No value
													// specified for parameter 1

			currentRoute = new ThreadLocal<>();
			cacheLocal = new ThreadLocal<>();
			sysCommStrLocal = new ThreadLocal<>();
			appointDS = new ThreadLocal<>();
		}
	}

	public static void initAfterChangeMultiDsSharding() { // initTL 默认时,走else; 当在代码设置了multiDS_sharding,才要刷新.
//		if (HoneyConfig.getHoneyConfig().multiDS_sharding) { //当使用分片模式,某些表又没有实行分片时,不宜使用parallelStream()并行操作
//			//因parallelStream()+InheritableThreadLocal会有问题,但分片又必要要用InheritableThreadLocal,所以分片时,不支持parallelStream并行操作
//			sqlPreValueLocal = new InheritableThreadLocal<>();
//			
//			currentRoute = new InheritableThreadLocal<>();
//			cacheLocal = new InheritableThreadLocal<>();
//			sysCommStrLocal = new InheritableThreadLocal<>();
//			appointDS = new InheritableThreadLocal<>();
//		}

		initTL();
	}

	static ConcurrentMap<String, String> getEntity2tableMap() {
		return entity2table;
	}

	synchronized static ConcurrentMap<String, String> getTable2entityMap() { // just create the Javabean files would use
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
				item = entity2table_array[i].trim().split(":"); // User2:temp_user,com.abc.user.User:temp_user
				if (item.length != 2) {
					Logger.warn("[" + entity2table_array[i].trim()
							+ "] wrong formatter,separate option is not colon(:). (in bee.properties file, key: bee.osql.name.mapping.entity2table)");
				} else {
					entity2table.put(item[0].trim(), item[1].trim());

					// if(table2entity.containsKey(item[1].trim())){ //check
					// Logger.warn(table2entity.get(item[1].trim()) +" and "+ item[0].trim() +" mapping same table:
					// "+item[1].trim());
					// }
					// table2entity.put(item[1].trim(), item[0].trim());
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
				item = entity2table_array[i].trim().split(":"); // User2:temp_user,com.abc.user.User:temp_user
				if (item.length != 2) {
					Logger.warn("[" + entity2table_array[i].trim()
							+ "] wrong formatter,separate option is not colon(:). (in bee.properties file, key: bee.osql.name.mapping.entity2table)");
				} else {
					// entity2table.put(item[0].trim(), item[1].trim());

					if (table2entity.containsKey(item[1].trim())) { // check 只是生成javabean时会用到,SqlLib不会用到.因会传入T entity 所以不会引起混淆
						Logger.warn(table2entity.get(item[1].trim()) + " and " + item[0].trim() + " mapping same table: "
								+ item[1].trim());
					}
					table2entity.put(item[1].trim(), item[0].trim());
				}
			}
		}
	}

	static void addBeanField(String key, String value) {
		if (key == null) return;
		if (HoneyConfig.getHoneyConfig().naming_useMoreTranslateType) {
			key += NameTranslateHandle.getNameTranslate().getClass().getName();
		}
		beanMap.put(key, value);
	}

	public static String getBeanField(String key) {
		if (key == null) key = "";
		if (HoneyConfig.getHoneyConfig().naming_useMoreTranslateType) {
			key += NameTranslateHandle.getNameTranslate().getClass().getName();
		}
		return beanMap.get(key);
	}

	static void clearFieldNameCache() {
		beanMap.clear();
	}

	static void addBeanCustomPKey(String key, String value) {
		if (key == null) return;
		if (HoneyConfig.getHoneyConfig().naming_useMoreTranslateType) {
			key += NameTranslateHandle.getNameTranslate().getClass().getName();
		}
		beanCustomPKey.put(key, value);
	}

	public static String getBeanCustomPKey(String mapKey) {
		if (mapKey == null) return null;
		if (HoneyConfig.getHoneyConfig().naming_useMoreTranslateType) {
			mapKey += NameTranslateHandle.getNameTranslate().getClass().getName();
		}
		return beanCustomPKey.get(mapKey);
	}

	public static void addCustomMap(String key, Map<String, String> mapValue) {
		if (key == null) {
			Logger.warn("Do not support the null key!", new BeeIllegalParameterException("Do not support the null key!"));
			return;
		}
		customMap.put(key, mapValue);
	}

	public static Map<String, String> getCustomMap(String key) {
		if (key == null) return null;
		return customMap.get(key);
	}

	public static String getCustomMapValue(String key1, String key2) {
		if (key1 == null || key2 == null) return null;
		Map<String, String> map = customMap.get(key1);
		if (map != null) return map.get(key2);

		return null;
	}

	public static void removeCustomMap(String key) {
		if (customMap.containsKey(key)) customMap.remove(key);
	}

	public static void setCustomMapLocal(String key, Map<String, String> mapValue) {
		if (mapValue == null) return;
		if (key == null || "".equals(key.trim())) return;
		Map<String, Map<String, String>> map = customMapLocal.get();
		if (map == null) map = new ConcurrentHashMap<>();
		map.put(key, mapValue);
		customMapLocal.set(map);
	}

	public static Map<String, String> getCustomMapLocal(String key) {
		Map<String, Map<String, String>> map = customMapLocal.get();
		if (map == null || key == null) return null;
		return map.get(key);
	}

	public static void removeCustomMapLocal(String key) {
		Map<String, Map<String, String>> map = customMapLocal.get();
		if (map == null || key == null) return;
		if (map.containsKey(key)) map.remove(key);
	}

	public static void setSysCommStrLocal(String key, String sysCommStr) {
		if (sysCommStr == null) return;
		if (key == null || "".equals(key.trim())) return;
		Map<String, String> map = sysCommStrLocal.get();
		if (map == null) map = new ConcurrentHashMap<>();
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

	public static void setSysCommStrInheritableLocal(String key, String value) {
		if (value == null) return;
		if (key == null || "".equals(key.trim())) return;
		Map<String, String> map = sysCommStrInheritableLocal.get();
		if (map == null) map = new ConcurrentHashMap<>();
		map.put(key, value);
		sysCommStrInheritableLocal.set(map);
	}

	public static String getSysCommStrInheritableLocal(String key) {
		Map<String, String> map = sysCommStrInheritableLocal.get();
		if (map == null || key == null) return null;
		return map.get(key);
	}

	public static void removeSysCommStrInheritableLocal(String key) {
		Map<String, String> map = sysCommStrInheritableLocal.get();
		if (map != null) map.remove(key);
	}

	public static void setTrueInSysCommStrLocal(String key) {
		setSysCommStrLocal(key, StringConst.tRue);
	}

	public static boolean isTrueInSysCommStrLocal(String key) {
		return StringConst.tRue.equals(HoneyContext.getSysCommStrLocal(key));
	}

	public static void setTrueInSysCommStrInheritableLocal(String key) {
		setSysCommStrInheritableLocal(key, StringConst.tRue);
	}

	public static boolean isTrueInSysCommStrInheritableLocal(String key) {
		return StringConst.tRue.equals(HoneyContext.getSysCommStrInheritableLocal(key));
	}

	public static void setListLocal(String key, List<String> listString) {
		if (listString == null) return;
		if (key == null || "".equals(key.trim())) return;
		Map<String, List<String>> map = listLocal.get();
		if (map == null) map = new ConcurrentHashMap<>();
		map.put(key, listString);
		listLocal.set(map);
	}

	public static List<String> getListLocal(String key) {
		Map<String, List<String>> map = listLocal.get();
		if (map == null || key == null) return null;
		return map.get(key);
	}

	public static void removeListLocal(String key) {
		Map<String, List<String>> map = listLocal.get();
		if (map != null) map.remove(key);
	}

	static void setPreparedValue(String sqlStr, List<PreparedValue> list) {
		if (list == null) return;
		if (sqlStr == null || "".equals(sqlStr.trim())) return;
		if (list.size() == 0) {
//			if (!isShowExecutableSql()) return;  //2.4.0  为了让chaning模式也能用占位符供preparedSql调用,参数为空时,不再放入上下文. 
			return;
		}

		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (map == null) map = new ConcurrentHashMap<>();
		map.put(sqlStr, list);
		sqlPreValueLocal.set(map);
	}

	static List<PreparedValue> justGetPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (map == null || sqlStr == null) return null;

		return map.get(sqlStr);
	}

	static void clearPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (map == null || sqlStr == null) return;

		if (map.containsKey(sqlStr)) map.remove(sqlStr);
	}

	static List<PreparedValue> getAndClearPreparedValue(String sqlStr) {
		Map<String, List<PreparedValue>> map = sqlPreValueLocal.get();
		if (map == null || sqlStr == null) return null;

		List<PreparedValue> list = map.get(sqlStr);
		if (map.containsKey(sqlStr)) map.remove(sqlStr);
		return list;
	}

	static void setCacheInfo(String sqlStr, CacheSuidStruct cacheInfo) {
		if (cacheInfo == null) return;
		if (sqlStr == null || "".equals(sqlStr.trim())) return;
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		if (map == null) map = new ConcurrentHashMap<>();
		map.put(sqlStr, cacheInfo);
		cacheLocal.set(map);
	}

	public static CacheSuidStruct getCacheInfo(String sqlStr) {
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		if (map == null || sqlStr == null) return null;
		return map.get(sqlStr);
	}

	static void deleteCacheInfo(String sqlStr) {
		Map<String, CacheSuidStruct> map = cacheLocal.get();
		// map.remove(sqlStr); //bug
		if (map != null) map.remove(sqlStr);
	}

	public static void setSqlServerPagingStruct(String sqlStr, SqlServerPagingStruct sqlServerPagingStruct) {
		if (sqlServerPagingStruct == null) return;
		if (sqlStr == null || "".equals(sqlStr.trim())) return;
		Map<String, SqlServerPagingStruct> map = sqlServerPaging.get();
		if (map == null) map = new ConcurrentHashMap<>();
		map.put(sqlStr, sqlServerPagingStruct);
		sqlServerPaging.set(map);
	}

	public static SqlServerPagingStruct getAndRemoveSqlServerPagingStruct(String sqlStr) {
		Map<String, SqlServerPagingStruct> map = sqlServerPaging.get();
		if (map == null || sqlStr == null) return null;
		SqlServerPagingStruct struct = map.get(sqlStr);
//		if (struct != null) map.remove(sqlStr);
		if (map.containsKey(sqlStr)) map.remove(sqlStr);
		return struct;
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

	public synchronized static void regConnForSelectRs(Connection conn) {
		String threadFlag = HoneyContext.getSysCommStrInheritableLocal(StringConst.ShardingSelectRs_ThreadFlag);
		Integer subThreadIndex = HoneyContext.getSqlIndexLocal();
		String key = threadFlag + subThreadIndex;
		conneForSelectRs.put(key, conn);
	}

	public synchronized static void clearConnForSelectRs(String key) {
		Connection conn = conneForSelectRs.get(key);
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				Logger.debug(e.getMessage(), e);
			} finally {
				conneForSelectRs.remove(key);
			}
		}
	}

	// V1.17
	public static Object getCurrentAppDB() {
		return currentAppDB.get();
	}

	public static void setCurrentAppDB(Object appDB) {
		if (isAppDBObject(appDB.getClass().getName())) {
			currentAppDB.set(appDB);
		}
	}

	public static void setCurrentAppDBIfNeed(Object appDB) {
		if (isAppDBObject(appDB.getClass().getName())) {
			if (OneTimeParameter.isTrue(StringConst.SAME_CONN_BEGIN)) {
				currentAppDB.set(appDB);
			}
		}
	}

	public static void removeCurrentAppDB() {
		currentAppDB.remove();
	}

	private static boolean isAppDBObject(String className) {
		return "android.database.sqlite.SQLiteDatabase".equals(className) || "ohos.data.rdb.RdbStore".equals(className);
	}

	public static NameTranslate getCurrentNameTranslate() {
		return currentNameTranslate.get();
	}

	/**
	 * 设置只一次有效.AbstractCommOperate在执行SUID操作返回前会清除.
	 * 若整个应用只有一个自定义的NameTranslate,可以使用
	 * NameRegistry.registerNameTranslate(nameTranslate)进行设置.
	 * @param nameTranslate
	 */
//	public static void setCurrentNameTranslate(NameTranslate nameTranslate) { //closed on V2.4.0
	static void setCurrentNameTranslateOneTime(NameTranslate nameTranslate) {
		HoneyContext.clearFieldNameCache();
		currentNameTranslate.set(nameTranslate);
	}

	public static void removeCurrentNameTranslate() {
		currentNameTranslate.remove();
	}

	public static String getSameConnectionDoing() {
		return sameConnectionDoing.get();
	}

	private static void setSameConnectionDoing() {
		sameConnectionDoing.set(StringConst.tRue);
	}

	private static void removesameConnectionDoing() {
		sameConnectionDoing.remove();
	}

	public static String getJdbcTranWriterDs() {
		String s = jdbcTranWriterDs.get();
		jdbcTranWriterDs.remove();
		return s;
	}

	public static void setJdbcTranWriterDs() {
		jdbcTranWriterDs.set(StringConst.tRue);
	}

	public static String getAppointDS() {
		return appointDS.get();
	}

	// 拦截器里获取的, 而拦截器则是从@MultiTenancy等获取到.
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

	public static boolean isMultiDs() {
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

	public static Integer getSqlIndexLocal() {
		return sqlIndexLocal.get();
	}

	public static void setSqlIndexLocal(int index) {
		sqlIndexLocal.set(index);
	}

	public static void removeSqlIndexLocal() {
		sqlIndexLocal.remove();
	}

	public static Condition getConditionLocal() {
		return conditionLocal.get();
	}

	public static void setConditionLocal(Condition condition) {
		if (condition != null)
			conditionLocal.set(condition.clone());
		else
			conditionLocal.set(condition);
	}

	public static void removeConditionLocal() {
		conditionLocal.remove();
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
		// V1..17 for Android
		if (HoneyConfig.getHoneyConfig().isAndroid || HoneyConfig.getHoneyConfig().isHarmony) { // Harmony只是删除上下文保存的,但是否关闭不在这负责
			if (OneTimeParameter.isTrue(StringConst.SAME_CONN_BEGIN)) { // all get from cache. 设置标志后,都是从缓存获取. 所以没有消费这个标识
				Logger.warn(
						"Do not get the new Connection in the SameConnection. Maybe all the results get from cache! ");
			}
			HoneyContext.removeCurrentAppDB(); // 同一连接结束时要删除上下文

			return;
		}

		if (OneTimeParameter.isTrue(StringConst.SAME_CONN_BEGIN)) { // all get from cache.
			Logger.warn("Do not get the new Connection in the SameConnection. Maybe all the results get from cache! ");
		} else if (!StringConst.tRue.equals(getSameConnectionDoing())) {
			if (OneTimeParameter.isTrue(StringConst.SAME_CONN_EXCEPTION)) {// exception, //异常时,会删除上下文连接
//				next select will get every conn like normal case.
//				若报异常后到调用endSameConnection()之时, 1)没有新获取连接,则直接到这个方法;  不用特别处理
//				2)有新的连接,用完后,正常关闭,到这里,也是这个提示.
				Logger.warn(
						"Do not use same Connection, because have exception in between the begin and end SameConnection !");
			} else { // miss beginSameConnection
				Logger.warn("Calling the endSameConnection(), but miss the beginSameConnection()");
			}
		} else if (StringConst.tRue.equals(getSameConnectionDoing())) { // 正常流程
			OneTimeParameter.setTrueForKey(StringConst.SAME_CONN_END);
			checkClose(null, getCurrentConnection());
		}
		removeCurrentConnection();
	}

	public static RouteStruct getCurrentRoute() {
		return currentRoute.get();
	}

	public static void setCurrentRoute(RouteStruct routeStruct) {
		currentRoute.set(routeStruct);
	}

	public static void removeCurrentRoute() {
		currentRoute.remove();
	}

	public static ShardingPageStruct getCurrentShardingPage() { // select * from table; 没有分页时,要注意
		return currentShardingPage.get();
	}

	public static void setCurrentShardingPage(ShardingPageStruct shardingPage) {
		currentShardingPage.set(shardingPage);
	}

	public static void removeCurrentShardingPage() {
		currentShardingPage.remove();
	}

	public static ShardingSortStruct getCurrentShardingSort() {
		return currentShardingSort.get();
	}

	public static void setCurrentShardingSort(ShardingSortStruct shardingSort) {
		currentShardingSort.set(shardingSort);
	}

	public static void removeCurrentShardingSort() {
		currentShardingSort.remove();
	}

	public static GroupFunStruct getCurrentGroupFunStruct() {
		return currentGroupFunStruct.get();
	}

	public static void setCurrentGroupFunStruct(GroupFunStruct groupFunStruct) {
		currentGroupFunStruct.set(groupFunStruct);
	}

	public static void removeCurrentGroupFunStruct() {
		currentGroupFunStruct.remove();
	}

	static void setContext(String sql, List<PreparedValue> list, String tableName) {
		setPreparedValue(sql, list);
		addInContextForCache(sql, tableName); // 若子句是在统一解析时设置上下文, 为保证异步起的子线程,能拿到值,cacheLocal应该也要用可继承本地线程
	}

	public static void addInContextForCache(String sql, String tableName) {
		CacheSuidStruct struct = new CacheSuidStruct();
		struct.setSql(sql);
		struct.setTableNames(tableName);
		setCacheInfo(sql, struct);
	}

	@SuppressWarnings("rawtypes")
	static void regEntityClass(Class clazz) {
		OneTimeParameter.setAttribute(StringConst.Route_EC, clazz); // EC:Entity Class
	}

	static void regFunType(FunctionType functionType) {
		HoneyContext.setSysCommStrInheritableLocal(StringConst.FunType, functionType.getName());
	}

	static void regSuidType(SuidType suidType) {
		// 为了在Android中分辨出insert,update,delete
		OneTimeParameter.setAttribute(StringConst.SuidType, suidType);
	}

	public static SuidType getSuidType() {
		return (SuidType) OneTimeParameter.getAttribute(StringConst.SuidType);
	}

	static Connection getConn() throws SQLException {
		Connection conn = null;

		conn = getCurrentConnection(); // 获取已开启事务或同一Connection的连接

		if (conn == null) {
			boolean isSameConn = OneTimeParameter.isTrue(StringConst.SAME_CONN_BEGIN);
			if (isSameConn) {
				checkShadingHasMoreDs("Donot support SameConnection in more DataSources at one time!");
				setSameConnectionDoing(); // 提前设置,因RW时,同一连接要改为默认走写库
			}

			conn = SessionFactory.getConnection();

			// 如果设置了同一Connection
			if (isSameConn) {
				setCurrentConnection(conn); // 存入上下文
			}
		}

		return conn;
	}

	public static DatabaseClientConnection getDatabaseConnection() {
		DatabaseClientConnection conn = null;
		conn = SessionFactory.getDatabaseConnection();
		return conn;
	}

	public static void checkShadingHasMoreDs(String exceptionMsg) {
		if (ShardingUtil.hadSharding()) {// 有分片时,涉及多个DS
			List<String> dsNameListLocal = HoneyContext.getListLocal(StringConst.DsNameListLocal);
			if (dsNameListLocal != null && dsNameListLocal.size() > 1) {
//				throw new ShardingErrorException("Donot support SameConnection in more DataSources!");
				throw new ShardingErrorException(exceptionMsg);
			}
		}
	}

	// For exception case. when have exception, must close the conn.
	static void closeConn(Connection conn) {
		try {
//			if (conn != null) conn.close();  //bug   can not be closed before transation rollback
			if (conn != null && conn.getAutoCommit()) conn.close();
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
//			"在事务中间报异常也要删除;"? 不在这里删   应该由程序在事务中调用rollback
//			"事务结束时要删除;"   在事务直接调用，不在这里

//			removeCurrentConnection(); // 同一conn也要删除  已移到以下括号内;   closed 2.2
			if (StringConst.tRue.equals(getSameConnectionDoing())) {
				removeCurrentConnection(); // 2.2
				removesameConnectionDoing(); // 同一conn
				OneTimeParameter.setTrueForKey(StringConst.SAME_CONN_EXCEPTION);
			}
		}
	}

	public static boolean isTransactionConn() { // 正在处理事务： 有当前Conn且不是同一连接
		return getCurrentConnection() != null && !StringConst.tRue.equals(getSameConnectionDoing());
	}

	public static void checkClose(Statement stmt, Connection conn) {
		checkClose(null, stmt, conn);
	}

	public static void checkClose(ResultSet rs, Statement stmt, Connection conn) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				// ignore
			}
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				throw ExceptionHelper.convert(e);
			}
		}
		if (conn != null) {
			try {
				// 如果设置了同一Connection
				// 并且调用了endSameConnection才关闭
				if (StringConst.tRue.equals(getSameConnectionDoing())) {
					if (OneTimeParameter.isTrue(StringConst.SAME_CONN_END)) { // 调用suid.endSameConnection();前的SQL操作 不会触发这里的.
						removesameConnectionDoing();
						if (conn != null) conn.close();
					}
					// else { do not close}
				} else {
					if (conn != null && conn.getAutoCommit()) {// 自动提交时才关闭.如果开启事务,则由事务负责
						conn.close();
					}
				}
			} catch (SQLException e) {
				Logger.debug(e.getMessage());
				throw ExceptionHelper.convert(e);
			}
		}
	}

	// for SqlLib
	static boolean updateInfoInCache(String sql, String returnType, SuidType suidType, Class entityClass) {
		CacheSuidStruct struct = getCacheInfo(sql);
		if (struct != null) {
			struct.setReturnType(returnType);
			struct.setSuidType(suidType.getType());
			struct.setEntityClass(entityClass);
			setCacheInfo(sql, struct);
			return true;
		}
		// 要是没有更新缓存,证明之前还没有登记过缓存,就不能去查缓存.
		return false;
	}

	// for SqlLib
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

	// 因同时使用不同类型DB,需要实时确认DB,而需要路由
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
		String entityList_includes = HoneyConfig.getHoneyConfig().genid_includesEntityList; // in
		_parseListToMap(entityList_includes, entityList_includes_Map, entityListWithStar_in);

		String entityList_excludes = HoneyConfig.getHoneyConfig().genid_excludesEntityList; // ex
		_parseListToMap(entityList_excludes, entityList_excludes_Map, entityListWithStar_ex);

		String levelTwoEntityList = HoneyConfig.getHoneyConfig().cache_levelTwoEntityList; // cache level 2
		_parseListToMap(levelTwoEntityList, entityList_levelTwo_Map, entityListWithStar_levelTwo);
	}

	private static void _parseListToMap(String str, Map<String, String> map, List<String> starList) {
		// com.xxx.aa.User,com.xxx.bb.*,com.xxx.cc.**
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

		if (clazz == null) return false;

		String fullName = clazz.getName();
		String ds = null;
		ds = map.get(fullName); // 1
		if (ds != null) return true;

		if (clazz.getPackage() != null) {
			String packageName = clazz.getPackage().getName();
			ds = map.get(packageName + ".*"); // 2
			if (ds != null) return true;

			// ds=entityClassPathToDs.get(packageName+".**"); //com.xxx.** 省略多级情况下,不适用

			for (int i = 0; i < starList.size(); i++) {
				String s = starList.get(i);
				if (s.endsWith(".**")) {
					String prePath = s.substring(0, s.length() - 2);
					if (fullName.startsWith(prePath)) return true; // 3
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
				needGenId = false; // 有排除,则 不生成的
			else
				needGenId = true;
		} else {
			if (isConfigForEntityEX(clazz))
				needGenId = false; // 有排除,则 不生成的
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

//  是多数据源,有同时使用多种不同类型DB
	static boolean isNeedRealTimeDb() {
		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
		boolean isDifferentDbType = HoneyConfig.getHoneyConfig().multiDS_differentDbType;
		if (enableMultiDs && isDifferentDbType) {
			if (getDsName2DbName() == null || getDsName2DbName().size() <= 1) return false; // V2.1.10 只有一个数据源时,返回false
			return true;
		} else {
			if (useStructForLevel2()) return true;// 1.17 fixed
			return false;
		}
	}

	static boolean useStructForLevel2() {
		boolean useLevelTwo = HoneyConfig.getHoneyConfig().cache_useLevelTwo;
		boolean levelOneTolevelTwo = HoneyConfig.getHoneyConfig().cache_levelOneTolevelTwo;
		return useLevelTwo && !levelOneTolevelTwo; // use LevelTwo,but do not put all levelOneTolevelTwo; need use the struct

	}

	// 同时使用多种类型数据库时,才会触发. 没有分页时,走原来的流程,到SqlLib,才获取数据源处理Suid操作.
	static String getRealTimeDbName() {
		String dbName = null;
		if (isNeedRealTimeDb()) {
			String dsName = Router.getDsName();
			if (dsName != null && HoneyContext.getDsName2DbName() != null) {
				String temp_dbName = HoneyContext.getDsName2DbName().get(dsName);
				if (temp_dbName == null) { // V1.17
					Logger.warn("Did not find the dataSource name : " + dsName); //数据源池里没有,应该抛异常
					throw new ConfigWrongException("Did not find the dataSource name : " + dsName);
				} else {
					return temp_dbName;
				}
			}
		}
		return dbName;
	}

	public static boolean isNeedDs() {
		if (isMultiDs()) {
			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
			boolean differentDbType = HoneyConfig.getHoneyConfig().multiDS_differentDbType;
			if (!(multiDsType == 1 && !differentDbType)) // 不是(模式1(RW)的同种DB) //sameDbType= !differentDbType
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

	private static boolean configRefresh = true;
	private static boolean dsMapConfigRefresh = false;
//	private volatile static boolean dsMapConfigRefresh = false;

	public static boolean isConfigRefresh() {
		return configRefresh;
	}

	public static boolean isDsMapConfigRefresh() {
		return dsMapConfigRefresh;
	}

	/**
	 * 在配置文件配置数据源时,才需要设置为true
	 * @param dsMapConfigRefresh
	 */
	public static void setDsMapConfigRefresh(boolean dsMapConfigRefresh) {
		HoneyContext.dsMapConfigRefresh = dsMapConfigRefresh;
	}

	/**
	 * 涉及变更路由信息的,就要刷新.
	 * if change the route info, need refresh.
	 * @param configRefresh
	 */
	public static void setConfigRefresh(boolean configRefresh) {
		HoneyContext.configRefresh = configRefresh;
		HoneyConfig.setChangeDataSource(true); // 1.17

//		if(configRefresh) initTLRefresh(); //2.2       2.4.0 close   multiDS_sharding应该在系统启动前就定好. 运行过程,不应该更改.
	}

	private static final Integer ONE = 1;

	public static void updateConfig(Map<String, Object> map) {

		if (ObjectUtils.isEmpty(map)) return;

		// ------V2.1.8----start--
//		String activeKey="bee.profiles.active"; //bug
//		String typeKey="bee.profiles.type";//bug

		String activeKey = "active"; // V2.1.10 fixed bug 使用的是HoneyConfig的属性名
		String typeKey = "type"; // V2.1.10 fixed bug

		String active = (String) map.get(activeKey);
		Integer type = (Integer) map.get(typeKey);
		if (StringUtils.isNotBlank(active)) {
			if (ONE.equals(type)) {
				HoneyConfig.getHoneyConfig().overrideByActive(active); // 先处理bee-{active}.properties; 后面再处理bee-spring-boot框架里的其它配置
				// V2.1.10
				HoneyContext.setConfigRefresh(true);
				HoneyContext.setDsMapConfigRefresh(true); // 直接设置, 因解析时会判断相应属性后才进行相应解析
			}
		}
		// 不需要删除active和type两个key; 因不会触发新刷新, 更新后,也可以从HoneyConfig知道当前是什么值.
		// ------V2.1.8----end--

		Object obj = HoneyConfig.getHoneyConfig();
		Class<?> clazz = obj.getClass();
		boolean hasShardingMap = false;
		boolean hasMultiDS_sharding = false;

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			try {
				Field field = clazz.getDeclaredField(entry.getKey());
				if (field != null) {
					HoneyUtil.setAccessibleTrue(field);
					HoneyUtil.setFieldValue(field, obj, entry.getValue());
					if ("multiDS_sharding".equals(entry.getKey()))
						hasMultiDS_sharding = true;
					else if ("sharding".equals(entry.getKey())) hasShardingMap = true;
				}
			} catch (Exception e) {
				throw ExceptionHelper.convert(e);
			}
		}

		setConfigRefresh(true);
		setDsMapConfigRefresh(true); // 是否应该有数据源配置时,才设置更新? 解析时会先判断相关属性的

		// 2.4.0
		if (hasMultiDS_sharding) initAfterChangeMultiDsSharding();
		if (hasMultiDS_sharding && hasShardingMap) ConfigRefreshUtil.prcessShardingRuleInProperties();

	}

	public static boolean getModifiedFlagForCache2(String tableName) {
		Boolean f = modifiedFlagMapForCache2.get(tableName);
		return f == null ? false : f;
	}

	public static void addModifiedFlagForCache2(String tableName, boolean isModified) {
		if (tableName == null) return;
		modifiedFlagMapForCache2.put(tableName, isModified);
	}

	public static Boolean getEntityInterceptorFlag(String fullClassName) {
		return entityInterceptorFlag.get(fullClassName);
	}

	public static void addEntityInterceptorFlag(String fullClassName, boolean isHas) {
		if (fullClassName == null) return;
		entityInterceptorFlag.put(fullClassName, isHas);
	}

	public static Boolean getCustomFlagMap(String key) {
		return customFlagMap.get(key);
	}

	public static void addCustomFlagMap(String key, boolean flag) {
		if (key == null) return;
		customFlagMap.put(key, flag);
	}

	public static boolean isInterceptorSubEntity() {
		return OneTimeParameter.isTrue(StringConst.InterceptorSubEntity);
	}

	// V2.1
	public static void setDataSource(DataSource dataSource) {
		BeeFactory.getInstance().setDataSource(dataSource);
		setConfigRefresh(true);
	}

	// V2.1
	public static void setDataSourceMap(Map<String, DataSource> dataSourceMap) {
		BeeFactory.getInstance().setDataSourceMap(dataSourceMap); // 这里设置,会重新设置dbName
	}

	// V2.1.8
	public static void refreshDataSourceMap() {
		if (HoneyContext.isDsMapConfigRefresh()) {
			Map<String, DataSource> map = ProcessDataSourceMap.refreshDataSourceMap();
			if (map != null && map.size() > 0) {
//			HoneyConfig.getHoneyConfig().dbName=null; //不需要,会重新解析的
				setDataSourceMap(map);

				CacheUtil.clear(); // V2.1.10

//				prcessShardingRuleInProperties(); //2.4.0
			}
			HoneyContext.setDsMapConfigRefresh(false);
		}
	}

	private static final String field2Column = StringConst.PREFIX + "Field2Column";

	static void initParseDefineColumn(Class entityClass) {
		try {
			if (entityClass == null) return;
			if (HoneyUtil.isJavaPackage(entityClass)) {
				Logger.debug("The parameter entityClass is from Java library");
				return;
			}

			Field fields[] = HoneyUtil.getFields(entityClass);
			String entityFullName = entityClass.getName();
			String defineColumn = "";
			String fiName = "";
			int len = fields.length;
			Map<String, String> kv = new HashMap<>();
			Map<String, String> column2Field = new HashMap<>();
			boolean has = false;
			for (int i = 0; i < len; i++) {
				if (HoneyUtil.isSkipField(fields[i])) continue;
				HoneyUtil.setAccessibleTrue(fields[i]);
				if (AnnoUtil.isColumn(fields[i])) {
					defineColumn = AnnoUtil.getValue(fields[i]);
					if (NameCheckUtil.isIllegal(defineColumn)) {
						throw new BeeIllegalParameterException("Annotation Column set wrong value:" + defineColumn);
					}

					fiName = fields[i].getName();
					kv.put(fiName, defineColumn);
					column2Field.put(defineColumn, fiName); // 单表查询拼结果会用到
//					if (findName.equals(fiName)) findDefineColumn = defineColumn;
					has = true;
				}
			} // end for

			if (has) {
				HoneyContext.addCustomMap(field2Column + entityFullName, kv);
				HoneyContext.addCustomMap(StringConst.Column2Field + entityFullName, column2Field); // SqlLib, select会用到.
				HoneyContext.addCustomFlagMap(field2Column + entityFullName, Boolean.TRUE);
			} else {
				HoneyContext.addCustomFlagMap(field2Column + entityFullName, Boolean.FALSE);
			}
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
			// ignore
		}
	}
	
	/**
	 * In production, this attribute should be set in the configuration file using "bee.dosql.multiDS.sharding".
	 * <br>And the running process should not be changed, otherwise relevant configuration and contextual information will be lost.
	 * <br>此方法,只建议在测试时使用.在生产上,此属性应该在配置文件中使用bee.dosql.multiDS.sharding设置,
	 * <br>且运行过程不宜更改,否则会丢失有关配置和上下文信息.
	 * @param multiDsSharding
	 * @since 2.5.2
	 */
	public static void resetMultiDsSharding(boolean multiDsSharding) {
		HoneyConfig.getHoneyConfig().setMultiDsSharding(multiDsSharding);
		initAfterChangeMultiDsSharding();
	}
	
	//after reset config, need call init
	public static void init() {
		HoneyContext.setConfigRefresh(true);
		HoneyContext.setDsMapConfigRefresh(true); // 直接设置, 因解析时会判断相应属性后才进行相应解析
		HoneyContext.refreshDataSourceMap(); 

		HoneyContext.initLoad();
		
		HoneyConfig config=HoneyConfig.getHoneyConfig();
		
		if (config.isAndroid || config.isHarmony) {// V1.17
			config.setDbName(DatabaseConst.SQLite);
//			resetAferSetDbName();
			DbFeatureRegistry.register(DatabaseConst.SQLite, new LimitOffsetPaging()); 
		}

		CalculateRegistry.register(1, new DateCalculate()); // 2.4.0 用户可以覆盖
	}

}
