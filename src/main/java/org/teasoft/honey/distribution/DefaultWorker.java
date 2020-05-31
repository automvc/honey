/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import org.teasoft.bee.distribution.Worker;

/**
 * @author Kingstar
 * @since  1.7.3
 */
public class DefaultWorker implements Worker{

	@Override
	public long getWorkerId() {
		
		//TODO  从配置文件中读取workid.
		
		return 1;
	}
	
}
