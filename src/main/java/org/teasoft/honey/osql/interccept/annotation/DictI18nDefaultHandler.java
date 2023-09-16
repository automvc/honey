/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept.annotation;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.annotation.AbstractDictI18nDefaultHandler;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.11-E
 */
@SuppressWarnings("rawtypes")
public class DictI18nDefaultHandler extends AbstractDictI18nDefaultHandler {

	private static Map<String, Map> dictMap = new HashMap<>();

	//有需要可以放到配置文件
	static String CommTableName = "_comm";
	static boolean useCommTabl = true;

	private String getLang() {
		String lang = HoneyConfig.getHoneyConfig().lang;
		if (StringUtils.isBlank(lang)) lang = "CN";
		return lang;
	}

	//	Lang
	//	   ds:table:field
	//	       pairMap即:Map<String, String>

	public Map getDictMap() {
		return dictMap;
	}

	public static void setDictMap(Map<String, Map> dictMap) {
		_setDictMap(dictMap);
	}

	public static void addMap(Map<String, Map> map) {
		_addDictMap(map);
	}

	private static void _setDictMap(Map<String, Map> dictMap) {
		DictI18nDefaultHandler.dictMap = dictMap;
	}

	private static void _addDictMap(Map<String, Map> dictMap) {
		DictI18nDefaultHandler.dictMap.putAll(dictMap);
	}

	@Override
	public void process(Field field, List list) {
		try {
			Object obj0 = list.get(0);

			String tableName = _toTableName(obj0).toLowerCase(); //表名不区分大小写
			String fieldName = field.getName();
			String columnName = _toColumnName(fieldName, obj0.getClass());

			Map<String, String> pairMap = new HashMap<>();
			Map langMap = null;
			try {
				langMap = (Map) getDictMap().get(getLang());
				if (langMap == null) {
					Logger.warn("Can not find the Dict Map in lang: " + getLang());
					return;
				}
				pairMap = (Map<String, String>) langMap.get(tableName + ":" + columnName);
				if (pairMap == null && useCommTabl)
					pairMap = (Map<String, String>) langMap.get(CommTableName + ":" + columnName);

			} catch (Exception e) {
				String simpleName = obj0.getClass().getSimpleName().toLowerCase();
				String fname = field.getName();
				Logger.warn("In language:" + getLang() + ", can not find the dict map(entityName:fieldName):" + simpleName
						+ ":" + fname, e);
				Logger.warn("In language:" + getLang() + ",  can not find the dict map(tableName:columnName):" + tableName
						+ ":" + columnName, e);
			}
			if (langMap==null || pairMap == null) {
				return;
			} else {
				for (int i = 0; i < list.size(); i++) {
					Map<String, String> commPairMap = null;
					Object obj = list.get(i);
					Field f = obj.getClass().getDeclaredField(field.getName());
//					f.setAccessible(true);
					HoneyUtil.setAccessibleTrue(f);
					Object value = f.get(obj);

					String v = pairMap.get((String) value); //没有映射则不替换  //值为null也可以转化
					if (v != null)
						HoneyUtil.setFieldValue(f, obj, v);
					else { //支持一个字段的字典值一部分自己配,一部分用公共的
						if (useCommTabl) commPairMap = (Map<String, String>) langMap.get(CommTableName + ":" + columnName);
						if (commPairMap != null) { //再从公共找,可以为null
							v = commPairMap.get((String) value);
							if (v != null) HoneyUtil.setFieldValue(f, obj, v);
						}
					}
				}
			}

		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
		}
	}

	private static String _toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
	}

	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
}
