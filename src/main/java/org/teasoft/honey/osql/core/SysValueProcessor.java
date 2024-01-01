/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.Properties;
import org.teasoft.bee.osql.annotation.SysValue;
import org.teasoft.honey.util.GroupMap;
import org.teasoft.honey.util.ObjectCreatorFactory;
import org.teasoft.honey.util.StringUtils;

/**
 * SysValue注解处理器.SysValue annotation Processor.
 * @author Kingstar
 * @since  1.4
 */
public class SysValueProcessor {
	
	private SysValueProcessor() {}
	
	public static <T> void process(T obj) {
		process(obj,BeeProp.getBeeProp());
	}
	
	public static <T> void process(T obj,Properties prop) {
		Field[] f = obj.getClass().getDeclaredFields();
		String value;
		String key = "";
		String proValue;
		boolean printOverride=OneTimeParameter.isTrue(StringConst.PREFIX+"need_override_properties");
		for (int i = 0; i < f.length; i++) {
			if (f[i].isAnnotationPresent(SysValue.class)) {
				SysValue sysValue = f[i].getAnnotation(SysValue.class);

				value = sysValue.value();
				if (value == null) {
					//do nothing
				} else if ("".equals(value.trim())) {
					//do nothing
				} else {
					value = value.trim();
					if (value.startsWith("${") && value.endsWith("}")) { // ${bee.properties.key}
						key = value.substring(2, value.length() - 1);
						proValue = prop.getProp(key);
						if (proValue == null) {
							continue;
//						}else if(StringUtils.isBlank(proValue) && !"java.lang.String".equals(f[i].getType().getName())) {
							// 配置值是空,但字段值不是String,则跳过
						} else if (StringUtils.isBlank(proValue) && !String.class.equals(f[i].getType())) {
							continue;
						} else {
							if(printOverride) System.out.println("[Bee] new config,  "+key+":" + proValue+"   ;");// NOSONAR
							try {
								Class<?> c = f[i].getType();
								HoneyUtil.setAccessibleTrue(f[i]);
								HoneyUtil.setFieldValue(f[i], obj, ObjectCreatorFactory.create(proValue, c));
							} catch (IllegalAccessException e) {
								throw ExceptionHelper.convert(e);
							}
						}
					} else {
						System.err.println("SysValue maybe wrong: " + value);// NOSONAR
					}
				}
			}
		}//end for
		
//		GroupMap beePropertiesDbs=null;  //未必是bee.properties;可能已经过更改
//		if(OneTimeParameter.isTrue(StringConst.PREFIX+"need_override_properties")) { //Bee内部,获取旧的dbs
//			beePropertiesDbs =getGroupMap(BeeProp.getBeeProp(),null);
//		}
		
//		GroupMap oldGroupMap=HoneyConfig.getHoneyConfig().getDbsGroupMap();
		
		Map<String, Map<String, String>> oldDbs=HoneyConfig.getHoneyConfig().getDbs();
		
		GroupMap gm =getGroupMap(prop,oldDbs);
		if (gm!=null && ! gm.isEmpty()) {
			try {
				Field dbsF = obj.getClass().getDeclaredField("dbs");
				
//				[下标或其它标识] 未必能按回list的顺序下标
//				List<Map<String, Object>> list=  (List<Map<String, Object>>)dbsF.get(obj);
//				if(list==null || list.size()==0) list=gm.toList();
//				else list.addAll(c);
				
				HoneyUtil.setAccessibleTrue(dbsF);
//				HoneyUtil.setFieldValue(dbsF, obj, gm.toList());
				HoneyUtil.setFieldValue(dbsF, obj, gm.getMap());
			} catch (Exception e) {
				// ignore
//				System.err.println(e.getMessage());
			}
		}
	}
	
	//V2.1.10
	private static GroupMap getGroupMap(Properties prop,Map<String, Map<String, String>> oldDbs) {
		Set<String> keySet = prop.getKeys();
		GroupMap gm = null;
//			gm.add("0", "name", "name0");
		if(keySet.isEmpty()) return gm;
		
		boolean has = false;
		for (String k : keySet) {
			if (k.startsWith("bee.db.dbs[")) {
				if (!has) {
					has = true;
					if(oldDbs!=null && ! oldDbs.isEmpty()) {
						gm =new GroupMap(oldDbs);
					}else {
						gm = new GroupMap();
					}
				}
				int end = k.indexOf(']', 11);
				String tag = k.substring(11, end);
				gm.add(tag, k.substring(end + 2), prop.getProp(k));
			}
		}
		return gm;
	}
	
//	public static void main(String[] args) {
//		HoneyConfig.getHoneyConfig();
//	}
}
