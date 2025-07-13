/*
 * Copyright 2013-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.teasoft.bee.osql.Serializer;
import org.teasoft.honey.distribution.ds.Router;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.util.ObjectUtils;

/**
 * 缓存工具.Cache Util.
 * @author Kingstar
 * @since  1.4
 */
public final class CacheUtil {

	private static final byte lock[] = new byte[0];

	private static final int MAX_SIZE;
	private static final int timeout;

	private static ConcurrentMap<String, Integer> map; // <key,index> 能O(1)从key得到index
	private static long time[]; // 放时间点
	private static Object obj[]; // cache obj
	private static String keys[]; // 超时批量删除时,用这个得到key,再去map删.

	private static ConcurrentMap<String, Set<Integer>> map_tableIndexSet; // <tableName,tableIndexSet> 用于记录某个表的所有缓存index

	private static ConcurrentMap<String, List<String>> map_tableNameList; // <key,tableName's list> 通过缓存的key找到表的key

	private static CacheArrayIndex arrayIndex;

	private static Map<String, String> neverCacheTableMap = new HashMap<>();
	private static Map<String, Integer> foreverCacheTableMap = new HashMap<>(); // table is forever or not
	private static Map<String, Integer> foreverCacheTableMap_sqlkey2exist = new HashMap<>(); // sql of forever table is exist or
																								// not
	private static Map<String, Integer> foreverCacheModifySynTableMap = new HashMap<>();
	private static final String NEVER = "1";
	private static final Integer FOREVER = 0;

	private static Map<String, Set<String>> map_foreverSynTableIndexSet = new HashMap<>(); // <tableName,foreverSynTableIndexSet>
																							// 用于记录某个foreverSyn表的所有缓存index
	// set放sqlKey? 用arrayList有更新时不方便删除 , 用map,key太长(要放set)

	private static Map<String, Object> foreverCacheObjectMap = new HashMap<>();
	private static Map<String, Object> foreverModifySynCacheObjectMap = new Hashtable<>();

	private static boolean isShowSql = false;
	private static String logCacheMsg = "==========get from Cache.";

	static {
		MAX_SIZE = HoneyConfig.getHoneyConfig().cache_mapSize;
		isShowSql = HoneyConfig.getHoneyConfig().showSQL;
		timeout = HoneyConfig.getHoneyConfig().cache_timeout;

		init();
	}

	static void clear() {
		map.clear();
		map_tableIndexSet.clear();
		map_tableNameList.clear();

		init();
	}

	private static void init() {

		map = new ConcurrentHashMap<>();

		time = new long[MAX_SIZE];
		obj = new Object[MAX_SIZE];
		keys = new String[MAX_SIZE]; // 超时批量删除时,用这个得到key,再去map删.

		map_tableIndexSet = new ConcurrentHashMap<>();
		map_tableNameList = new ConcurrentHashMap<>();

		arrayIndex = new CacheArrayIndex();

		initSpecialTable(HoneyConfig.getHoneyConfig().cache_never, HoneyConfig.getHoneyConfig().cache_forever,
				HoneyConfig.getHoneyConfig().cache_modifySyn);
	}

	private CacheUtil() {}

	private static boolean getUseLevelTwo() {
		return HoneyConfig.getHoneyConfig().cache_useLevelTwo;
	}

