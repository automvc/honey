/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.mongodb;

import java.util.Map;
import java.util.Set;

/**
 * @author Kingstar
 * @since  2.0
 */
public interface MongodbComm {

	public Set<Map.Entry<String, Object>> getCollectStrcut(String collectionName);

	public String[] getAllCollectionNames();
}
