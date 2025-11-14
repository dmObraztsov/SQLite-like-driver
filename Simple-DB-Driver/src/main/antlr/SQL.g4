grammar SQL;

query : createDBStatement
      | dropDBStatement
      | useDBStatement
      | createTableStatement
      | dropTableStatement;

createDBStatement : CREATE DATABASE ifNotExists? name;
dropDBStatement : DROP DATABASE name;
useDBStatement : USE DATABASE name;
createTableStatement : CREATE TABLE ifNotExists? name ('(' column (',' column)* ')')?;
dropTableStatement : DROP TABLE name;


ifNotExists : IF NOT EXISTS; //TODO
notNull : NOT NULL;
primaryKey : PRIMARY KEY;
column : name TYPE constraint*;
constraint : notNull | primaryKey | AUTOINCREMENT | UNIQUE | NULL | CHECK | DEFAULT;
name: ID;

CREATE : 'CREATE';
DROP : 'DROP';
USE : 'USE';

DATABASE : 'DATABASE';
TABLE : 'TABLE';

IF : 'IF';
NOT : 'NOT';
NULL : 'NULL';
EXISTS : 'EXISTS';
PRIMARY : 'PRIMARY';
KEY : 'KEY';
AUTOINCREMENT : 'AUTOINCREMENT';
UNIQUE : 'UNIQUE';
CHECK : 'CHECK';
DEFAULT : 'DEFAULT';

TYPE : 'INTEGER' | 'REAL' | 'TEXT' | 'BLOB'; //Null type need to be set?

ID : [a-zA-Z_][a-zA-Z_0-9]*;
WS : [ \t\r\n]+ -> skip;