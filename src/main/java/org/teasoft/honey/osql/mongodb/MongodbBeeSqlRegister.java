/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.mongodb;

import org.teasoft.bee.mongodb.MongodbBeeSql;
import org.teasoft.bee.osql.Registry;

/**
 * @author Kingstar
 * @since  2.0
 */
public class MongodbBeeSqlRegister implements Registry {

	private static MongodbBeeSql mongodbBeeSql0 = null;

	public static void register(MongodbBeeSql mongodbBeeSql) {
		System.out.println("---------------------------------注册成功!");
		MongodbBeeSqlRegister.mongodbBeeSql0 = mongodbBeeSql;
	}

	public static MongodbBeeSql getInstance() {
		System.out.println("获取mongodbBeeSql0对象: "+mongodbBeeSql0);
		return mongodbBeeSql0;
	}

}
