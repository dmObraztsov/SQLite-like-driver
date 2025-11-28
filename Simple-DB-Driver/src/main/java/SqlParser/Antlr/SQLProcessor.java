package SqlParser.Antlr;

import SqlParser.QueriesStruct.QueryInterface;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class SQLProcessor {
    public static QueryInterface getQuery(String sql)
    {
        CharStream input = CharStreams.fromString(sql);
        SQLLexer lexer = new SQLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SQLParser parser = new SQLParser(tokens);

        ParseTree tree = parser.query();

        if(parser.getNumberOfSyntaxErrors() > 0)
        {
            return null;
        }

        SQLVisitor<QueryInterface> visitor = new AntlrParser();

        return visitor.visit(tree);
    }
}