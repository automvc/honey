/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import org.teasoft.bee.distribution.Worker;
import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * @author Kingstar
 * @since  1.8
 */
public class DefaultWorker implements Worker {

	@Override
	public long getWorkerId() {
		int workerid = HoneyConfig.getHoneyConfig().genid_workerid;
		if (workerid < 0 || workerid > 1023) throw new ConfigWrongException(" workerid is wrong, need in [0,1023]");
		return workerid;
	}

}
