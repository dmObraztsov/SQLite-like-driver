// Generated from /Users/sofasidorenko/Documents/NSU/SQLite-like-driver/Simple-DB-Driver/src/main/antlr/SQL.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SQLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SQLVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SQLParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(SQLParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#createDBStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateDBStatement(SQLParser.CreateDBStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#dropDBStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropDBStatement(SQLParser.DropDBStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#useDBStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseDBStatement(SQLParser.UseDBStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#createTableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableStatement(SQLParser.CreateTableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#dropTableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTableStatement(SQLParser.DropTableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#alterTableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterTableStatement(SQLParser.AlterTableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#insertTableStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertTableStatement(SQLParser.InsertTableStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#alterAction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterAction(SQLParser.AlterActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#addColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddColumn(SQLParser.AddColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#dropColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropColumn(SQLParser.DropColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#renameColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameColumn(SQLParser.RenameColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#renameTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameTable(SQLParser.RenameTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#ifNotExists}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfNotExists(SQLParser.IfNotExistsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#notNull}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotNull(SQLParser.NotNullContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#primaryKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryKey(SQLParser.PrimaryKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn(SQLParser.ColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint(SQLParser.ConstraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(SQLParser.NameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#tablename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablename(SQLParser.TablenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLParser#data}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData(SQLParser.DataContext ctx);
}