	/**
	 * 从缓存获取结果.get the result from cache.
	 * @param sql sql语句或相关key. sql string or other key.
	 * @return 从缓存获取的结果.the result get from cache.
	 */
	public static Object get(String sql) {
		String key = CacheKey.genKey(sql);
		if (key == null) return null;

		Integer index = map.get(key);
		if (index != null) { // 已有缓存结果
			if (_isTimeout(index)) {
				// arrayIndex.setKnow(index); //标识已知超时的元素边界 删除时,才传入. 满时,不超时,也会删除一定比例
				if (!arrayIndex.isStartDelete()) {
					delCache(key); // 只删除一个
				} else {
					new CacheDeleteThread(index).begin(); // 起一个线程执行
				}
				return null;
			}
			if (isShowSql) logSQL(logCacheMsg);

			// 要是能返回缓存的结果集,说明不用上下文的缓存结构信息了. 可以删
			HoneyContext.deleteCacheInfo(sql);
//			return obj[index];
			return copyObjectForGet(obj[index]);
		} else { // 还没有放一般缓存的 , 则判断是否有在永久或长久缓存

			List<String> tableNameList = CacheKey.genTableNameList(sql); // 支持多表的情况
			// forever 判断是否在永久缓存表
			if (tableNameList != null && tableNameList.size() == 1) {
//				Integer forever = foreverCacheTableMap.get(tableKeyList.get(0).toLowerCase());
				Integer forever = _getConfigCacheTableMapValue(foreverCacheTableMap, tableNameList);

				if (forever != null) { // 检测到是forever //并且已放缓存(只是该表有放过而矣,不一定是相同查询)
					if (foreverCacheTableMap_sqlkey2exist.get(key) != null) { // 查forever的结果已在缓存
						Object obj0 = foreverCacheObjectMap.get(key);
						if (obj0 != null) { // 是该查询放缓存才删缓存cacheStruct信息
							HoneyContext.deleteCacheInfo(sql);
							if (isShowSql) logSQL(logCacheMsg);
						}

//						return obj0;
						return copyObjectForGet(obj0);
					}
				}

//				Integer foreverModifySyn=foreverCacheModifySynTableMap.get(tableKeyList.get(0).toLowerCase());
				Integer foreverModifySyn = _getConfigCacheTableMapValue(foreverCacheModifySynTableMap, tableNameList);

				if (foreverModifySyn != null) { // 检测到是forever modifySyn
//				 && foreverModifySyn == 1) { //并且已放缓存  (同一个表,但不一定是相同查询)
					Object obj1 = foreverModifySynCacheObjectMap.get(key);
					if (obj1 != null) { // 放缓存了
						HoneyContext.deleteCacheInfo(sql);// 是该查询放缓存才删缓存cacheStruct信息
						if (isShowSql) logSQL(logCacheMsg);
					}

					return copyObjectForGet(obj1);
				}
			}

			// 到这表示缓存里还没有
//			要清除缓存结构   不能清.  还没放缓存,要查DB, 查了DB之后,放缓存还是要的
			return null;
		}
	}

	// 通过key删除缓存 超时才会被调用
	private static void delCache(String key) {
		if (key == null) return;
		Integer i = map.get(key);
		if (i != null) {
			map.remove(key);
			time[i] = -1;
			obj[i] = null;
			keys[i] = null;
//			Logger.info("------------------delCache cache "+i);
			// 要考虑维护表相关的index
			_delTableIndexSetByKey(key, i);
		}
	}

	// 超时,或者满了都要删除 由新起线程执行
	static void delCacheInBetween(int knowIndex) {
		int low = arrayIndex.getLow();
		int high = arrayIndex.getHigh();
//		int know = arrayIndex.getKnow();
		int know = knowIndex;
		if (low <= high) {
			// 删除low与know之间的
//			Logger.info("删除缓存,low:"+low+",knowIndex: "+know+", high: "+high);
//			Logger.info("删除缓存从:"+low+",到: "+know);
			for (int i = low; i <= know; i++) { // i <= know ,not high
				_deleteCacheByIndex(i);
			}
			arrayIndex.setLow(know + 1);
		} else { // 循环的情况 low >high
//			Logger.info("(循环)删除缓存,low:"+low+",knowIndex: "+know+", high: "+high);
//			Logger.info("(循环)删除缓存从:"+low+",到: "+know);
			if (low < know) { // all:0-99; low 80 know:90 99, 0 20:high
				for (int i = low; i <= know; i++) {
					_deleteCacheByIndex(i);
				}
				arrayIndex.setLow((know + 1) % MAX_SIZE); // know=MAX_SIZE-1时进入循环
			} else if (know < high) {// all:0-99; low 80 90 99, 0 know:10 20:high
				for (int i = low; i < MAX_SIZE; i++) { // i!=size
					_deleteCacheByIndex(i);
				}

				for (int i = 0; i <= know; i++) {
					_deleteCacheByIndex(i);
				}
				arrayIndex.setLow(know + 1);
			}
		}
	}

	private static void _deleteCacheByIndex(int i) {
		_deleteCacheByIndex(i, true);
	}

	// 通过下标删除缓存
	private static void _deleteCacheByIndex(int i, boolean includeTableName) {
		if (keys[i] != null) {
			map.remove(keys[i]);
			// 要考虑维护表相关的index
			if (includeTableName)
				_delTableIndexSetByKey(keys[i], i); // 表有更新时,整个set都被删除,不用在这里一个个删 该方法内也会维护map_tableNameList
			else
				map_tableNameList.remove(keys[i]);
		}
		time[i] = -1;
		obj[i] = null;
		keys[i] = null;
	}

