/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

/**
 * @author Kingstar
 * @since  1.9
 */
public class StringUtils {

	public static boolean isBlank(final String str) {
		return str == null || "".equals(str.trim());
	}

	public static boolean isNotBlank(final String str) {
		return str != null && !"".equals(str.trim());
	}

	public static boolean isEmpty(final String str) {
		return str == null || str.length() == 0;
	}

    public static boolean isNotEmpty(final String str) {
        return !isEmpty(str);
    }
}
