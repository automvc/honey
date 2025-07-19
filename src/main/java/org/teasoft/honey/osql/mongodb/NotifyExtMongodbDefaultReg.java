/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.mongodb;

import org.teasoft.honey.logging.Logger;

/**
 * @author Kingstar
 * @since  2.1
 */
public class NotifyExtMongodbDefaultReg {

	static void init() {
		try {
			Class.forName("org.teasoft.beex.mongodb.MongodbRegHandler");
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
	}

}
