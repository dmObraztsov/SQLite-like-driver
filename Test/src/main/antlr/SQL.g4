grammar SQL;

query : createDBStatement
      | dropDBStatement;

createDBStatement : CREATE DATABASE tableName;
dropDBStatement : DROP DATABASE tableName;


DROP : 'DROP';
CREATE : 'CREATE' ;
DATABASE : 'DATABASE' ;

tableName: STRING;

STRING : [a-zA-Z_][a-zA-Z_0-9]* ;
WS : [ \t\r\n]+ -> skip ;