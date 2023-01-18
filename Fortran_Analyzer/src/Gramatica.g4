grammar Gramatica;

/* Opciones, header, memebers */
options{
    language='Java';
    caseInsensitive = true;
}

/* Gramatica */
/************************** I N S T R U C C I O N E S ********************************/
start   : instructions EOF
        ;

instructions   : instruction* block_program
               ;

instruction : subrutina
            | funcion
            ;

/************************** S U B R U T I N A *************************************/
subrutina   : 'subroutine' idi=IDEN '(' list_parameter? ')' 'implicit' 'none' list_decla_param sentences* 'end' 'subroutine' ide=IDEN
            ;

call_subrutina  : 'call' IDEN '(' list_parameter? ')'
                ;

/************************** F U N C I O N *************************************/
funcion : 'function' idi=IDEN '(' list_parameter? ')' 'result' '(' idr=IDEN ')' 'implicit' 'none' list_decla_param sentences* 'end' 'function' ide=IDEN
        ;

/************************** M A I N   P R O G R A M ********************************/
block_program   : 'program' idi=IDEN 'implicit' 'none' sentences* 'end' 'program' ide=IDEN
                ;

/************************** P A R A M E T R O S *************************************/
list_parameter : expr (',' expr)*
                ;

list_decla_param    : decla_parameter*
                    ;

decla_parameter : type ',' 'intent' '(' 'in' ')' '::' IDEN ('(' list_parameter ')')?
                ;

/************************** S E N T E N C I A S *************************************/
sentences   : print
            | declaration
            | assignment
            | call_subrutina
            | array_statement
            | array_assignment2d
            | array_assignment
            | if_statement
            | do_statement
            | dowhile_statement
            | exit_control
            | cycle_control
            ;

/************************** I M P R E S I O N   P R I N T *******************************/
print   : ch1='print' '*' (',' print_parameter)*
        ;

print_parameter : expr      #printexp
                | str=STRING    #printstr
                ;

/************************** D E C L A R A C I O N   D E   V A R I A B L E S *******************/
declaration : type '::' decla_asign (',' decla_asign)*
            ;

decla_asign : IDEN ('=' expr)?
            ;

/************************** A S I G N A C I O N   D E   V A R I A B L E S *********************/
assignment  : IDEN '=' expr
            ;

/************************** T I P O S   D E   D A T O S *************************************/
type    : 'integer'
        | 'real'
        | 'complex'
        | 'character'
        | 'logical'
        ;

/************************** D E C L A R A C I O N   D E   A R R E G L O S *********************/
array_statement : type ',' 'dimension' '(' list_parameter ')' '::' IDEN #arraydimdecla
                | type '::' IDEN '(' list_parameter ')'                 #arraydecla
                ;

/************************** A S I G N A C I O N   D E   A R R E G L O S   2D *********************/
array_assignment2d  : IDEN '[' list_parameter ']' '=' expr
                    ;

/************************** A S I G N A C I O N   D E   A R R E G L O S   1D *********************/
array_assignment    : IDEN '=' '(' '/' list_parameter '/' ')'   #arraylistassig
                    | IDEN '[' list_parameter ']' '=' expr            #arrayposassig
                    ;

/************************** S E N T E N C I A   I F ***********************************/
if_statement    : 'if' '(' expr ')' 'then' stmt_if=sentences* else_if_stmt* ('else' stmt_else=sentences*)? 'end' 'if'
                ;

else_if_stmt    : 'else' 'if' '(' expr ')' 'then' sentences*
                ;

/************************** S E N T E N C I A   D O ***********************************/
do_statement    : 'do' assignment ',' e1=expr (',' e2=expr)? sentences* 'end' 'do'
                ;

/************************** S E N T E N C I A   D O   W H I L E ***********************************/
dowhile_statement   : 'do' 'while' '(' expr ')' sentences* 'end' 'do'
                    ;

/************************** C O N T R O L   D E   C I C L O S   E X I T ****************************/
exit_control    : 'exit'
                ;

/************************** C O N T R O L   D E   C I C L O S   C Y C L E ****************************/
cycle_control    : 'cycle'
                ;

/************************** E X P R E S I O N E S *************************************/
expr    : <assoc=right> op='-' right=expr   #opunaria
        | <assoc=right> op='.not.' right=expr   #opunaria
        | left=expr op='**' right=expr      #opexpr
        | left=expr op=('*'|'/') right=expr #opexpr
        | left=expr op=('+'|'-') right=expr #opexpr
        | left=expr op=oprel right=expr     #opexprrel
        | left=expr op='.and.' right=expr   #opexprlog
        | left=expr op='.or.' right=expr    #opexprlog
        | '(' expr ')'                      #parenexpr
        | atom=NUM                          #atomexpr
        | deci=REAL                         #deciexpr
        | char=CHARACTER                    #charexpr
        | id=IDEN                           #idexpr
        | cmplx=COMPLEX                     #cmplxexpr
        | '.true.'                          #boleanexpr
        | '.false.'                         #boleanexpr
        | idf=IDEN '(' list_parameter? ')'  #funcexpr
        | ida=IDEN '[' list_parameter ']'   #arrayexpr
        ;

oprel   : ope=('=='|'.eq.')
        | ope=('/='|'.ne.')
        | ope=('>'|'.gt.')
        | ope=('<'|'.lt.')
        | ope=('>='|'.ge.')
        | ope=('<='|'.le.')
        ;

/* Tokens */
NUM             : [0-9]+ ;
REAL            : NUM '.' NUM;
COMPLEX         : '(' REAL ',' REAL ')';
CHARACTER       : '\'' ~['\\\r\n] '\''
                | '"' ~["\\\r\n] '"';
IDEN            : [a-zA-Z][a-zA-Z0-9_]* ;
STRING          : '"' ~["\\\r\n]* '"'
                | '\'' ~['\\\r\n]* '\'';
LINE_COMMENT    : '!' ~[\r\n]* -> skip;
WS              : [ \t\r\n]+ -> skip ;