	private static boolean _isTimeout(int index) {
		long now = System.currentTimeMillis();
//		time[index]=-1 或0 无效
		if (time[index] > 0 && (now - time[index] > timeout))
			return true;
		else
			return false;
	}

	/**
	 * false未放缓存,true已放缓存.
	 * False not cached, true cached
	 * @param sql sql语句或相关key. sql string or other key
	 * @param rs 结果集.result
	 * @return 返回是否已放缓存.whether it has been put in cache.
	 */
	static boolean add(String sql, Object rs) {
		return addInCache(sql, rs);
	}

	// 添加缓存是否可以另起一个线程执行,不用影响到原来的. 但一次只能添加一个元素,作用不是很大.要考虑起线程的开销
	static boolean addInCache(String sql, Object rs) {

		List<String> tableNameList = CacheKey.genTableNameList(sql); // 支持多表的情况
//		 tableNameList 假如为空时，不放入缓存？      是可以放缓存且能用上缓存，但有更改时，没有表关联，清除不了，会有脏数据的风险；
		if (ObjectUtils.isEmpty(tableNameList)) return false; // 2.4.0 没有指定表名则不放缓存

		if (getCachePrototype() == 1 || getCachePrototype() == 2) {
			try {

				boolean isSerial = isSerial(rs); // rs用ArrayList包装后是序列化,但实体仍然不是,也会序列化不了.
				if (!isSerial && getCachePrototype() == 1) {
					Logger.debug("bee.osql.cache.prototype=1 , the entity is not Serial, will do not put in cache!");
					return false; // do not put in cache
				}
				if (isSerial) { // 实体有实现Serializable接口
					Serializer jdks = new JdkSerializer();
					Object rsNew = jdks.unserialize(jdks.serialize(rs));
					if (rs instanceof String) rs = (String) rsNew;
					else if (rs instanceof List) rs = (List) rsNew;
					else rs = rsNew;
				}

			} catch (Exception e) { // NotSerializableException
				Logger.debug(e.getMessage(), e);
				if (getCachePrototype() == 1) return false; // 严格 异常则不放入缓存
				// 不严格: 有异常则使用原对象 serialize发生异常,则会往后执行(使用原来的)
			}
		}

		String key = CacheKey.genKey(sql);

		// never 列表的不用放缓存 暂时只是用表名标识
		if (tableNameList != null && tableNameList.size() == 1) {
//			if(neverCacheTableMap.get(tableKeyList.get(0).toLowerCase()) !=null) { //检测到是never
			if (_inConfigCacheTableMap(neverCacheTableMap, tableNameList)) { // 检测到是never
				// 要清除cacheStruct
				HoneyContext.deleteCacheInfo(sql);
				return false;
			}
		}

		// forever
		if (tableNameList != null && tableNameList.size() == 1) {
//			if(foreverCacheTableMap.get(tableKeyList.get(0).toLowerCase()) !=null   //检测到是forever 
			if (_inConfigCacheTableMap(foreverCacheTableMap, tableNameList)
					&& foreverCacheTableMap_sqlkey2exist.get(key) == null) { // 并且还没有放缓存

//				foreverCacheTableMap.put(tableKeyList.get(0), 1);  //标记已放缓存
				foreverCacheTableMap_sqlkey2exist.put(key, 1); // 标记已放缓存
				foreverCacheObjectMap.put(key, rs);

				// 要清除cacheStruct
				HoneyContext.deleteCacheInfo(sql);
				return false; // 永久缓存默认不放二级缓存
			}

//			常驻缓存,但有更新时会清除缓存(下次重新查询并放缓存)
//			if(foreverCacheModifySynTableMap.get(tableKeyList.get(0).toLowerCase()) !=null   //检测到是foreverModifySyn
//			&& foreverCacheModifySynTableMap.get(tableKeyList.get(0).toLowerCase())==0 ) {  //并且还没有放缓存 

			if (_inConfigCacheTableMap(foreverCacheModifySynTableMap, tableNameList) // 检测到是foreverModifySyn
					&& _getConfigCacheTableMapValue(foreverCacheModifySynTableMap, tableNameList) == 0) { // 并且还没有放缓存

//				foreverCacheModifySynTableMap.put(tableKeyList.get(0), 1);  //标记已放缓存
				_regForeverSynTable(tableNameList.get(0), key);
				foreverModifySynCacheObjectMap.put(key, rs);
				HoneyContext.deleteCacheInfo(sql);// 要清除cacheStruct
				return false; // 长久缓存默认不放二级缓存
			}

		}

		// 满了,还要处理呢 满了后,一次删10%? 已在配置里设置
		if (arrayIndex.isWouldbeFull()) {
//			Logger.info("==================== cache is wouldbe full ..");
			if (isShowSql) Logger.warn("[Bee] ==========Cache would be full!");
//			满了后,起一个线程,一次删除一部分,如10%;然后立即返回,本次不放缓存
//			new CacheClearThread(arrayIndex.getDeleteCacheIndex()).start();  //起一个线程执行
			new CacheDeleteThread(arrayIndex.getDeleteCacheIndex()).begin(); // 快满了,删除一定比例最先存入的

			// 快满就清除,还是可以放部分的,所以不用立即返回 --> 要是剩下的位置不多,来的数据就足够快,还是有危险.直接返回会安全些

			if (arrayIndex.getUsedRate() >= 90) {
				if (isShowSql) Logger.warn("[Bee] ==========Cache already used more than 90% !");
				HoneyContext.deleteCacheInfo(sql);// 要清除cacheStruct
				return false; // 快满了,本次不放缓存,直接返回
			}
		}

//		tableNameList=CacheKey.genTableNameList(sql);  //支持多表的情况           重复?????  
		if (!getUseLevelTwo()) // 使用level two cache后,要处理了二级才能删
			HoneyContext.deleteCacheInfo(sql);// 要清除cacheStruct

		int i = arrayIndex.getNext(); // 要保证是线程安全的,否则可能会错
		long ms = System.currentTimeMillis();
		map.put(key, i);
		time[i] = ms;
		obj[i] = rs;
		keys[i] = key;

		synchronized (lock) {
			String tableName = null;
			for (int k = 0; tableNameList != null && k < tableNameList.size(); k++) {
				tableName = tableNameList.get(k);
				if (ShardingUtil.isSharding()) {// 2.4.2
					String baseTabName = ShardingRegistry.getBaseTabName(tableName);
					if (baseTabName != null) tableName = baseTabName + StringConst.ShardingTableIndexStr; // 2.4.2 Sharding使用基本表关联
				}
				_regTabCache(tableName, i);
				_addIntableNameList(key, tableName);
			}
		}

		return true;
	}

