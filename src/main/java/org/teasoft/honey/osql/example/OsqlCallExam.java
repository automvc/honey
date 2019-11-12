package org.teasoft.honey.osql.example;

import java.sql.CallableStatement;
import java.sql.SQLException;

import org.teasoft.bee.osql.CallableSql;
import org.teasoft.honey.osql.core.CallableSqlLib;


public class OsqlCallExam {
	public static void main(String[] args)  throws SQLException{
		CallableSql callableSql=new CallableSqlLib();
//		String sql = "{call dbBorrow(?)}";
//		CallableStatement cstmt = conn.prepareCall(sql);
//		cstmt.setString(1, id);
		String callSql="dbBorrow(?)";
		int num=callableSql.modify(callSql, new Object[]{"5"});
		System.out.println(num);
		
//		CallableStatement
		CallableStatement cstmt=callableSql.getCallableStatement(callSql);
		cstmt.setString(1, "5");
		int num2=callableSql.modify(cstmt);
		System.out.println(num2);
		
		CallableStatement cstmt2=callableSql.getCallableStatement(callSql);
	}

}
