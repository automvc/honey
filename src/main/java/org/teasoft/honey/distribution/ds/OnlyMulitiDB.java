/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution.ds;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.teasoft.bee.distribution.ds.Route;
import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;

/**
 * @author Kingstar
 * @since  1.8
 */
public class OnlyMulitiDB implements Route {

	//	private static List dsList=new ArrayList();
	//	private static Map<String,String> map;
	private String defaultDs;

	private Map<String, String> entityClassPathToDs = new ConcurrentHashMap<>();
	private Map<String, String> tableToDs = new ConcurrentHashMap<>();

	private List<String> entityClassPathToDsWithStar = new CopyOnWriteArrayList<>();
	
//	static {
	{ //will run every time use new. for refresh.
		init();
	}
	
	private void init(){
		  String matchEntityClassPath;
		  String matchTable;
		   defaultDs = HoneyConfig.getHoneyConfig().multiDS_defalutDS;
		   
			//仅分库,需要配置默认DB
			if( defaultDs==null || "".equals(defaultDs.trim()) ){
				throw new NoConfigException("Error: bee.dosql.multiDS.defalutDS can not be null or empty when bee.dosql.multiDS.type=2! ");
			}
			matchEntityClassPath = HoneyConfig.getHoneyConfig().multiDS_matchEntityClassPath;
			matchTable = HoneyConfig.getHoneyConfig().multiDS_matchTable;
			if( (matchEntityClassPath==null || "".equals(matchEntityClassPath.trim()))  &&  (matchTable==null || "".equals(matchTable.trim())) ){
				throw new NoConfigException("Error: bee.dosql.multiDS.matchEntityClassPath and bee.dosql.multiDS.matchTable can not be null or empty at same time when bee.dosql.multi-DS.type=2! ");
			}

			parseListToMap(matchEntityClassPath, entityClassPathToDs, true);
			parseListToMap(matchTable, tableToDs, false); //不带*   tableToDs  表名不区分大小
	}

	private void parseListToMap(String str, Map<String, String> map, boolean isAdd2List) {
//		ds2:com.xxx.aa.User,com.xxx.bb.*,com.xxx.cc.**;ds3:com.xxx.dd.User
        if(str==null || "".equals(str.trim())) return ;
		String str1[] = str.split(";");
		for (int i = 0; i < str1.length; i++) {
			String str2[] = str1[i].split(":");

			String str3[] = str2[1].trim().split(",");
			for (int k = 0; k < str3.length; k++) {
				if (isAdd2List && str3[k].trim().endsWith(".**")) 
					entityClassPathToDsWithStar.add(str3[k].trim()); // .** 结尾同时存一份到list
				if(str3[k].trim().indexOf(".")>0){
					map.put(str3[k].trim(), str2[0].trim());
				}else{
					map.put(str3[k].trim().toLowerCase(), str2[0].trim());  //表名不区分大小写
				}
				
			}
		}
	}

	@Override
	public String getDsName() {
		RouteStruct routeStruct = HoneyContext.getCurrentRoute();
		if (routeStruct == null) return defaultDs;
		String ds = null;
		String tables = routeStruct.getTableNames();
		Class clazz = routeStruct.getEntityClass();
		if (clazz == null) {
			//用map传递查询信息,没有Javabean,则class=null.但可以通过bee.dosql.multiDS.matchTable指定数据源.
			ds=getDsViaTables(tables);
			if(ds!=null) return ds;
			
			return defaultDs;
		}
		String fullName = clazz.getName();
//		String ds = null;
		ds = entityClassPathToDs.get(fullName);
		if (ds != null) return ds;

		if (clazz.getPackage() != null) {
			String packageName = clazz.getPackage().getName();
			ds = entityClassPathToDs.get(packageName + ".*");
			if (ds != null) return ds;

			//ds=entityClassPathToDs.get(packageName+".**");   //com.xxx.** 省略多级情况下,不适用

			for (int i = 0; i < entityClassPathToDsWithStar.size(); i++) {
				String s = entityClassPathToDsWithStar.get(i);
				if (s.endsWith(".**")) {
					String prePath = s.substring(0, s.length() - 2);
					if (fullName.startsWith(prePath)) return entityClassPathToDs.get(s);
				} 
//				else if (s.endsWith(".*")) {
//					String prePath = s.substring(0, s.length() - 1);
//					if (fullName.startsWith(prePath)) return entityClassPathToDs.get(s);
//				}

			}
			ds=getDsViaTables(tables);
			if(ds!=null) return ds;
		}

		return defaultDs;
	}
	
	private String getDsViaTables(String tables) {
//		String tables = routeStruct.getTableNames();  
		String ds = null;
		if (tables != null) {
			if (!tables.contains("##")) {
				ds = tableToDs.get(tables.trim().toLowerCase());
				if (ds != null) return ds;
			} else { //only multi-Ds,tables don't allow in different db.仅分库时，多表查询的多个表要在同一个数据源.
                String ts[]=tables.split("##");
				ds = tableToDs.get(ts[0].toLowerCase());
				if (ds != null) return ds;
			}
		}
		return ds;
	}

}
