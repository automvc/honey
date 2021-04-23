/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.MapSql;
import org.teasoft.bee.osql.MapSuid;
import org.teasoft.bee.osql.MoreTable;
import org.teasoft.bee.osql.PreparedSql;
import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.SuidRich;

/**
 * 获取接口相应对象的帮助类.Helper Class for get the corresponding object of the interface.
 * @author Kingstar
 * @since  1.9
 */
public class BeeFactoryHelper {
	
	
	public static Suid getSuid() {
		return BeeFactory.getHoneyFactory().getSuid();
	}
	
	public static SuidRich getSuidRich() {
		return BeeFactory.getHoneyFactory().getSuidRich();
	}
	
	public static Condition getCondition() {
        return BeeFactory.getHoneyFactory().getCondition();
	}
	
	public static MoreTable getMoreTable() {
		return BeeFactory.getHoneyFactory().getMoreTable();
	}
	
	public static PreparedSql getPreparedSql() {
		return BeeFactory.getHoneyFactory().getPreparedSql();
	}
	
	public static MapSuid getMapSuid() {
		return BeeFactory.getHoneyFactory().getMapSuid();
	}
	
	public static MapSql getMapSql() {
		return BeeFactory.getHoneyFactory().getMapSql();
	}

}
