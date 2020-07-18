/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kingstar
 * @since  1.4
 */
public final class CacheUtil {

	private final static int MAX_SIZE;
	private final static int timeout;
	
	private static ConcurrentHashMap<String,Integer> map;  //<key,index>  能O(1)从key得到index
	private static long time[];  //放时间点
	private static Object obj[]; //cache obj
	private static String keys[]; //超时批量删除时,用这个得到key,再去map删.
	
	private static Map<String,Set<Integer>> map_tableIndexSet;  //<tableKey,tableIndexSet>  用于记录某个表的所有缓存index
	
	private static Map<String,List<String>> map_tableKeyList;  //<key,tableKey's list>  通过缓存的key找到表的key
	
	private static CacheArrayIndex arrayIndex;
	
	private static Map<String,String> neverCacheTableMap=new HashMap<>();
	private static Map<String,Integer> foreverCacheTableMap=new HashMap<>();  //table is forever or not
	private static Map<String,Integer> foreverCacheTableMap_sqlkey2exist=new HashMap<>(); //sql of forever table is exist or not
	private static Map<String,Integer> foreverCacheModifySynTableMap=new HashMap<>();
	private static String NEVER="1";
	private static Integer FOREVER=0;
	
	private static Map<String,Set<String>> map_foreverSynTableIndexSet=new HashMap<>();  //<tableKey,foreverSynTableIndexSet>  用于记录某个foreverSyn表的所有缓存index
	//set放sqlKey?  用arrayList有更新不方便删除 , 用map,key太长(要放set)
	
	private static Map<String,Object> foreverCacheObjectMap=new HashMap<>();
	private static Map<String,Object> foreverModifySynCacheObjectMap=new Hashtable<>();
	
	private static boolean isShowSql=false;
	
	static {
		MAX_SIZE=HoneyConfig.getHoneyConfig().getCacheMapSize();
		isShowSql=HoneyConfig.getHoneyConfig().isShowSQL();
		
		map=new ConcurrentHashMap<>();
		
		time=new long[MAX_SIZE];
		obj=new Object[MAX_SIZE];
		keys=new String[MAX_SIZE]; //超时批量删除时,用这个得到key,再去map删.
		
		map_tableIndexSet=new Hashtable<>();
		map_tableKeyList=new Hashtable<>();
		
		timeout=HoneyConfig.getHoneyConfig().getCacheTimeout();
		
		arrayIndex=new CacheArrayIndex();
		
		initSpecialTable(HoneyConfig.getHoneyConfig().getNeverCacheTableList(), 
				         HoneyConfig.getHoneyConfig().getForeverCacheTableList(),
				         HoneyConfig.getHoneyConfig().getForeverCacheModifySynTableList()
				);
	}
	
	public static Object get(String sql) {
		String key = CacheKey.genKey(sql);
		if (key == null) return null;

		Integer index = map.get(key);
		if (index != null) { //已有缓存结果
			if (_isTimeout(index)) {
				//			arrayIndex.setKnow(index); //标识已知超时的元素边界     删除时,才传入.  满时,不超时,也会删除一定比例

				if (!arrayIndex.isStartDelete()) {
					delCache(key); //只删除一个
				} else {
					new CacheDeleteThread(index).begin(); //起一个线程执行
				}
				return null;
			}
			if(isShowSql) Logger.print("[Bee] ==========get from cache.");

			// 要是能返回缓存的结果集,说明不用上下文的缓存结构信息了. 可以删
			HoneyContext.deleteCacheInfo(sql);

			return obj[index];
		} else { //还没有放一般缓存的

			List<String> tableKeyList = CacheKey.genTabKeyList(sql); //支持多表的情况  
			//forever 判断是否在永久缓存表
			if (tableKeyList != null && tableKeyList.size() == 1) {
				Integer forever = foreverCacheTableMap.get(tableKeyList.get(0));
				if (forever != null) { //检测到是forever //并且已放缓存(只是该表有放过而矣,不一定是相同查询)
					if (foreverCacheTableMap_sqlkey2exist.get(key) != null) { //查forever的结果已在缓存
						Object obj = foreverCacheObjectMap.get(key);
						if (obj != null) { //是该查询放缓存才删缓存cacheStruct信息
							HoneyContext.deleteCacheInfo(sql);
							if(isShowSql) Logger.print("[Bee] ==========get from cache.");
						}
						
						return obj;
					}
				}
				
				Integer foreverModifySyn=foreverCacheModifySynTableMap.get(tableKeyList.get(0));
				
				if (foreverModifySyn != null){ //检测到是forever modifySyn
//				 && foreverModifySyn == 1) { //并且已放缓存  (同一个表,但不一定是相同查询)
					Object obj = foreverModifySynCacheObjectMap.get(key);
					if (obj != null) { //放缓存了
						HoneyContext.deleteCacheInfo(sql);//是该查询放缓存才删缓存cacheStruct信息
						if(isShowSql) Logger.print("[Bee] ==========get from cache.");
					}
					
					return obj;
				}
			}
			
			//到这表示缓存里还没有
//			要清除缓存结构   不能清.  还没放缓存,要查DB, 查了DB之后,放缓存还是要的
			return null;
		}
	}
	
