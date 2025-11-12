grammar SQL;

query : createDBStatement
      | dropDBStatement
      | useDBStatement
      | createTableStatement;

createDBStatement : CREATE DATABASE name;
dropDBStatement : DROP DATABASE name;
useDBStatement : USE DATABASE name;
createTableStatement : CREATE TABLE name;

CREATE : 'CREATE' ;
DROP : 'DROP' ;
USE : 'USE' ;
DATABASE : 'DATABASE' ;
TABLE : 'TABLE';

name: STRING;

STRING : [a-zA-Z_][a-zA-Z_0-9]* ;
WS : [ \t\r\n]+ -> skip ;