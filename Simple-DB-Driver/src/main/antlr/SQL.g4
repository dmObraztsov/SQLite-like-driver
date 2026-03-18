grammar SQL;

query
    : createDBStatement
    | dropDBStatement
    | useDBStatement
    | createTableStatement
    | dropTableStatement
    | alterTableStatement
    | insertTableStatement
    | selectStatement
    | deleteStatement
    | beginTransactionStatement
    | commitStatement
    | rollbackStatement
    ;

createDBStatement : CREATE DATABASE ifNotExists? identifier;
dropDBStatement : DROP DATABASE identifier;
useDBStatement : USE DATABASE identifier;

createTableStatement : CREATE TABLE ifNotExists? identifier LPAREN columnDef (COMMA columnDef)* RPAREN;

dropTableStatement : DROP TABLE identifier;

alterTableStatement : ALTER TABLE name alterAction;

alterAction
    : addColumn
    | dropColumn
    | renameColumn
    | renameTable
    ;

addColumn    : ADD COLUMN column;
dropColumn   : DROP COLUMN name;
renameColumn : RENAME COLUMN name TO name;
renameTable  : RENAME TO name;

insertTableStatement :
    INSERT INTO identifier (LPAREN identifier (COMMA identifier)* RPAREN)?
    VALUES LPAREN literal (COMMA literal)* RPAREN;

selectStatement : SELECT selectCols FROM tablename joinClause* whereClause?;

deleteStatement : DELETE FROM tablename whereClause?;

selectCols
    : STAR
    | columnRef (COMMA columnRef)*
    ;

joinClause : JOIN tablename ON condition;
whereClause : WHERE condition;

condition : orCondition;
orCondition : andCondition (OR andCondition)*;
andCondition : predicate (AND predicate)*;
predicate
    : LPAREN condition RPAREN
    | operand comparisonOperator operand
    ;

operand
    : columnRef
    | literal
    ;

comparisonOperator
    : EQ
    | NE
    | GT
    | LT
    | GE
    | LE
    ;

tablename : identifier;

columnDef : identifier dataType columnConstraint*;

column : name dataType constraint*;

columnRef : identifier (DOT identifier)?;

columnConstraint
    : notNullConstraint
    | primaryKeyConstraint
    | autoIncrementConstraint
    | uniqueConstraint
    | nullConstraint
    | checkConstraint
    | defaultConstraint
    ;

notNullConstraint      : NOT NULL;
primaryKeyConstraint   : PRIMARY KEY;
autoIncrementConstraint: AUTOINCREMENT;
uniqueConstraint       : UNIQUE;
nullConstraint         : NULL;
checkConstraint        : CHECK LPAREN condition RPAREN;
defaultConstraint      : DEFAULT literal;

constraint
    : NOT NULL
    | PRIMARY KEY
    | AUTOINCREMENT
    | UNIQUE
    | CHECK
    | DEFAULT
    ;

ifNotExists : IF NOT EXISTS;

beginTransactionStatement : BEGIN TRANSACTION?;
commitStatement           : COMMIT;
rollbackStatement         : ROLLBACK;

name       : identifier;
identifier : NAME;

literal
    : NUMBER
    | STRING
    | NULL
    ;

dataType
    : INTEGER
    | REAL
    | TEXT
    | BLOB
    ;

// ── Keywords ──────────────────────────────────────────────
SELECT      : 'SELECT';
FROM        : 'FROM';
WHERE       : 'WHERE';
JOIN        : 'JOIN';
DELETE      : 'DELETE';
ON          : 'ON';

CREATE      : 'CREATE';
DROP        : 'DROP';
USE         : 'USE';
ALTER       : 'ALTER';
ADD         : 'ADD';
COLUMN      : 'COLUMN';
RENAME      : 'RENAME';
TO          : 'TO';
INSERT      : 'INSERT';
INTO        : 'INTO';
VALUES      : 'VALUES';

DATABASE    : 'DATABASE';
TABLE       : 'TABLE';
TRANSACTION : 'TRANSACTION';
BEGIN       : 'BEGIN';
COMMIT      : 'COMMIT';
ROLLBACK    : 'ROLLBACK';

IF          : 'IF';
NOT         : 'NOT';
NULL        : 'NULL';
EXISTS      : 'EXISTS';
PRIMARY     : 'PRIMARY';
KEY         : 'KEY';
AUTOINCREMENT: 'AUTOINCREMENT';
UNIQUE      : 'UNIQUE';
CHECK       : 'CHECK';
DEFAULT     : 'DEFAULT';
AND         : 'AND';
OR          : 'OR';

INTEGER     : 'INTEGER';
REAL        : 'REAL';
TEXT        : 'TEXT';
BLOB        : 'BLOB';

// ── Operators ─────────────────────────────────────────────
EQ  : '=';
NE  : '!=' | '<>';
GE  : '>=';
LE  : '<=';
GT  : '>';
LT  : '<';

// ── Punctuation ───────────────────────────────────────────
STAR   : '*';
DOT    : '.';
COMMA  : ',';
LPAREN : '(';
RPAREN : ')';

// ── Literals & whitespace ─────────────────────────────────
NAME   : [a-zA-Z_][a-zA-Z_0-9]*;
STRING : '"' (~["\\] | '\\' .)* '"';
NUMBER : [0-9]+ ('.' [0-9]+)?;
WS     : [ \t\r\n]+ -> skip;