	//通过key删除缓存  超时才会被调用
	private static void delCache(String key){
		if(key==null) return ;
		Integer i=map.get(key); 
		if (i != null) {
			map.remove(key);
			time[i] = -1;
			obj[i] = null;
			keys[i] = null; //TODO是否需要?
//			Logger.info("------------------delCache cache "+i);
			//要考虑维护表相关的index
			_delTableKeyListByKey(key,i);
		}
	}

	//超时,或者满了都要删除
	 static void delCacheInBetween(int knowIndex) {
		int low = arrayIndex.getLow();
		int high = arrayIndex.getHigh();
//		int know = arrayIndex.getKnow();
		int know = knowIndex;
		if (low <= high) {
			//删除low与know之间的
//			Logger.info("删除缓存,low:"+low+",knowIndex: "+know+", high: "+high);
//			Logger.info("删除缓存从:"+low+",到: "+know);
			for (int i = low; i <= know; i++) {  //i <= know    ,not high
				_deleteCacheByIndex(i);
			}
			arrayIndex.setLow(know + 1);
		} else { //循环的情况  low >high
//			Logger.info("(循环)删除缓存,low:"+low+",knowIndex: "+know+", high: "+high);
//			Logger.info("(循环)删除缓存从:"+low+",到: "+know);
			if ( low < know) { //all:0-99;  low 80    know:90   99, 0  20:high
				for (int i = low; i <= know; i++) {
					_deleteCacheByIndex(i);
				}
				arrayIndex.setLow((know + 1)%MAX_SIZE); //know=MAX_SIZE-1时进入循环
			} else if (know < high) {//all:0-99; low 80    90   99, 0   know:10  20:high
				for (int i = low; i < MAX_SIZE; i++) {  // i!=size
					_deleteCacheByIndex(i);
				}
				
				for (int i = 0; i <= know; i++) {
					_deleteCacheByIndex(i);
				}
				arrayIndex.setLow(know + 1);
			}
		}
	}
	
	 private static void _deleteCacheByIndex(int i){
		 _deleteCacheByIndex(i,true);
	 }
	 
	 //通过下标删除缓存
	private static void _deleteCacheByIndex(int i,boolean includeTableKey){
		if(keys[i]!=null){
		     map.remove(keys[i]);
		   //要考虑维护表相关的index
		     if(includeTableKey)_delTableKeyListByKey(keys[i],i); //表有更新时,整个set都被删除,不用在这里一个个删
		}
		time[i] = -1;
		obj[i] = null;
		keys[i] = null; //TODO是否需要
	}
	
	private static boolean _isTimeout(int index){
		long now=System.currentTimeMillis();
//		time[index]=-1 或0 无效
		if( time[index] >0 && (now-time[index]  > timeout) ) return true;
		else return false;
	}
	
	public static void add(String sql,Object rs){
		addInCache(sql,rs);
	}
	
