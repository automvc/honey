/*
 * Copyright 2020-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

import java.util.ArrayList;
import java.util.List;

import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  2.5.2
 */
public class KeyWord {

	private final static String[] key_work = { "select", "from", "where", "group", "by", "having", "order", "by", "limit",
			"offset", "insert", "into", "values", "update", "set", "delete", "truncate", "create", "alter", "drop",
			"rename", "table", "view", "index", "sequence", "join", "inner", "join", "left", "join", "right", "join",
			"full", "join", "cross", "join", "union", "union", "all", "intersect", "except", "minus", "distinct", "count",
			"sum", "avg", "min", "max", "and", "or", "not", "in", "between", "like", "is", "null", "is", "not", "null",
			"primary", "key", "foreign", "key", "unique", "check", "default", "auto_increment", "commit", "rollback",
			"savepoint", "begin", "transaction", "grant", "revoke", "deny", "with", "cascade", "constraint" };

	// 1. MySQL
	private final static String[] mysql_keywords = { "engine", "auto_increment", "charset", "collate", "storage",
			"partition", "fulltext", "spatial", "temporary", "if", "not", "exists", "if", "exists", "delayed", "ignore",
			"low_priority", "high_priority", "sql_cache", "sql_no_cache" };

	// 2. Oracle
	private final static String[] oracle_keywords = { "dual", "rownum", "rowid", "nvl", "nvl2", "decode", "connect", "by",
			"start", "with", "prior", "level", "sysdate", "systimestamp", "sequence", "flashback", "purge", "within",
			"group", "over", "partition", "by", "model" };

	// 4. MariaDB
	private final static String[] mariadb_keywords = { "engine", "auto_increment", "charset", "collate", "storage",
			"partition", "fulltext", "spatial", "temporary", "if", "not", "exists", "if", "exists", "delayed", "ignore",
			"low_priority", "high_priority", "sql_cache", "sql_no_cache" };
	// 5. H2
	private final static String[] h2_keywords = { "cache", "group_concat", "merge", "offset", "rownum", "top", "truncate",
			"value" };

	// 6. SQLite
	private final static String[] sqlite_keywords = { "autoincrement", "conflict", "fail", "ignore", "replace",
			"rollback", "abort", "without", "rowid", "vacuum", "attach", "detach", "temp", "temporary" };

	// 7. PostgreSQL
	private final static String[] postgresql_keywords = { "ilike", "similar", "to", "distinct", "on", "returning",
			"serial", "bigserial", "smallserial", "with", "oids", "inherits", "like", "including", "excluding", "with",
			"time", "zone", "without", "time", "zone" };

	// 8. MS Access
	private final static String[] msaccess_keywords = { "top", "distinctrow", "transform", "pivot", "param", "declare",
			"database", "workspace", "dbengine", "currentdb", "currentuser", "currentproject" };

	// 9. 金仓 (Kingbase)
	private final static String[] kingbase_keywords = { "with", "oids", "inherits", "like", "including", "excluding",
			"serial", "bigserial", "smallserial", "returning", "ilike", "similar", "to" };

	// 10. 达梦 (DM)
	private final static String[] dm_keywords = { "dual", "rownum", "rowid", "nvl", "nvl2", "decode", "connect", "by",
			"start", "with", "prior", "level", "sysdate", "systimestamp", "sequence" };

	// 11. OceanBase
	private final static String[] oceanbase_keywords = { "dual", "rownum", "rowid", "nvl", "nvl2", "decode", "connect",
			"by", "start", "with", "prior", "level", "sysdate", "systimestamp", "sequence" };
	
//	naming_SqlKeyWordInColumn  
//	define for append if bee do not contain them.
	private static List<String> appendKeyWord = new ArrayList<>();
	static {
		String sqlKeyWordInColumn = HoneyConfig.getHoneyConfig().naming_SqlKeyWordInColumn;
		if (StringUtils.isNotBlank(sqlKeyWordInColumn)) {
			String t[] = sqlKeyWordInColumn.split(",");
			for (int i = 0; i < t.length; i++) {
				String kw = t[i];
				if (StringUtils.isNotBlank(kw)) appendKeyWord.add(kw.trim());
			}
		}
	}

	public static boolean isKeyWord(String name) {
		if (name == null) return false;

		for (int i = 0; i < key_work.length; i++) {
			String kw = key_work[i];
			if (kw.equals(name.toLowerCase())) return true;
		}

		for (String kw : appendKeyWord) {
			if (kw.equals(name.toLowerCase())) return true;
		}

		return false;
	}
	
	static String transformNameIfKeyWork(String name) {

		boolean allowKeyWordInColumn = HoneyConfig.getHoneyConfig().naming_allowKeyWordInColumn;
		if (!allowKeyWordInColumn) return name;

		if (isKeyWord(name))
			return "`" + name + "`";
		else
			return name;
	}
}
