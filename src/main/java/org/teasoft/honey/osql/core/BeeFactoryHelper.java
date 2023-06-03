/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.mongodb.MongodbRawSql;
import org.teasoft.bee.mvc.service.ObjSQLRichService;
import org.teasoft.bee.mvc.service.ObjSQLService;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.MapSql;
import org.teasoft.bee.osql.api.MapSuid;
import org.teasoft.bee.osql.api.MoreTable;
import org.teasoft.bee.osql.api.PreparedSql;
import org.teasoft.bee.osql.api.Suid;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.chain.UnionSelect;

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
	
	// @since 2.0
	public static UnionSelect getUnionSelect() {
		return BeeFactory.getHoneyFactory().getUnionSelect();
	}
	// @since 2.0
	public static Suid getSuidForMongodb() {
		return BeeFactory.getHoneyFactory().getSuidForMongodb();
	}
	// @since 2.0
	public static SuidRich getSuidRichForMongodb() {
		return BeeFactory.getHoneyFactory().getSuidRichForMongodb();
	}

	// @since 2.1
	public ObjSQLService getObjSQLService() {
		return BeeFactory.getHoneyFactory().getObjSQLService();
	}
	// @since 2.1
	public ObjSQLRichService getObjSQLRichService() {
		return BeeFactory.getHoneyFactory().getObjSQLRichService();
	}
	
//	// @since 2.1
//	public MongodbBeeSql getMongodbBeeSql() {
//		return BeeFactory.getHoneyFactory().getMongodbBeeSql();
//	}
	// @since 2.1
	public MongodbRawSql getMongodbRawSql() {
		return BeeFactory.getHoneyFactory().getMongodbRawSql();
	}

}
