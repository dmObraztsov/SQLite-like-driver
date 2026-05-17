package SqlParser.QueriesStruct;

import java.util.List;

public sealed interface WhereCondition
        permits WhereCondition.Simple, WhereCondition.And, WhereCondition.Or {

    record Simple(String column, String op, String value) implements WhereCondition {}
    record And(List<WhereCondition> operands) implements WhereCondition {}
    record Or(List<WhereCondition> operands) implements WhereCondition {}
}