	// 添加缓存是否可以另起一个线程执行,不用影响到原来的.   但一次只能添加一个元素,作用不是很大.要考虑起线程的开销
	 static void addInCache(String sql,Object rs){
		 
		 String key=CacheKey.genKey(sql);
		 List<String> tableKeyList=CacheKey.genTabKeyList(sql);  //支持多表的情况
		 
		 //never 列表的不用放缓存       暂时只是用表名标识
		 if(tableKeyList!=null && tableKeyList.size()==1){
			if(neverCacheTableMap.get(tableKeyList.get(0)) !=null) { //检测到是never
				//要清除cacheStruct
				HoneyContext.deleteCacheInfo(sql);
				return ;
			}
		 }
		 
		 //forever
		 if(tableKeyList!=null && tableKeyList.size()==1){
			if(foreverCacheTableMap.get(tableKeyList.get(0)) !=null   //检测到是forever
			&& foreverCacheTableMap_sqlkey2exist.get(key)==null ) {  //并且还没有放缓存
				
//				foreverCacheTableMap.put(tableKeyList.get(0), 1);  //标记已放缓存
				foreverCacheTableMap_sqlkey2exist.put(key, 1);  //标记已放缓存
				foreverCacheObjectMap.put(key, rs);
				
				//要清除cacheStruct
				HoneyContext.deleteCacheInfo(sql);
				return ;
			}
			
//			常驻缓存,但有更新时会清除缓存(下次重新查询并放缓存)
			if(foreverCacheModifySynTableMap.get(tableKeyList.get(0)) !=null   //检测到是foreverModifySyn
			&& foreverCacheModifySynTableMap.get(tableKeyList.get(0))==0 ) {  //并且还没有放缓存    //TODO
				
//				foreverCacheModifySynTableMap.put(tableKeyList.get(0), 1);  //标记已放缓存
				_regForeverSynTable(tableKeyList.get(0),key);
				foreverModifySynCacheObjectMap.put(key, rs);
				HoneyContext.deleteCacheInfo(sql);//要清除cacheStruct
				return ;
			}
			
		 }
		 
		//满了,还要处理呢   满了后,一次删10%?  已在配置里设置
		if(arrayIndex.isWouldbeFull()){
//			Logger.info("==================== cache is wouldbe full ..");
			if(isShowSql) Logger.warn("[Bee] ==========Cache would be full!");
//			满了后,起一个线程,一次删除一部分,如10%;然后立即返回,本次不放缓存
//			new CacheClearThread(arrayIndex.getDeleteCacheIndex()).start();  //起一个线程执行
			new CacheDeleteThread(arrayIndex.getDeleteCacheIndex()).begin();  //快满了,删除一定比例最先存入的
			 
			//快满就清除,还是可以放部分的,所以不用立即返回  --> 要是剩下的位置不多,来的数据就足够快,还是有危险.直接返回会安全些
			
			if(arrayIndex.getUsedRate() >=90) {
				if(isShowSql) Logger.warn("[Bee] ==========Cache already used more than 90% !");
				HoneyContext.deleteCacheInfo(sql);//要清除cacheStruct
				return ;  //快满了,本次不放缓存,直接返回
			}
		}
		
		
		tableKeyList=CacheKey.genTabKeyList(sql);  //支持多表的情况    
		HoneyContext.deleteCacheInfo(sql);//要清除cacheStruct
		
		int i=arrayIndex.getNext();  //要保证是线程安全的,否则可能会错
		long ms=System.currentTimeMillis();
		map.put(key, i);
		time[i] = ms;
		obj[i] = rs;
		keys[i] = key; 
		
//		Logger.info("==========================add in cache i:"+i);
		//TODO NULL
		for (int k = 0; k < tableKeyList.size(); k++) {
			_regTabCache(tableKeyList.get(k),i);
			_addIntableKeyList(key,tableKeyList.get(k));
		}
	}
	 
	/**
	 * @param tableKey
	 * @param index 缓存数组的下标
	 */
	private static void _regTabCache(String tableKey,int index){
		Set<Integer> set=map_tableIndexSet.get(tableKey);
		if(set!=null){
			set.add(index);
		}else{
			set=new LinkedHashSet<Integer>();
			set.add(index);
			map_tableIndexSet.put(tableKey, set);
		}
	}
	
	
	private static void _regForeverSynTable(String tableKey,String sqlKey){
		Set<String> set=map_foreverSynTableIndexSet.get(tableKey);
		if(set!=null){
			set.add(sqlKey);
		}else{
			set=new HashSet<String>();
			set.add(sqlKey);
			map_foreverSynTableIndexSet.put(tableKey, set);  //forever table对应查询的Set,Set放的是sqlKey
		}
	}
	
	
	
	
	//用于相关表有更新时,要清除所有与该表相关的缓存
	public static void clear(String sql){
		_clearForeverModifySyn(sql);
		_clearMoreTabCache(sql);  //这步方法会清除cacheStruct
	}
	private static void _clearMoreTabCache(String sql){
	    List<String> tableKeyList=CacheKey.genTabKeyList(sql); 
	    HoneyContext.deleteCacheInfo(sql);//要清除cacheStruct
		for (int j = 0; j < tableKeyList.size(); j++) {  //TODO NULL tableKeyList
			_clearOneTabCache(tableKeyList.get(j));
		}
	}
	
