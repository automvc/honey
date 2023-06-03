/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.spi;

import org.teasoft.bee.spi.PreLoad;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.interccept.InterceptorChainRegistry;
import org.teasoft.honey.osql.interccept.annotation.CustomAutoSetInterceptor;
import org.teasoft.honey.osql.interccept.annotation.CustomInterceptor;
import org.teasoft.honey.sharding.ShardingInterceptor;

/**
 * 提前预加载初始化
 * @author Kingstar
 * @since  1.11-E
 */
public class PreLoadInit implements PreLoad{
	
	static {
		Logger.info("[Bee] ========= Preload class PreLoadInit, load...");
		init();
	}
	
	private static void init() {
		InterceptorChainRegistry.addInterceptor(new CustomAutoSetInterceptor());//添加定制拦截器.
		InterceptorChainRegistry.addInterceptor(new ShardingInterceptor()); //分片拦截器
		InterceptorChainRegistry.addInterceptor(new CustomInterceptor()); //添加定制拦截器.
	}
}
