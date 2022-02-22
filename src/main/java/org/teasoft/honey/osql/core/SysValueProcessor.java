/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;

import org.teasoft.bee.osql.Properties;
import org.teasoft.bee.osql.annotation.SysValue;
import org.teasoft.honey.util.ObjectCreatorFactory;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.4
 */
public class SysValueProcessor {
	
	public static <T> void process(T obj) {
		process(obj,BeeProp.getBeeProp());
	}
	
	
	public static <T> void process(T obj,Properties prop) {
		Field[] f = obj.getClass().getDeclaredFields();
		String value;
		String key = "";
		String proValue;
		for (int i = 0; i < f.length; i++) {
			if (f[i].isAnnotationPresent(SysValue.class)) {
				SysValue sysValue = f[i].getAnnotation(SysValue.class);

				value = sysValue.value();
				if (value == null) {
					
				} else if ("".equals(value.trim())) {
					
				} else {
					value = value.trim();
					if (value.startsWith("${") && value.endsWith("}")) { //  ${bee.properties.key}
						key = value.substring(2, value.length() - 1);
						proValue = prop.getProp(key);
						if (proValue == null) {
							continue;
//						}else if(StringUtils.isBlank(proValue) && !"java.lang.String".equals(f[i].getType().getName())) {
						//配置值是空,但字段值不是String,则路过
						}else if(StringUtils.isBlank(proValue) && !String.class.equals(f[i].getType())) {
							continue;
						}else {
							value = proValue;
						}
					}
					try {
						Class c = f[i].getType();
						f[i].setAccessible(true);
						f[i].set(obj, ObjectCreatorFactory.create(value, c));
					} catch (IllegalAccessException e) {
						throw ExceptionHelper.convert(e);
					}
				}
			}
		}
	}
}
