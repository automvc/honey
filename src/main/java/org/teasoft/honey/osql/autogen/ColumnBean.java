/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.autogen;

/**
 * @author Kingstar
 * @since  1.9.8
 */
public class ColumnBean {

	private String name;
	private String type;
	private Boolean ynnull; // 是否允许为空 yes or no null
	private Boolean ynkey; // 是否是主键 yes or no key
	private String label;// 标题,列名注释
	private String tablename;
	private String tablecomment;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getYnnull() {
		return ynnull;
	}

	public void setYnnull(Boolean ynnull) {
		this.ynnull = ynnull;
	}

	public Boolean getYnkey() {
		return ynkey;
	}

	public void setYnkey(Boolean ynkey) {
		this.ynkey = ynkey;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getTablename() {
		return tablename;
	}

	public void setTablename(String tablename) {
		this.tablename = tablename;
	}

	public String getTablecomment() {
		return tablecomment;
	}

	public void setTablecomment(String tablecomment) {
		this.tablecomment = tablecomment;
	}

}
