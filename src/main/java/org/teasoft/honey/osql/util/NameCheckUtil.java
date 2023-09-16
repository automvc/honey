/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.util;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.teasoft.bee.osql.LowerKey;
import org.teasoft.bee.osql.exception.BeeErrorNameException;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  1.9
 */
public class NameCheckUtil {
	
	private static Set<String> keywordSet;
	
	private NameCheckUtil() {}
	
	static {
		keywordSet = _keywordSet();
	}

	public static Set<String> keywordSet() {
		return keywordSet;
	}

	public static boolean isKeyName(String name) {
		return keywordSet.contains(name);
	}

	private static Set<String> _keywordSet() {
		LowerKey entity = new LowerKey();
		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		Set<String> set = new HashSet<>();
		try {
			for (int i = 0; i < len; i++) {
//				fields[i].setAccessible(true);
				HoneyUtil.setAccessibleTrue(fields[i]);
				set.add(fields[i].get(entity).toString());
			}
		} catch (Exception e) {
		    //ignore
		}

		return set;
	}

	public static boolean isValidName(String name) {
		String p = "^[a-zA-Z]{1}[0-9a-zA-Z_.]{0,}$";
//		String p = "^[0-9a-zA-Z_.]{1,}$";    //mysql可以用数字为字段名,但java不允许数字开头，转换时会报错
		Pattern pattern = Pattern.compile(p);
		return pattern.matcher(name).find();
	}

	public static boolean isNotValidName(String name) {
		return !isValidName(name);
	}
	
	public static void checkName(String name) {
		if (name != null && name.contains(",")) {
			String n[] = name.split(",");
			for (int i = 0; i < n.length; i++) {
				_checkName(n[i].trim());
			}
		} else {
			_checkName(name);
		}
	}

	private static void _checkName(String name) {
		
		if(name==null) throw new BeeErrorNameException("The name is null !");
		
		if("count(*)".equalsIgnoreCase(name)) return ;

		if (NameCheckUtil.isKeyName(name)) {
			Logger.warn("The name : '" + name + "' , it is key word name!");
		}

		if (NameCheckUtil.isValidName(name)) {
			return;
		} else {
			if (isIllegal(name)) {
				throw new BeeErrorNameException("The name: '" + name + "' is illegal!");
			} else {
//				Logger.debug("The name is '" + name + "' , does not conform to naming conventions!",new Exception()); 
				Logger.debug("The name is '" + name + "' , does not conform to naming conventions!"); 
			}
		}

	}

	//是否非法名称
	public static boolean isIllegal(String fieldName) {
		if (fieldName == null || fieldName.contains(" ") || fieldName.contains("-")
				|| fieldName.contains("#") || fieldName.contains("|") || fieldName.contains("+")
				|| fieldName.contains("/*")
				|| fieldName.contains(";")
				|| fieldName.contains("//")
				) {
			return true;
		}
		return false;
	}

}
