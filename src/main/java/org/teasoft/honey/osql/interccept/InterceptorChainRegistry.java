/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.interccept;

import org.teasoft.bee.osql.Registry;
import org.teasoft.bee.osql.interccept.Interceptor;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.osql.core.HoneyUtil;

/**
 * 注册全局用的拦截器链.Register interceptor chain for global.
 * @author Kingstar
 * @since  1.11
 */
public class InterceptorChainRegistry implements Registry {

	private static InterceptorChain interceptorChain = new DefaultInterceptorChain();
	
	public static InterceptorChain getInterceptorChain() {
		return HoneyUtil.copy(interceptorChain);
	}

	public static void register(InterceptorChain interceptorChain) {
		InterceptorChainRegistry.interceptorChain = interceptorChain;
	}
	
	public static void addInterceptor(Interceptor interceptor) {
		InterceptorChainRegistry.interceptorChain.addInterceptor(interceptor);
	}
	
}
