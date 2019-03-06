package org.honey.osql.example.chain;

import org.bee.osql.BeeException;
import org.bee.osql.Op;
import org.bee.osql.chain.Update;
import org.honey.osql.chain.UpdateImpl;

/**
 * @author Kingstar
 * @since  1.3
 */
public class UpdateExam {
	public static void main(String[] args) {
	 try{
		Update updateSql=new UpdateImpl();
		updateSql.update("orders")
		.set("name", "bee")
		.set("userid", "bee")
		.where()
		.op("id", Op.eq, 100001)
		;
		
		System.out.println(updateSql.toSQL());
	   } catch (BeeException e) {
			e.printStackTrace();
	   }
	}

}
