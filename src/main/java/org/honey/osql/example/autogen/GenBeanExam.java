package org.honey.osql.example.autogen;

import java.io.IOException;

import org.honey.osql.atuogen.GenBean;
import org.honey.osql.atuogen.GenConfig;
import org.honey.osql.core.HoneyConfig;

/**
 * @author Kingstar
 * @since  1.0
 */
public class GenBeanExam {
		public static void main(String[] args) throws IOException {

			
			String dbName=HoneyConfig.getHoneyConfig().getDbName();
//			driverName,url,username,password config in bee.properties.

			GenConfig config = new GenConfig();

			config.setDbName(dbName);
			
			config.setGenToString(true);
			config.setGenSerializable(true);
			
			config.setBaseDir("D:\\JavaWeb\\workspaceGit\\Honey\\src\\main\\java\\");
//			                   D:\JavaWeb\workspaceGit\Honey\src\main\java\org\honey\osql\example\entity
			config.setPackagePath("org.honey.osql.example.entity");

			GenBean genBean = new GenBean(config);
			
//			genBean.genAllBeanFile();
//			genBean.genSomeBeanFile("orders,items");//只创建部分表对应的JavaBean
			genBean.genSomeBeanFile("orders");
//			genBean.genSomeBeanFile("ordersView");
	}
}
