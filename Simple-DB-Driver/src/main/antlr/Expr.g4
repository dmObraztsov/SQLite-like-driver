grammar Expr;

start : expr EOF ;

expr : expr op=OR expr            # Con
     | expr op=AND expr             # Dis
     | INT                         # int
     | '(' expr ')'                # parens
     ;

AND : '&&' ;
OR : '||' ;

INT : [0-1]+ ;
WS : [ \t\r\n]+ -> skip ;