	/**
	 * @param tableName 
	 * @param index 缓存数组的下标
	 */
	private static void _regTabCache(String tableName, int index) {
		Set<Integer> set = map_tableIndexSet.get(tableName);
		if (set != null) {
			set.add(index);
		} else {
			set = new LinkedHashSet<>();
			set.add(index);
			map_tableIndexSet.put(tableName, set);
		}
	}

	private static void _regForeverSynTable(String tableName, String sqlKey) {
		Set<String> set = map_foreverSynTableIndexSet.get(adjust(tableName));
		if (set != null) {
			set.add(sqlKey);
		} else {
			set = new HashSet<>();
			set.add(sqlKey);
			map_foreverSynTableIndexSet.put(adjust(tableName), set); // forever table对应查询的Set,Set放的是sqlKey
		}
	}

	// adjust tableName
	private static String adjust(String tableName) {
//		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
//		int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
//		boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
////		if (enableMultiDs && multiDsType == 2) {//仅分库,有多个数据源时
//		if (enableMultiDs && (multiDsType ==2 || (multiDsType ==1 && differentDbType ) ) ) {
		if (HoneyContext.isNeedDs()) {
			String ds = Router.getDsName();
			tableName = ds + "." + tableName;
		}
		return tableName;
	}

	// 用于相关表有更新时,要清除所有与该表相关的缓存
	public static void clear(String sql) {
		_clearForeverModifySyn(sql);
		_clearMoreTabCache(sql); // 这步方法会清除cacheStruct
	}

	private static void _clearMoreTabCache(String sql) {
		List<String> tableNameList = CacheKey.genTableNameList(sql);
		HoneyContext.deleteCacheInfo(sql);// 要清除cacheStruct
		for (int j = 0; tableNameList != null && j < tableNameList.size(); j++) { // NULL tableNameList
			_clearOneTabCache(tableNameList.get(j));
		}
	}

