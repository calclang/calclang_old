grammar CalcLang;
import CalcLexer;

@header {
    import java.util.*;
    import java.lang.*;
}
// Parser
compilationUnit locals [
    Map<String, FnblockContext> functions = new HashMap<String, FnblockContext>(),
    Map<String, ExprContext> globals = new HashMap<String, ExprContext>()
]
: NEWLINE* (importstmt NEWLINE+)* ((codeline NEWLINE+)* codeline)? NEWLINE*;

codeline: globalassignstmt | block;
importstmt: INC STRING;

block locals [
    Map<String, ExprContext> symbols = new HashMap<String, ExprContext>(),
    ExprContext returnValue = null,
    Boolean execute = false,
    Boolean hasReturned = false
]
: fnblock | inlinefnblock;

inlinefnblock: FN name=ID params ARROW expr;
fnblock:
    FN name=ID params (NEWLINE stmts)? NEWLINE+ END lastName=ID { $name.text.equals($lastName.text) }?
    { $compilationUnit::functions.put($name.text.trim(), _localctx); };
params: PAREN_L ((ID COMMA)* ID)? PAREN_R;

ifstmt: IF PAREN_L condition PAREN_R (NEWLINE+ stmts)? NEWLINE+ END IF;

condition:
    expr
    | expr EQU expr | expr NOT expr
    | expr LT expr | expr LTE expr
    | expr GT expr | expr GTE expr
    | expr AND expr | expr OR expr
;

stmts: ((stmt NEWLINE+)* stmt);
stmt: printstmt | assignstmt | callstmt | ifstmt | decstmt | incstmt | loopstmt | opstmt | returnstmt;
assignstmt: ID EQUALS expr { $block::symbols.put($ID.getText().trim(), _localctx.expr()); };
callstmt:
    function=ID expr
    | function=ID (expr COMMA)* expr
    | function=ID PAREN_L (expr COMMA)* expr PAREN_R
;
decstmt: PLUS PLUS ID | MINUS MINUS ID;
globalassignstmt: GLOBAL ID EQUALS expr { $compilationUnit::globals.put($ID.getText().trim(), _localctx.expr()); };
incstmt: ID PLUS PLUS | ID MINUS MINUS;
loopstmt: LOOP expr (NEWLINE+ stmts)? NEWLINE+ END LOOP;
opstmt: ID operator EQUALS expr;
printstmt:
    PRINT (STRING|expr|condition)
    | PRINT PAREN_L (STRING|expr|condition) PAREN_R
;
returnstmt: RET expr {
    if ($block::returnValue == null) {
        $block::returnValue = _localctx.expr();
    }
};

expr:
    HEX
    | NUMBER
    | TRUE
    | FALSE
    | PLUS PLUS ID | MINUS MINUS ID
    | ID PLUS PLUS | ID MINUS MINUS
    | function=ID expr
    | function=ID (expr COMMA)* expr
    | function=ID PAREN_L (expr COMMA)* expr PAREN_R
    | expr operator expr
    | ID
    | PAREN_L expr PAREN_R;

operator: CARET | MODULO | TIMES | SLASH | PLUS | MINUS;

