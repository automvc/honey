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
public class ChainSqlFactory {

	private Select select;
	private Update update;
	private Insert insert;
	private Delete delete;

	private UnionSelect unionSelect;

	public Select getSelect() {
		if (select == null) return new SelectImpl();
		return select;
	}

	public void setSelect(Select select) {
		this.select = select;
	}

	public Update getUpdate() {
		if (update == null) return new UpdateImpl();
		return update;
	}

	public void setUpdate(Update update) {
		this.update = update;
	}

	public Insert getInsert() {
		if (insert == null) return new InsertImpl();
		return insert;
	}

	public void setInsert(Insert insert) {
		this.insert = insert;
	}

	public Delete getDelete() {
		if (delete == null) return new DeleteImpl();
		return delete;
	}

	public void setDelete(Delete delete) {
		this.delete = delete;
	}

	public UnionSelect getUnionSelect() {
		if (unionSelect == null) return new UnionSelectImpl();
		return unionSelect;
	}

	public void setUnionSelect(UnionSelect unionSelect) {
		this.unionSelect = unionSelect;
	}

}