	private static void _clearForeverModifySyn(String sql) {

		List<String> tableNameList = CacheKey.genTableNameList(sql);
//		String key=CacheKey.genKey(sql);

		Integer k;
		if (tableNameList != null && tableNameList.size() > 0) {
			for (int j = 0; j < tableNameList.size(); j++) { // need check NULL tableNList
//				k = foreverCacheModifySynTableMap.get(tableKeyList.get(j).toLowerCase());
				k = _getConfigCacheTableMapValue(foreverCacheModifySynTableMap, tableNameList);
				if (k != null) { // foreverModifySynTable
//					foreverModifySynCacheObjectMap.remove(key);  //sql是modify类型的sql,不是查询的
					_clearOneTabCache_ForeverModifySyn(tableNameList.get(j));
				}
			}
		}
	}

	// 用于相关表有更新时,要清除所有与该表相关的缓存 foreverModifySynTable
	private static void _clearOneTabCache_ForeverModifySyn(String tableName) {
		Set<String> set = map_foreverSynTableIndexSet.get(adjust(tableName));
//		Set<String> set=_getSynTableIndexSet(tableKey);
		if (set != null) {
			// 清除相关index
			for (String i : set) {
				foreverModifySynCacheObjectMap.remove(i);
//				Logger.info("------------------clear cause by modify (forever syn)---");
			}
			// 最后将set=null;
			set = null;
			map_foreverSynTableIndexSet.remove(adjust(tableName)); // 不能少 // 仅分库时, DS.tableName
		}
	}

	// 用于相关表有更新时,要清除所有与该表相关的缓存
	private static void _clearOneTabCache(String tableName) {
		Set<Integer> set = map_tableIndexSet.get(tableName);
		if (set == null && ShardingUtil.isSharding()) {// 2.4.2
			String baseTabName = ShardingRegistry.getBaseTabName(tableName);
			set = map_tableIndexSet.get(baseTabName + StringConst.ShardingTableIndexStr);
		}
		if (set != null) {
			// 清除相关index
			for (Integer i : set) {

				// for more table join select 用于多表关联查询
				if (keys[i] != null) {
					List<String> list = map_tableNameList.get(keys[i]);
					for (int j = 0; list != null && j < list.size(); j++) {
						if (!tableName.equals(list.get(j))) {
							map_tableIndexSet.remove(list.get(j));
						}
					}
				}

				_deleteCacheByIndex(i, false); // 将有查询到该表的缓存都删除

			}
			// 最后将set=null;
			set = null;
			map_tableIndexSet.remove(tableName); // 不能少
		}
	}

	private static void _addIntableNameList(String key, String tableName) {
		List<String> tableNameList = map_tableNameList.get(key);
		if (tableNameList != null) {
			tableNameList.add(tableName);
		} else {
//			 tableNameList= new ArrayList<>(3);  //一般一条语句最多三个表  
//			 tableNameList= new ArrayList<>();  // 但多线程,不同线程但一样的语句,都会放进来, 会超过3个.  已改为同步
//			 tableNameList.add(tableName);         //多个线程都添加数据, 可能导致越界  ,但删除时,可以删完. 
			tableNameList = new Vector<>();
			tableNameList.add(tableName);
			map_tableNameList.put(key, tableNameList);
		}
	}

	private static void _delTableIndexSetByKey(String key, int index) {
		List<String> tableNameList = map_tableNameList.get(key);

		if (tableNameList != null) {
			Set<String> set = new HashSet<>();
			int size = tableNameList.size();
			for (int i = 0; i < size; i++) {
				if (!set.add(tableNameList.get(i))) { // V1.9.8 多线程时(有相同表名),需要将整个对应的map_tableIndexSet里set删除.
					_clearOneTabCache(tableNameList.get(i));
//					---------------- map_tableIndexSet :  {test_user=[0, 1, 2, 3, 4, 5, 6, 7]}
//					---------------- map_tableNameList :  {select * from test_user (@separator#) [returnType]: List<T>=
//					[test_user, test_user, test_user, test_user, test_user, test_user, test_user, test_user]}
				} else {
					_deleteTableIndexSet(tableNameList.get(i), index);
				}
			}
		}
		map_tableNameList.remove(key); // v1.9
	}