	private static void _clearForeverModifySyn(String sql) {

		List<String> tableKeyList = CacheKey.genTabKeyList(sql);
		String key=CacheKey.genKey(sql);
		 
		Integer k;
		if (tableKeyList != null && tableKeyList.size() > 0) {
			for (int j = 0; j < tableKeyList.size(); j++) { //TODO NULL tableKeyList
				k = foreverCacheModifySynTableMap.get(tableKeyList.get(j));
				if(k!=null){ //foreverModifySynTable
//					foreverModifySynCacheObjectMap.remove(key);  //sql是modify类型的sql,不是查询的
					_clearOneTabCache_ForeverModifySyn(tableKeyList.get(j));
				}
			}
		}
	}
	//用于相关表有更新时,要清除所有与该表相关的缓存  foreverModifySynTable
	private static void _clearOneTabCache_ForeverModifySyn(String tableKey){
		Set<String> set=map_foreverSynTableIndexSet.get(tableKey);
		if(set!=null){
			//清除相关index
			for(String i : set){
				foreverModifySynCacheObjectMap.remove(i);
//				Logger.info("------------------clear cause by modify (forever syn)---");
			}
			//最后将set=null;
			set=null;
			map_foreverSynTableIndexSet.remove(tableKey);  //不能少
		}
	}
	
	
	
	//用于相关表有更新时,要清除所有与该表相关的缓存
	private static void _clearOneTabCache(String tableKey){
		Set<Integer> set=map_tableIndexSet.get(tableKey);
		if(set!=null){
			//清除相关index
			for(Integer i : set){
//				Logger.info("------------------clear cause by modify-----delete i: "+i);
				_deleteCacheByIndex(i,false);  //将有查询到该表的缓存都删除
			}
			//最后将set=null;
			set=null;
			map_tableIndexSet.remove(tableKey);  //不能少
		}
	}
	
	 private static void _addIntableKeyList(String key,String tableKey){
		 List<String> tableKeyList=map_tableKeyList.get(key); 
		 if(tableKeyList!=null){
			 tableKeyList.add(tableKey);
		 }else{
			 tableKeyList= new ArrayList<>(3);  //一般一条语句最多三个表
			 tableKeyList.add(tableKey);
			 map_tableKeyList.put(key, tableKeyList);
		 }
	 }
	
	 private static void _delTableKeyListByKey(String key,int index){
		 List<String> tableKeyList=map_tableKeyList.get(key);
		 //TODO
//         if(tableKeyList==null) return;
		 for (int i = 0; i < tableKeyList.size(); i++) {
			 _deleteTableIndexSet(tableKeyList.get(i),index);
		}
	 }
	
	//用于删除缓存时, 表关联的index记录也要维护(删除)
	//假如不维护tableIndexSet,则在删了缓存后,index给新的缓存用了,要是旧的表有更新,就会把新的缓存也删了.
	private static void _deleteTableIndexSet(String tableKey, int index) {
		Set<Integer> set = map_tableIndexSet.get(tableKey);
		if (set != null) {
			set.remove(index);
		}
	}
	
	private static void initSpecialTable(String never,String forever,String foreverModifySyn){
		if (never != null) {
			String ns[] = never.split(",");
			for (int i = 0; i < ns.length; i++) {
				neverCacheTableMap.put(ns[i].trim(), NEVER);
			}
		}

		if (forever != null) {
			String fs[] = forever.split(",");
			for (int i = 0; i < fs.length; i++) {
				foreverCacheTableMap.put(fs[i].trim(), FOREVER);
			}
		}

		if (foreverModifySyn != null) {
			//	         常驻缓存,但有更新时会清除缓存	
			String fs_syn[] = foreverModifySyn.split(",");
			for (int i = 0; i < fs_syn.length; i++) {
				foreverCacheModifySynTableMap.put(fs_syn[i].trim(), FOREVER);
			}
		}
	}
}
