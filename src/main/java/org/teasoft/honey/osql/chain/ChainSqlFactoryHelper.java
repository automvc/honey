/*
 * Copyright 2019-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.chain.Delete;
import org.teasoft.bee.osql.chain.Insert;
import org.teasoft.bee.osql.chain.Select;
import org.teasoft.bee.osql.chain.UnionSelect;
import org.teasoft.bee.osql.chain.Update;

/**
 * @author Kingstar
 * @since  2.4.0
 */
public class ChainSqlFactoryHelper {

	private static ChainSqlFactory factory;
	static {
		factory = new ChainSqlFactory();
	}

	public static Select getSelect() {
		return factory.getSelect();
	}

	public static Update getUpdate() {
		return factory.getUpdate();
	}

	public static Insert getInsert() {
		return factory.getInsert();
	}

	public static Delete getDelete() {
		return factory.getDelete();
	}

	public static UnionSelect getUnionSelect() {
		return factory.getUnionSelect();
	}

}
