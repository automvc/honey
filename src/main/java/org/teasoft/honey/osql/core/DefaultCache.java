/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.Cache;
import org.teasoft.honey.osql.cache.CacheUtil;

/**
 * @author Kingstar
 * @since  1.4
 */
public class DefaultCache implements Cache{

	@Override
	public Object get(String sql) {
		return CacheUtil.get(sql);
	}

	@Override
	public void add(String sql, Object resultSet) {
		CacheUtil.add(sql, resultSet);
	}
	
	@Override
	public void clear(String sql) {
	    CacheUtil.clear(sql);
	}
	
}
