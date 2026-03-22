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
    | updateStatement
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

selectStatement : SELECT DISTINCT? selectCols FROM tablename joinClause* whereClause?;

deleteStatement : DELETE FROM tablename whereClause?;
updateStatement : UPDATE tablename SET updateAssignment (COMMA updateAssignment)* whereClause?;

selectCols : STAR | columnRef (COMMA columnRef)*;

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

updateAssignment : columnRef EQ operand;

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

SELECT       : [sS] [eE] [lL] [eE] [cC] [tT];
DISTINCT     : [dD] [iI] [sS] [tT] [iI] [nN] [cC] [tT];
FROM         : [fF] [rR] [oO] [mM];
WHERE        : [wW] [hH] [eE] [rR] [eE];
JOIN         : [jJ] [oO] [iI] [nN];
DELETE       : [dD] [eE] [lL] [eE] [tT] [eE];
UPDATE : [uU] [pP] [dD] [aA] [tT] [eE];
SET : [sS] [eE] [tT];
ON           : [oO] [nN];

CREATE      : [cC] [rR] [eE] [aA] [tT] [eE];
DROP        : [dD] [rR] [oO] [pP];
USE         : [uU] [sS] [eE];
ALTER       : [aA] [lL] [tT] [eE] [rR];
ADD         : [aA] [dD] [Dd];
COLUMN      : [cC] [oO] [lL] [uU] [mM] [nN];
RENAME      : [rR] [eE] [nN] [aA] [mM] [eE];
TO          : [tT] [oO];
INSERT      : [iI] [nN] [sS] [eE] [rR] [tT];
INTO        : [iI] [nN] [tT] [oO];
VALUES      : [vV] [aA] [lL] [uU] [eE] [sS];

DATABASE    : [dD] [aA] [tT] [aA] [bB] [aA] [sS] [eE];
TABLE       : [tT] [aA] [bB] [lL] [eE];
TRANSACTION : [tT] [rR] [aA] [nN] [sS] [aA] [cC] [tT] [iI] [oO] [nN];
BEGIN       : [bB] [eE] [gG] [iI] [nN];
COMMIT      : [cC] [oO] [mM] [mM] [iI] [tT];
ROLLBACK    : [rR] [oO] [lL] [lL] [bB] [aA] [cC] [kK];

IF          : [iI] [fF];
NOT         : [Nn] [Oo] [Tt];
NULL        : [nN] [uU] [lL] [lL];
EXISTS      : [eE] [xX] [iI] [sS] [tT] [sS];
PRIMARY     : [pP] [rR] [iI] [mM] [aA] [rR] [yY];
KEY         : [kK] [eE] [yY];
AUTOINCREMENT: [aA] [uU] [tT] [oO] [iI] [nN] [cC] [rR] [eE] [mM] [eE] [nN] [tT];
UNIQUE      : [uU] [nN] [iI] [qQ] [uU] [eE];
CHECK       : [cC] [hH] [eE] [cC] [kK];
DEFAULT     : [dD] [eE] [fF] [aA] [uU] [lL] [tT];
AND         : [aA] [nN] [dD];
OR          : [oO] [rR];

INTEGER     : [iI] [nN] [tT] [eE] [gG] [eE] [rR];
REAL        : [rR] [eE] [aA] [lL];
TEXT        : [tT] [eE] [xX] [tT];
BLOB        : [bB] [lL] [oO] [bB];

EQ  : '=';
NE  : '!=' | '<>';
GE  : '>=';
LE  : '<=';
GT  : '>';
LT  : '<';

STAR   : '*';
DOT    : '.';
COMMA  : ',';
LPAREN : '(';
RPAREN : ')';

NAME   : [a-zA-Z_][a-zA-Z_0-9]*;
STRING : '"' (~["\\] | '\\' .)* '"';
NUMBER : [0-9]+ ('.' [0-9]+)?;
WS     : [ \t\r\n]+ -> skip;