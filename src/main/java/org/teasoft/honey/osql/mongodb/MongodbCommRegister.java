/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.mongodb;

import org.teasoft.bee.osql.Registry;

/**
 * @author Kingstar
 * @since  2.0
 */
public class MongodbCommRegister implements Registry {

	private static MongodbComm comm = null;

	static {
		NotifyExtMongodbDefaultReg.init();
	}

	public static void register(MongodbComm mongodbComm) {
		MongodbCommRegister.comm = mongodbComm;
	}

	public static MongodbComm getInstance() {
		return comm;
	}

}
