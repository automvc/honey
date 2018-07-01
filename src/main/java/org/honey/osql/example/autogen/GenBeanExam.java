package org.honey.osql.example.autogen;

import java.io.IOException;

import org.honey.osql.atuogen.GenBean;
import org.honey.osql.atuogen.GenConfig;
import org.honey.osql.core.HoneyConfig;

/**
 * @author KingStar
 * @since  1.0
 */
public class GenBeanExam {
		public static void main(String[] args) throws IOException {
			// MySQL
			String driverName = "com.mysql.jdbc.Driver";
			String url = "jdbc:mysql://localhost:3306/bee?characterEncoding=UTF-8";
			String username = "root";
			String password = "";
			
			String dbName=HoneyConfig.getHoneyConfig().getDbName();
			
			// SQL Server
			// String driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
			// String url = "jdbc:sqlserver://localhost:1433";
			// String username = "sa";
			// String password = "";

			// Oracle
			// String driverName = "oracle.jdbc.driver.OracleDriver";
			// String url = "";
			// String username = "";
			// String password = "";

			GenConfig config = new GenConfig();
			config.setDriverName(driverName);
			config.setUrl(url);
			config.setUsername(username);
			config.setPassword(password);
			config.setDbName(dbName);
			
			config.setGenToString("true");
			
			config.setBaseDir("D:\\JavaWeb\\workspace\\Honey\\src\\main\\java\\");
//			                   D:\JavaWeb\workspace\Honey\src\main\java\org\honey\osql\example\entity
			config.setPackagePath("org.honey.osql.example.entity");

			GenBean genBean = new GenBean(config);
			
//			genBean.genAllBeanFile();
//			genBean.genSomeBeanFile("orders,items");//只创建部分表对应的JavaBean
			genBean.genSomeBeanFile("orders");
			
	}
}
