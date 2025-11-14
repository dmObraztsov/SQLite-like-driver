package SqlParser.pars;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class SQLProcessor {
    public static QueryInterface getQuery()
    {
        String sql = "DROP DATABASE";

        CharStream input = CharStreams.fromString(sql);

        SQLLexer lexer = new SQLLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        SQLParser parser = new SQLParser(tokens);

        ParseTree tree = parser.query();

        SQLVisitor<QueryInterface> visitor = new AntlrParser();

        return visitor.visit(tree);
    }
}