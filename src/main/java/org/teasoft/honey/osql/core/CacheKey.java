/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kingstar
 * @since  1.4
 */
public class CacheKey {
	
	public static String genKey(String key){
		
		return fullSql(key);
	}
	
	private static String fullSql(String sql) {
		
		String value ="";
		String returnType ="";
				
//		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql,true);
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
//			value=struct.getSqlValue();
//			v1.8
			List list=HoneyContext._justGetPreparedValue(sql);
			value=HoneyUtil.list2Value(list,true); 
			returnType=struct.getReturnType();
		}
		
		StringBuffer strBuf=new StringBuffer();
		strBuf.append(sql);
		
		if (value == null || "".equals(value.trim())){
			
		}else{
			strBuf.append("(@#)[values]: ");
			strBuf.append(value);
		}
		
		strBuf.append("(@#)[returnType]: ");
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
	static List<String> genTabKeyList(String sql){
		
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		List<String> list=new ArrayList<>();
		if (struct != null) {
//		if (struct != null && SuidType.MODIFY.getType().equals(struct.getSuidType()) ) {  //查询时,放缓存也要用到
			String tableNames=struct.getTableNames();
			String tabs[]=tableNames.trim().split("##");
			for (int i = 0; i < tabs.length; i++) {
				list.add(tabs[i]);  //TODO 还要加上数据源信息等其它
			}
		}
		return list;
	}
}
