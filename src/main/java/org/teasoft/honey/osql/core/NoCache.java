/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.Cache;

/**
 * 不使用缓存.No Cache.
 * @author Kingstar
 * @since  1.4
 */
public class NoCache implements Cache {
	@Override
	public Object get(String key) {
		return null;
	}

	@Override
	public void add(String key, Object rs) {
		// do nothing
	}

	@Override
	public void clear(String tableCacheKey) {
		// do nothing
	}

}
