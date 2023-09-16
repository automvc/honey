/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept.annotation;

import java.lang.reflect.Field;
import java.util.List;

import org.teasoft.bee.osql.annotation.Desensitize;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.11-E
 */
public class DesensitizeHandler {

	private DesensitizeHandler() {

	}

	/**
	 * 
	 * @param field 带有Desensitize注解的Field
	 * @param list 需要处理Desensitize注解的List
	 */
	public static void process(Field field, List<?> list) {

		try {
			Desensitize anno = field.getAnnotation(Desensitize.class);

			int start = anno.start();
			int end = anno.size();
			String s = anno.mask();

			for (int i = 0; i < list.size(); i++) {
				Object obj = list.get(i);
				Field f = obj.getClass().getDeclaredField(field.getName());
//				f.setAccessible(true);
				HoneyUtil.setAccessibleTrue(f);
				String targetStr = (String) f.get(obj);
				HoneyUtil.setFieldValue(f, obj, replace(targetStr, start, end, s));
			}

		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
		}
	}

	/**
	 * @param targetStr 需要处理的目录字符串.
	 * @param start 起始索引（包含）,从0开始.
	 * @param size 需要替换的字符数量.如果是-1,则一直替换到结束
	 * @param mask 使用的掩码.
	 * @return
	 */
	private static String replace(String targetStr, int start, int size, String mask) {
		if (StringUtils.isBlank(targetStr)) return targetStr;
		if (start > targetStr.length()) {
			Logger.warn("The start position exceeds the length of the string!", new Exception());//加new Exception()有得追踪位置. 但systemLogger体现不出来.
			return targetStr;
		}

		if (start < 0) start = 0;
		int end = start + size;
		if (size == -1) end = targetStr.length(); //V2.0

		StringBuffer b = new StringBuffer(targetStr);
		for (int i = start; i < end && i < targetStr.length(); i++) {
			b.replace(i, i + 1, mask);
		}

		return b.toString();

	}

}
