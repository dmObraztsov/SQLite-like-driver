grammar SQL;

query : createDBStatement
      | dropDBStatement
      | useDBStatement
      | createTableStatement;

createDBStatement : CREATE DATABASE name;
dropDBStatement : DROP DATABASE name;
useDBStatement : USE DATABASE name;
createTableStatement : CREATE TABLE name '(' column (',' column)* ')';

column : name TYPE constraint*;
constraint : NOT_NULL | PRIMARY KEY | AUTOINCREMENT | UNIQUE | NULL | CHECK;
name: ID;

CREATE : 'CREATE';
DROP : 'DROP';
USE : 'USE';

DATABASE : 'DATABASE';
TABLE : 'TABLE';

NOT_NULL : 'NOT' NULL;
NULL : 'NULL';
PRIMARY : 'PRIMARY';
KEY : 'KEY';
AUTOINCREMENT : 'AUTOINCREMENT';
UNIQUE : 'UNIQUE';
CHECK : 'CHECK';

TYPE : 'INTEGER' | 'REAL' | 'TEXT' | 'BLOB'; //Null type need to be set?

ID : [a-zA-Z_][a-zA-Z_0-9]*;
WS : [ \t\r\n]+ -> skip;