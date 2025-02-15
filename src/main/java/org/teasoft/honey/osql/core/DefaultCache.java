/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;

import org.teasoft.bee.osql.Cache;

/**
 * 默认的缓存实现.Default Bee Cache.
 * @author Kingstar
 * @since  1.4
 */
public class DefaultCache implements Cache, Serializable {

	private static final long serialVersionUID = 1596710362260L;

	@Override
	public Object get(String sql) {
		return CacheUtil.get(sql);
	}

	@Override
	public void add(String sql, Object result) {
		CacheUtil.add(sql, result);
	}

	@Override
	public void clear(String sql) {
		CacheUtil.clear(sql);
	}

}
