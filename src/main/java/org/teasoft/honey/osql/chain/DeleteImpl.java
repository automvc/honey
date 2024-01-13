/*
 * Copyright 2020-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.chain.Delete;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  2.4.0
 */
public class DeleteImpl extends WhereImpl<Delete> implements Delete {

	public DeleteImpl() {

	}

	@Override
	public Delete delete(String table) {
		checkExpression(table);
		_appendTable(table);

		sql.append(K.delete).append(K.space).append(K.from).append(K.space);
		sql.append(table);

		return this;
	}

//	truncate table org_copy1;
	@Override
	public Delete truncate(String table) {
		checkExpression(table);
		_appendTable(table);

		sql.append(K.truncate).append(K.space).append(K.table).append(K.space);
		sql.append(table);

		return this;
	}

//	drop table org_copy1;
	@Override
	public Delete drop(String table) {
		checkExpression(table);
		_appendTable(table);

		drop();
		sql.append(table);

		return this;
	}

	@Override
	public Delete drop() {
		sql.append(K.drop).append(K.space).append(K.table).append(K.space);

		return this;
	}

	@Override
	public Delete IfExists() {
		sql.append(K.If).append(K.space).append(K.exists).append(K.space);
		return this;
	}

	@Override
	public Delete table(String table) {
		checkExpression(table);
		_appendTable(table);

		sql.append(table).append(K.space);
		return this;
	}

	private void _appendTable(String table) {
		super.appendTable(table);
	}

}

