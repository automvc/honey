/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.dialect.sqlserver;

import org.teasoft.bee.osql.OrderType;

/**
 * @author Kingstar
 * @since 1.17
 */
public class SqlServerPagingStruct {

	private String orderColumn;
	private OrderType orderType=OrderType.ASC;
	private boolean hasOrderBy; //用于>=2012版本,order by id offset语法
	private boolean justChangeOrderColumn;

	public String getOrderColumn() {
		return orderColumn;
	}

	public void setOrderColumn(String orderColumn) {
		this.orderColumn = orderColumn;
	}

	public OrderType getOrderType() {
		return orderType;
	}

	public void setOrderType(OrderType orderType) {
		this.orderType = orderType;
	}

	public boolean isHasOrderBy() {
		return hasOrderBy;
	}

	public void setHasOrderBy(boolean hasOrderBy) {
		this.hasOrderBy = hasOrderBy;
	}

	public boolean isJustChangeOrderColumn() {
		return justChangeOrderColumn;
	}

	public void setJustChangeOrderColumn(boolean justChangeOrderColumn) {
		this.justChangeOrderColumn = justChangeOrderColumn;
	}

}
