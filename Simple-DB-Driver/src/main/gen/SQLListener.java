// Generated from /Users/sofasidorenko/Documents/NSU/SQLite-like-driver/Simple-DB-Driver/src/main/antlr/SQL.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SQLParser}.
 */
public interface SQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SQLParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(SQLParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(SQLParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#createDBStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateDBStatement(SQLParser.CreateDBStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#createDBStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateDBStatement(SQLParser.CreateDBStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#dropDBStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropDBStatement(SQLParser.DropDBStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#dropDBStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropDBStatement(SQLParser.DropDBStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#useDBStatement}.
	 * @param ctx the parse tree
	 */
	void enterUseDBStatement(SQLParser.UseDBStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#useDBStatement}.
	 * @param ctx the parse tree
	 */
	void exitUseDBStatement(SQLParser.UseDBStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#createTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableStatement(SQLParser.CreateTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#createTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableStatement(SQLParser.CreateTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#dropTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropTableStatement(SQLParser.DropTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#dropTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropTableStatement(SQLParser.DropTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#alterTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterTableStatement(SQLParser.AlterTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#alterTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterTableStatement(SQLParser.AlterTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#insertTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterInsertTableStatement(SQLParser.InsertTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#insertTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitInsertTableStatement(SQLParser.InsertTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#alterAction}.
	 * @param ctx the parse tree
	 */
	void enterAlterAction(SQLParser.AlterActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#alterAction}.
	 * @param ctx the parse tree
	 */
	void exitAlterAction(SQLParser.AlterActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#addColumn}.
	 * @param ctx the parse tree
	 */
	void enterAddColumn(SQLParser.AddColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#addColumn}.
	 * @param ctx the parse tree
	 */
	void exitAddColumn(SQLParser.AddColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#dropColumn}.
	 * @param ctx the parse tree
	 */
	void enterDropColumn(SQLParser.DropColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#dropColumn}.
	 * @param ctx the parse tree
	 */
	void exitDropColumn(SQLParser.DropColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#renameColumn}.
	 * @param ctx the parse tree
	 */
	void enterRenameColumn(SQLParser.RenameColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#renameColumn}.
	 * @param ctx the parse tree
	 */
	void exitRenameColumn(SQLParser.RenameColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#renameTable}.
	 * @param ctx the parse tree
	 */
	void enterRenameTable(SQLParser.RenameTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#renameTable}.
	 * @param ctx the parse tree
	 */
	void exitRenameTable(SQLParser.RenameTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#ifNotExists}.
	 * @param ctx the parse tree
	 */
	void enterIfNotExists(SQLParser.IfNotExistsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#ifNotExists}.
	 * @param ctx the parse tree
	 */
	void exitIfNotExists(SQLParser.IfNotExistsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#notNull}.
	 * @param ctx the parse tree
	 */
	void enterNotNull(SQLParser.NotNullContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#notNull}.
	 * @param ctx the parse tree
	 */
	void exitNotNull(SQLParser.NotNullContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#primaryKey}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryKey(SQLParser.PrimaryKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#primaryKey}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryKey(SQLParser.PrimaryKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#column}.
	 * @param ctx the parse tree
	 */
	void enterColumn(SQLParser.ColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#column}.
	 * @param ctx the parse tree
	 */
	void exitColumn(SQLParser.ColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#constraint}.
	 * @param ctx the parse tree
	 */
	void enterConstraint(SQLParser.ConstraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#constraint}.
	 * @param ctx the parse tree
	 */
	void exitConstraint(SQLParser.ConstraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(SQLParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(SQLParser.NameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#tablename}.
	 * @param ctx the parse tree
	 */
	void enterTablename(SQLParser.TablenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#tablename}.
	 * @param ctx the parse tree
	 */
	void exitTablename(SQLParser.TablenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SQLParser#data}.
	 * @param ctx the parse tree
	 */
	void enterData(SQLParser.DataContext ctx);
	/**
	 * Exit a parse tree produced by {@link SQLParser#data}.
	 * @param ctx the parse tree
	 */
	void exitData(SQLParser.DataContext ctx);
}