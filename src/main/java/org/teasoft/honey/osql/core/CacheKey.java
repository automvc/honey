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
	
	private static String fullSql(String sql) {
		
		String value ="";
		String returnType ="";
				
//		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql,true);
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
//			value=struct.getSqlValue();
//			v1.8
			List list=HoneyContext.justGetPreparedValue(sql);
			value=HoneyUtil.list2Value(list,true); 
			returnType=struct.getReturnType();
		}
		
		StringBuffer strBuf=new StringBuffer();
		
//		v1.8
//		boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
//		int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
//		boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
////	if (enableMultiDs && multiDsType == 2) {//仅分库,有多个数据源时
////	if (enableMultiDs && (multiDsType == 2 || (multiDsType ==1 && differentDbType ))) {
//		if (enableMultiDs && ( !(multiDsType ==1 && !differentDbType ))) { //多个数据源,且不是同种类型DB的只读模式
		if(HoneyContext.isNeedDs()) {
			String ds=Router.getDsName();
			strBuf.append("DataSourceName:");
			strBuf.append(ds);
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
		
		return strBuf.toString();
		
//		if (value == null || "".equals(value.trim())){
//			return sql;
//		}else{
////			return sql + "   [values]: " + value;
//			return sql + "(@#)[values]: " + value + "(@#)[returnType]: "+returnType;	
//		}

	}
	
//	public String toMD5(String str){
//		
//	}

	
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
