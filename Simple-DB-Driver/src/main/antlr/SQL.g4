grammar SQL;

query : createDBStatement;

createDBStatement : CREATE DATABASE tableName;

CREATE : 'CREATE' ;
DATABASE : 'DATABASE' ;

tableName: STRING;

STRING : [a-zA-Z_][a-zA-Z_0-9]* ;
WS : [ \t\r\n]+ -> skip ;