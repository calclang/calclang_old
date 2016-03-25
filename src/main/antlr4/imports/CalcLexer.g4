lexer grammar CalcLexer;

WS: (' ' | '\t' | '\r' | '\f')+ -> skip;
SEMI: ';' -> skip;
NEWLINE: '\n'+;

// Symbols
ARROW: '=>';
BRACKET_L: '[';
BRACKET_R: ']';
COMMA: ',';
DOT: '.';
PAREN_L: '(';
PAREN_R: ')';

// Primitives
HEX: '0x' [a-zA-Z0-9]+;
NUMBER: MINUS? [0-9]+ (DOT [0-9]+)?;
SINGLE_LINE_COMMENT : ('//' Input_character*) -> channel(HIDDEN);
fragment Input_character: ~([\u000D\u000A\u0085\u2028\u2029]);
STRING : '"' ~('\r' | '\n' | '"')* '"' ;

// Keywords
ELSE: 'else';
END: 'end';
FN: 'fn';
GLOBAL: 'global';
IF: 'if';
INC: 'inc';
LOOP: 'loop';
PRINT: 'print';
RET: 'ret';

// Boolean
AND: '&&';
EQU: '==';
FALSE: 'false';
GT: '>';
GTE: '>=';
LT: '<';
LTE: '<=';
NOT: '!';
OR: '||';
TRUE: 'true';

// Math
CARET: '^';
EQUALS: '=';
MINUS: '-';
MODULO: '%';
PLUS: '+';
SLASH: '/';
TIMES: '*';

// This should be last
ID: [a-zA-Z] [a-zA-Z0-9_]*;
