package SqlParser;

import SqlParser.pars.SQLProcessor;

public class Main {
    public static void main(String[] args)
    {
        System.out.println(SQLProcessor.getQuery().getStringVision());
    }
}
