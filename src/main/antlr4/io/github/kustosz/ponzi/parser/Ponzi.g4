grammar Ponzi ;

init : expression ;

expression : identifier
           | literal
           | lambdaExpression
           | conditional
           | procedureCall
           ; //todo

literal : quotation | selfEvaluating;
procedureCall : '(' expression expression* ')' ;

selfEvaluating : bool | number; //todo

quotation : '\'' datum
          | '(' 'quote' datum ')'
          ;

conditional : '(' 'if' test=expression ifTrue=expression ifFalse=expression? ')' ;

lambdaExpression : '(' 'lambda' formals body ')' ;
formals : '(' Identifier* ')' ; //todo
body : expression* expression; //todo

datum : simpleDatum | compoundDatum ; //todo

simpleDatum : bool | number | symbol ; //todo

compoundDatum : list | abbreviation ; //todo

list : '(' datum* ')' ; //todo

abbreviation : '\'' datum; //todo

symbol : Identifier ;

bool : '#true' | '#false' | '#t' | '#f'  ;

number : num_10 ; //todo
num_10 : Number_10 ; //todo

identifier : Identifier; // todo

Identifier : Initial Subsequent* ;
Number_10 : '0' | '-'? [1-9] [0-9]* ;

fragment Subsequent : Initial | Digit | SpecialSubsequent ;
fragment Initial : Letter | SpecialInitial ;
fragment Digit : [0-9] ;
fragment Letter : [a-z] | [A-Z] ;
fragment SpecialInitial : '!' | '$' | '%' | '&' | '*' | '/' | ':' | '<' | '=' | '>' | '?' | '^' | '_' | '~' ;
fragment SpecialSubsequent : ExplicitSign | '.' | '@' ;
fragment ExplicitSign : '+' | '-' ;

Whitespace : [ \t\r\n]+ -> skip ;
