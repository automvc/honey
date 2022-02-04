/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.BeeExtCache;
import org.teasoft.honey.distribution.ds.RouteStruct;

/**
 * @author Kingstar
 * @since  1.11
 */
public class DefaultBeeExtCache implements BeeExtCache {

	private static boolean useLevelTwo = HoneyConfig.getHoneyConfig().cache_useLevelTwo;
	private static boolean levelOneTolevelTwo = HoneyConfig.getHoneyConfig().cache_levelOneTolevelTwo;
	
	@Override
	public Object get(String sql) {

		Object obj = CacheUtil.get(sql);
		if (useLevelTwo && obj == null) {
			
			//一级缓存获取不到,还有可能是因为有修改原因清了缓存.
			//要检测,不是因为修改的原因,才给查二级的  (这样的话,就要维护修改的语句关联了哪个表, 查询的语句关联了哪个表)
			boolean isModified=false; //  改成动态的   Bee-Ext要能更新
			
			List<String> tableNameList=CacheKey.genTableNameList(sql); 
			for (int j = 0; tableNameList!=null && j < tableNameList.size(); j++) { //涉及的表都没有被更改才获取二级的
				isModified= HoneyContext.getModifiedFlagForCache2(tableNameList.get(j))  ||  isModified ;
			}
			
			//按表来区分 
			if(! isModified) {
			   String key = CacheKey.genKey(sql);
			   obj = getInExtCache(key);   //通过sql生成key,要执行两次,效率???
			}
		}

		return obj;
	}

	@Override
	public void add(String sql, Object result) {
		boolean f = CacheUtil.add(sql, result); //需要知道,哪些不放缓存.  是否放缓存与一级缓存一致.若有例外,还需要另外检测
		if (useLevelTwo) {
			if (f && levelOneTolevelTwo) { //放一级缓存,要levelOneTolevelTwo=true,才放二级(永久和长久缓存默认不放二级缓存,其它一级缓存可通过该配置设置)     
				String key = CacheKey.genKey(sql);
				addInExtCache(key, result);
			} else {
				//不放一级缓存, 二级也有可能放        这种情况不多,所以只需要特别声明只放二级缓存的即可
				RouteStruct routeStruct = HoneyContext.getCurrentRoute();
				boolean f2 = HoneyContext.isLevelTwoCache(routeStruct.getEntityClass());
				if (f2) {
					String key = CacheKey.genKey(sql);
					addInExtCache(key, result);
				}
			}
		}
	}

	@Override
	public void clear(String sql) {
		
//		sql要知道关联了哪个表
//		表对应所有缓存      CacheUtil是记录表缓存的下标到一个set. 下标是一级缓存的,只有一级缓存才有用.   所以二级缓存要另外定义
		
		if (useLevelTwo) {
			//标记对应表已修改过(缓存是脏的).  以便不再给获取缓存
			//todo 这里是用表名,  但二级列表,则是用带包名实体       将所有数据源的同表名的缓存都清除,脏数据风险更小.
			List<String> tableNameList = CacheKey.genTableNameList(sql);
			for (int j = 0; tableNameList != null && j < tableNameList.size(); j++) {
				HoneyContext.addModifiedFlagForCache2(tableNameList.get(j), Boolean.TRUE);
			}

			String key = CacheKey.genKey(sql);

			//要放在Bee-Ext
			//步骤: 1.用sql从genTableNameList获取到对应的表, 2.再用表获取到redis对应缓存的key. 3.循环key,将对应的缓存删了. 
			//删了主节点的,如何同步所有节点    循环所有节点???    Redis本身是如何同步的????
			//用Future??  异步执行,等完成了返回结果,再在java端标记已更新???
			//配置异步线程数. 最大20,  不在[1,20]则定义为2,默认为2

			//要修改时,才要清缓存.   
			//redis如何清缓存????  如何多节点同步???
			clearInExtCache(key); //TODO 2 删除具体二级缓存的

			for (int j = 0; tableNameList != null && j < tableNameList.size(); j++) {
				HoneyContext.addModifiedFlagForCache2(tableNameList.get(j), Boolean.FALSE);
			}
		}
		CacheUtil.clear(sql); //原来是维护有关系结构,知道sql对应表相关的缓存.然后才删除.
		//要放在后面, 原来的会清除结构体   所以先清除二级的
	}
	
	@Override
	public Object getInExtCache(String key) {
		Logger.info("DefaultBeeExtCache:   doing in getInExtCache");
		
		return null;
	}

	@Override
	public void addInExtCache(String key, Object result) {
		Logger.info("DefaultBeeExtCache:   doing in addInExtCache");
	}

	@Override
	public void clearInExtCache(String key) {
		Logger.info("DefaultBeeExtCache:   doing in clearInExtCache");
	}
}