	// 用于删除缓存时, 表关联的index记录也要维护(删除)
	// 假如不维护tableIndexSet,则在删了缓存后,index给新的缓存用了,要是旧的表有更新,就会把新的缓存也删了.
	private static void _deleteTableIndexSet(String tableName, int index) {
		Set<Integer> set = map_tableIndexSet.get(tableName);
		if (set == null && ShardingUtil.isSharding()) { // 2.4.2
			String baseTabName = ShardingRegistry.getBaseTabName(tableName);
			set = map_tableIndexSet.get(baseTabName + StringConst.ShardingTableIndexStr);
		}
		if (set != null) {
			set.remove(index);
		}
	}

	private static boolean _inConfigCacheTableMap(Map map, List<String> list) {
//		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
//		if (enableMultiDs) {
//			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
//			boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
////			if (multiDsType == 2) {//仅分库,有多个数据源时
//			if ((multiDsType ==2 || (multiDsType ==1 && differentDbType) )) {
		if (HoneyContext.isNeedDs()) {
			String tableName = list.get(0);
			if (map.get(tableName.toLowerCase()) != null) {
				return true;
			} else {
				String ds = Router.getDsName();
				if (map.get((ds + "." + tableName).toLowerCase()) != null)
					return true;
				else
					return false;
			}
		}
//		}
		return map.get(list.get(0).toLowerCase()) != null ? true : false;
	}

	private static Integer _getConfigCacheTableMapValue(Map<String, Integer> map, List<String> list) {

//		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;

//		if (enableMultiDs) {
//			int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
//			boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
////			if (multiDsType == 2) {//仅分库,有多个数据源时

//			if ((multiDsType ==2 || (multiDsType ==1 && differentDbType ))) {//仅分库,有多个数据源时
		if (HoneyContext.isNeedDs()) {
			String tableName = list.get(0);
			Integer v1 = map.get(tableName.toLowerCase());
			if (v1 != null) { // 先检测不带数据源的
				return v1;
			} else {
				String ds = Router.getDsName();
				Integer v2 = map.get((ds + "." + tableName).toLowerCase());
				return v2;
			}
		}
//		}

		return map.get(list.get(0).toLowerCase());
	}

	private static void initSpecialTable(String never, String forever, String foreverModifySyn) {
		if (never != null) {
			String ns[] = never.split(",");
			for (int i = 0; i < ns.length; i++) {
				neverCacheTableMap.put(ns[i].trim().toLowerCase(), NEVER); // 表名不区分大小写
			}
		}

		if (forever != null) {
			String fs[] = forever.split(",");
			for (int i = 0; i < fs.length; i++) {
				foreverCacheTableMap.put(fs[i].trim().toLowerCase(), FOREVER);
			}
		}

		if (foreverModifySyn != null) {
			// 常驻缓存,但有更新时会清除缓存
			String fs_syn[] = foreverModifySyn.split(",");
			for (int i = 0; i < fs_syn.length; i++) {
				foreverCacheModifySynTableMap.put(fs_syn[i].trim().toLowerCase(), FOREVER);
			}
		}
	}

	private static int getCachePrototype() {
		return HoneyConfig.getHoneyConfig().cache_prototype;
	}

	// 1,2 deepCopy, 0: original
	private static Object copyObjectForGet(Object object) {

		if (getCachePrototype() == 0) return object;

		try {
			if (isSerial(object)) { // 1,2
				Serializer jdks = new JdkSerializer();
				return jdks.unserialize(jdks.serialize(object));
			} else {
				if (getCachePrototype() == 1) return null; // 严格
				if (getCachePrototype() == 2) return object;
			}
		} catch (Exception e) { // NotSerializableException
			Logger.debug(e.getMessage(), e);
			if (getCachePrototype() == 1) return null; // 严格
		}
		return object; // 不严格 有异常则返回原对象
	}

	@SuppressWarnings("rawtypes")
	private static boolean isSerial(Object rs) {
		if (rs instanceof List) {
			try {
				List list = (List) rs;
				if (list != null && list.size() > 0 && list.get(0) != null) {
					return Serializable.class.isAssignableFrom(list.get(0).getClass());
				}
			} catch (Exception e) {
				Logger.debug(e.getMessage(), e);
			}
		}

		return Serializable.class.isAssignableFrom(rs.getClass());
	}
	
	private static void logSQL(String hardStr) {
		HoneyUtil.logSQL(hardStr);
	}
}
