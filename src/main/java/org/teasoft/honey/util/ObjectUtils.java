/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.List;
import java.util.Map;

/**
 * @author Kingstar
 * @since  1.9
 */
public class ObjectUtils {
	
	public static boolean isEmpty(final List list) {
		return list == null || list.size() == 0;
	}
	
    public static boolean isNotEmpty(final List list) {
        return !isEmpty(list);
    }
	
	public static boolean isEmpty(final Map map) {
		return map == null || map.size() == 0;
	}
	
    public static boolean isNotEmpty(final Map map) {
        return !isEmpty(map);
    }

}
