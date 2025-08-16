package org.teasoft.honey.osql.serviceimpl;

import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.SuidRich;
import org.teasoft.bee.osql.service.ObjSQLRichAbstractServiceImpl;
import org.teasoft.honey.osql.core.BeeFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjSQLRichServiceImpl extends ObjSQLRichAbstractServiceImpl {

//	private Suid suid;
	private SuidRich suidRich;

	public ObjSQLRichServiceImpl() {}

//	public void setSuid(Suid suid) {
//		this.suid = suid;
//	}

	@Override
	public Suid getSuid() { // 为什么要保留这个,因为不能用多继承,不能从ObjSQLServiceImpl继承.
//		if(suid==null) return BeeFactory.getHoneyFactory().getSuid();
//		return suid; //V2.1 fixed bug.   引发与spring整合时,多生成一个实例.
//		@Autowired
//		ObjSQLRichService objSQLRichService;  //会生成两个对象,导致setDataSourceName("ds0")设置的,与获取的,不是一个对象.

		return getSuidRich();
	}

	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}

	@Override
	public SuidRich getSuidRich() {
		if (suidRich == null) return BeeFactory.getHoneyFactory().getSuidRich();
		return suidRich;
	}
}
