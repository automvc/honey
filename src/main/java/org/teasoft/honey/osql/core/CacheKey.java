/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;

import org.teasoft.honey.distribution.ds.Router;
import org.teasoft.honey.osql.util.MD5;
import org.teasoft.honey.sharding.ShardingUtil;

/**
 * Cache Key.
 * @author Kingstar
 * @since  1.4
 */
public final class CacheKey {
	
	private static final String SEPARATOR=" (@separator#) ";
	private static boolean cacheKeyUseMD5=HoneyConfig.getHoneyConfig().cache_keyUseMD5;
	
	private CacheKey(){}
	
	public static String genKey(String key) {
		String str = fullSql(key);
		if (cacheKeyUseMD5) {//v1.8.99
			str = MD5.getMd5(str);
		}
		return str;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static String fullSql(String sql) {
		
		String value ="";
		String returnType ="";
		Class entityClass=null; 
				
//		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql,true);
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
//			value=struct.getSqlValue();
//			v1.8
			List list=HoneyContext.justGetPreparedValue(sql);
			value=HoneyUtil.list2Value(list,true); 
			returnType=struct.getReturnType();
			entityClass=struct.getEntityClass(); //V2.0
		}
		
		StringBuffer strBuf=new StringBuffer();
		
		if(HoneyContext.isNeedDs()) {
			String ds=Router.getDsName();
			strBuf.append("DataSourceName:");
			strBuf.append(ds);
			strBuf.append(SEPARATOR);
		}
		
		//确定DataSourceName:null (@separator#) Sharding_tabNameList:null时,是否可以表示查所有??
//		DataSourceName:null (@separator#) Sharding_tabNameList:null (@separator#) select id,userid,orderid,name,total,createtime,remark,sequence,abc,updatetime from orders##(index)## where remark=? (@separator#) [values]: Bee(ORM Framework)(String) (@separator#) [returnType]: List<T>
		
		if(ShardingUtil.hadSharding() && HoneyContext.getSqlIndexLocal()==null) { //用于分片的总查询; 每个子线程都有一个具体表名,不需要.
			strBuf.append("Sharding_tabNameList:");
//			strBuf.append(HoneyContext.getTabNameListLocal());
			strBuf.append(HoneyContext.getListLocal(StringConst.TabNameListLocal)+"");
			strBuf.append(SEPARATOR);
		}
		
		if(HoneyConfig.getHoneyConfig().naming_useMoreTranslateType) { //使用多种命名转换类型
			strBuf.append("TranslateType:");
			strBuf.append(NameTranslateHandle.getNameTranslate().getClass().getName());
			strBuf.append(SEPARATOR);
		}
		
		strBuf.append(sql);
		
		if (value == null || "".equals(value.trim())){
			// do nothing
		}else{
			strBuf.append(SEPARATOR);
			strBuf.append("[values]: ");
			strBuf.append(value);
		}
		
		strBuf.append(SEPARATOR);
		strBuf.append("[returnType]: ");
		strBuf.append(returnType);
		
		if (entityClass != null) { //V2.0
			strBuf.append(SEPARATOR);
			strBuf.append("[entity class]: ");
			strBuf.append(entityClass.getName());
		}
		
		return strBuf.toString();
	}

//	//用于清除缓存时,找到sql相关的table
//	public static List<String> genTabKeyList(String sql){
//		return genTabKeyList(sql,false);
//	}
	
	//用于清除缓存时,找到sql相关的table
	static List<String> genTableNameList(String sql){
		
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		List<String> list=new ArrayList<>();
		if (struct != null) {
//		if (struct != null && SuidType.MODIFY.getType().equals(struct.getSuidType()) ) {  //查询时,放缓存也要用到
			String tableNames=struct.getTableNames();
			String tabs[]=tableNames.trim().split("##");
			for (int i = 0; i < tabs.length; i++) {
				list.add(tabs[i]);  // 还要加上数据源信息等其它      在CacheUtil已为仅分库情型加DS
				                    //不加数据源,相同表名数据有更改,同表名的缓存就清除,这样缓存数据更可靠
			}
		}
		return list;
	}
}
