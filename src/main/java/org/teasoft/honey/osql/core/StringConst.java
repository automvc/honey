/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

/**
 * String Const.
 * @author Kingstar
 * @since  1.9
 */
public final class StringConst {
	
	private StringConst() {}

	public static final String tRue = "tRue";
	public static final String PREFIX="_SYS_Bee_";
	public static final String TABLE_NAME = "_SYS_Bee_TableName";
	public static final String DoNotCheckAnnotation = "_SYS_Bee_DoNotCheckAnnotation";
	public static final String ALREADY_SET_ROUTE = "_SYS_Bee_ALREADY_SET_ROUTE";
	public static final String Select_Fun = "_SYS_Bee_Select_Fun";
	public static final String MoreStruct_to_SqlLib = "_SYS_Bee_MoreStruct_to_SqlLib";

	public static final String OLD_ID_EXIST = "_SYS_Bee_OLD_ID_FOR_AUTO_ID_EXIST";
	public static final String OLD_ID = "_SYS_Bee_OLD_ID_FOR_AUTO_ID";
	
	public static final String Primary_Key_Name = "_SYS_Bee_PK_NAME";//PK:Primary_Key
	public static final String PK_Name_For_ReturnId = "_SYS_Bee_PK_Name_For_InsertAndReturnId"; //PK:Primary_Key

	public static final String SUBENTITY_FIRSTANNOTATION_FIELD = "_SYS_Bee_subEntityFirstAnnotationField";
	
	public static final String Reset_Ds_OneTime="_SYS_Bee_Reset_Ds_OneTime";
	
	public static final String InterceptorChainForMoreTable="_SYS_Bee_InterceptorChainForMoreTable";
	
	public static final String JdbcTranWriterDS="_SYS_Bee_JdbcTran_WriterDS";
	
	public static final String MapSuid_Insert_Has_ID="_SYS_Bee_MapSuid_Insert_Has_ID";
	public static final String Column2Field="_SYS_Bee_Column2Field:";
	
	public static final String Route_EC="_SYS_Bee_ROUTE_EC"; ////EC:Entity Class
	public static final String Column_EC="_SYS_Bee_COLUMN_EC";
	
	public static final String SchemaName="_SYS_Bee_SchemaName";
	public static final String SuidType="_SYS_Bee_SuidType"; //V1.17

	
	public static final String HadSharding=PREFIX+"HadSharding";
	
//	public static final String ShardingTableIndexStr="##(index)##";
	public static final String ShardingTableIndexStr="[$#(index)#$]";
	
	public static final String DsNameListLocal=PREFIX+"DsNameListLocal";
	public static final String TabNameListLocal=PREFIX+"TabNameListLocal";
	public static final String TabSuffixListLocal=PREFIX+"TabSuffixListLocal";
	
	
	public static final String ShardingFullSelect=PREFIX+"ShardingFullSelect";
	public static final String ShardingSomeDsFullSelect=PREFIX+"ShardingSomeDsFullSelect";
	public static final String ShardingTab2DsMap=PREFIX+"ShardingTab2DsMap";
	
	public static final String HintDsTab=PREFIX+"HintDsTab";
	public static final String FunType=PREFIX+"FunType";
	
	public static final String DsNameListForBatchLocal=PREFIX+"DsNameListForBatchLocal";
	public static final String TabNameListForBatchLocal=PREFIX+"TabNameListForBatchLocal";
	
	public static final String MoreTableQuery=PREFIX+"MoreTableQuery";
	
	public static final String InterceptorSubEntity=PREFIX+"InterceptorSubEntity";
	
	public static final String ShardingBatchInsertDoing=PREFIX+"ShardingBatchInsertDoing";
	
	public static final String  Check_Group_ForSharding=PREFIX+"Check_Group_ForSharding";
	public static final String  Get_GroupFunStruct=PREFIX+"Get_GroupFunStruct";
	public static final String  Return_GroupFunStruct=PREFIX+"Return_GroupFunStruct";
}
