grammar SQL;

query : createDBStatement
      | dropDBStatement
      | useDBStatement
      | createTableStatement
      | dropTableStatement
      | alterTableStatement;

createDBStatement : CREATE DATABASE ifNotExists? name;
dropDBStatement : DROP DATABASE name;
useDBStatement : USE DATABASE name;
createTableStatement : CREATE TABLE ifNotExists? name ('(' column (',' column)* ')')?;
dropTableStatement : DROP TABLE name;
alterTableStatement : ALTER TABLE name alterAction;

alterAction : addColumn
            | dropColumn
            | renameColumn
            | renameTable;

addColumn : ADD COLUMN column;
dropColumn : DROP COLUMN name;
renameColumn : RENAME COLUMN name TO name;
renameTable : RENAME TO name;

ifNotExists : IF NOT EXISTS;
notNull : NOT NULL;
primaryKey : PRIMARY KEY;
column : name TYPE constraint*;
constraint : notNull | primaryKey | AUTOINCREMENT | UNIQUE | NULL | CHECK | DEFAULT;
name: ID;

CREATE : 'CREATE';
DROP : 'DROP';
USE : 'USE';
ALTER : 'ALTER';
ADD : 'ADD';
COLUMN : 'COLUMN';
RENAME : 'RENAME';
TO : 'TO';

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

TYPE : 'INTEGER' | 'REAL' | 'TEXT' | 'BLOB';

ID : [a-zA-Z_][a-zA-Z_0-9]*;
WS : [ \t\r\n]+ -> skip;