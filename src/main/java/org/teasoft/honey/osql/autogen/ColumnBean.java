/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.autogen;

/**
 * @author Kingstar
 * @since  1.9.8
 * @since  2.4.2
 */
public class ColumnBean {

	private String col;  //old version is name
	private String type;
	private Boolean ynNull; //是否允许为空  yes or no null
	private Boolean ynKey; //是否是主键      yes or no key
	private String label;//标题,列名注释
	private String tablename;
	private String tablecomment;
	
	public String getCol() {
		return col;
	}

	public void setCol(String col) {
		this.col = col;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getYnNull() {
		return ynNull;
	}

	public void setYnNull(Boolean ynNull) {
		this.ynNull = ynNull;
	}

	public Boolean getYnKey() {
		return ynKey;
	}

	public void setYnKey(Boolean ynKey) {
		this.ynKey = ynKey;
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
