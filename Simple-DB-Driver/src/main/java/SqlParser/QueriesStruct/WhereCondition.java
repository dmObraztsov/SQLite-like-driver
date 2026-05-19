package SqlParser.QueriesStruct;

import java.util.List;

public sealed interface WhereCondition
        permits WhereCondition.Simple, WhereCondition.And, WhereCondition.Or,
                WhereCondition.IsNull, WhereCondition.In, WhereCondition.Between {

    record Simple(String column, String op, String value) implements WhereCondition {}
    record And(List<WhereCondition> operands) implements WhereCondition {}
    record Or(List<WhereCondition> operands) implements WhereCondition {}
    record IsNull(String column, boolean isNull) implements WhereCondition {}
    record In(String column, List<String> values, boolean negated) implements WhereCondition {}
    record Between(String column, String low, String high) implements WhereCondition {}
}
