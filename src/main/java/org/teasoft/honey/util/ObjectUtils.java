/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kingstar
 * @since  1.9
 */
@SuppressWarnings("rawtypes")
public class ObjectUtils {

	private ObjectUtils() {}

	public static boolean isEmpty(final Collection collection) {
		return collection == null || collection.size() == 0;
	}

	public static boolean isEmpty(final List list) {
		return list == null || list.size() == 0;
	}

	public static boolean isNotEmpty(final List list) {
		return !isEmpty(list);
	}

	public static boolean isEmpty(final Set set) {
		return set == null || set.size() == 0;
	}

	public static boolean isNotEmpty(final Set set) {
		return !isEmpty(set);
	}

	public static boolean isEmpty(final Map map) {
		return map == null || map.size() == 0;
	}

	public static boolean isNotEmpty(final Map map) {
		return !isEmpty(map);
	}

	public static boolean isEmpty(final String str) {
		return StringUtils.isEmpty(str);
	}

	public static boolean isNotEmpty(final String str) {
		return StringUtils.isNotEmpty(str);
	}

	public static boolean isEmpty(final String strings[]) {
		return StringUtils.isEmpty(strings);
	}

	public static boolean isNotEmpty(final String strings[]) {
		return StringUtils.isNotEmpty(strings);
	}

	public static boolean isTrue(Boolean b) {
		return b == null ? false : b;
	}

	public static String string(Object obj) {
		return obj == null ? null : obj.toString();
	}

	/**
	 * check object is null.
	 * @param obj
	 * @since 2.4.0
	 */
	public static void checkNull(Object obj) {
		if (obj == null) throw new NullPointerException("The object is null.");
	}
}
