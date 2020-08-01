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
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;

/**
 * @author Kingstar
 * @since  1.7.3
 */
public class OnlyMulitiDB implements Route {

	//	private static List dsList=new ArrayList();
	//	private static Map<String,String> map;
	private static String defaultDs;

	private static Map<String, String> entityClassPathToDs = new ConcurrentHashMap<>();
	private static Map<String, String> tableToDs = new ConcurrentHashMap<>();

	private static List<String> entityClassPathToDsWithStar = new CopyOnWriteArrayList<>();

	//	private static String multi_dbList;
	private static String matchEntityClassPath;
	private static String matchTable;

//	static {
	{ //will run every time use new. for refresh.
		defaultDs = HoneyConfig.getHoneyConfig().multiDsDefalutDS;
	  //multi_dbList = HoneyConfig.getHoneyConfig().multi_dbList;
		matchEntityClassPath = HoneyConfig.getHoneyConfig().matchEntityClassPath;
		matchTable = HoneyConfig.getHoneyConfig().matchTable;

		parseListToMap(matchEntityClassPath, entityClassPathToDs, true);
		parseListToMap(matchTable, tableToDs, false); //不带*
	}

	private static void parseListToMap(String str, Map<String, String> map, boolean isAdd2List) {
//		ds2:com.xxx.aa.User,com.xxx.bb.*,com.xxx.cc.**;ds3:com.xxx.dd.User
		String str1[] = str.split(";");
		for (int i = 0; i < str1.length; i++) {
			String str2[] = str1[i].split(":");

			String str3[] = str2[1].trim().split(",");
			for (int k = 0; k < str3.length; k++) {
				if (isAdd2List && str3[k].trim().endsWith(".**")) 
					entityClassPathToDsWithStar.add(str3[k].trim()); //带星号同时存一份到list
				map.put(str3[k].trim(), str2[0].trim());
			}
		}
	}

	@Override
	public String getDsName() {
		RouteStruct routeStruct = HoneyContext.getCurrentRoute();
		if (routeStruct == null) return defaultDs;

		Class clazz = routeStruct.getEntityClass();
		String fullName = clazz.getName();
		String ds = null;
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

			String tables = routeStruct.getTableNames();
			if (tables != null && !tables.contains("##")) {
				ds = tableToDs.get(tables.trim());
				if (ds != null) return ds;
			}
		}

		return defaultDs;
	}

/*	public static void main(String[] args) {
		String str = "ds2:com.xxx.aa.User,com.xxx.bb.*,com.xxx.cc.**;ds3:com.xxx.dd.User";
		parseListToMap(matchEntityClassPath, entityClassPathToDs, true);
		parseListToMap(matchTable, tableToDs, false); //不带*
		System.out.println(entityClassPathToDs);
		System.out.println(entityClassPathToDsWithStar);
		System.out.println(tableToDs);

		System.out.println(String.class.getName());
		System.out.println(String.class.getSimpleName());
		System.out.println(String.class.getPackage().getName());
	}*/

}
