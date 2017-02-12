// Todo: continue, break
grammar Fray;

SHEBANG: '#!' ~('\n')* -> channel(HIDDEN);
SL_CMT: ('#' | '//') ~('\n')* -> channel(HIDDEN);
WS: (' ' | '\n' | '\r' | '\r\n') -> skip;

// Symbols
ARROBA: '@';
COLON: ':';
COMMA: ',';
CURLY_L: '{';
CURLY_R: '}';
DOT: '.';
LT: '<';
GT: '>';
PAREN_L: '(';
PAREN_R: ')';
SEMI: ';';
SQUARE_L: '[';
SQUARE_R: ']';
QUESTION: '?';

// Operators
// Todo: precedence
ARROW: '=>';
EQUALS: '=';
ARITHMETIC_OPERATOR: '+' | '-' | '*' | '/' | '%' | '^';
BOOLEAN_OPERATOR: '==' | '!=' | '<=' | '>=' | '&&' | '||';
ASSIGNMENT_OPERATOR: '+=' | '-=' | '*=' | '/=' | '%=' | '^=';
UNARY_OPERATOR: '!' | '++' | '--';

// Keywords
AS: 'as';
CATCH: 'catch';
CLASS: 'class';
CONSTRUCTOR: 'constructor';
DO: 'do';
ELSE: 'else';
EXPORT: 'export';
EXTENDS: 'extends';
FINAL: 'final';
FINALLY: 'finally';
FN: 'fn';
FOR: 'for';
IF: 'if';
IS: 'is';
IMPORT: 'import';
LET: 'let';
NEW: 'new';
NULL: 'null';
OF: 'of';
RET: 'ret';
SUPER: 'super';
THIS: 'this';
THROW: 'throw';
TRY: 'try';
WHILE: 'while';

// Boolean Literals
FALSE: 'false';
TRUE: 'true';

// Numeric Literals
fragment POW10: ('E' | 'e') '-'? [0-9]+;
DOUBLE: '-' ? [0-9]+ '.' [0-9]+ POW10?;
HEX: '0x' [A-F0-9]+;
INT: '-' ? [0-9]+ POW10?;

// String Literals
fragment ESCAPED: '\\\'' | '\\r' | '\\n';
RAW_STRING: 'r\'' (ESCAPED | ~('\n'|'\r'|'\''))*? '\'';
STRING: '\'' (ESCAPED | ~('\n'|'\r'|'\''))*? '\'';

// Identifier - always last
IDENTIFIER: [A-Za-z_] [A-Za-z0-9_]*;

compilationUnit: topLevelDefinition*;

topLevelDefinition:
    emptyDeclaration
    | importDeclaration
    | exportDeclaration
    | topLevelFunctionDefinition
    | topLevelVariableDeclaration
    | classDefinition
    | topLevelStatement
;

emptyDeclaration: SEMI;
importDeclaration: annotations=annotation* IMPORT importOf? source=importSource importAs? SEMI?;
importOf: SQUARE_L ((names+=IDENTIFIER COMMA)* names+=IDENTIFIER COMMA?)? SQUARE_R OF;
importSource: standardImport | expression;
standardImport: LT source=IDENTIFIER GT;
importAs: AS alias=IDENTIFIER;
exportDeclaration: EXPORT importOf? source=importSource;

topLevelFunctionDefinition: functionSignature functionBody SEMI?;
topLevelVariableDeclaration: annotations=annotation* (FINAL | LET) (variableDeclaration COMMA)* variableDeclaration SEMI?;
topLevelStatement: statement;

classDefinition:
    annotations=annotation* CLASS name=IDENTIFIER (EXTENDS superClass=expression)? CURLY_L (constructorDefinition | topLevelFunctionDefinition | topLevelVariableDeclaration)* CURLY_R
;

constructorDefinition: annotations=annotation* CONSTRUCTOR (name=IDENTIFIER)? functionBody;

functionSignature: annotations=annotation* FN name=IDENTIFIER;
annotation: ARROBA target=expression;

functionBody: parameters (blockBody | expressionBody);
parameters: names+=IDENTIFIER | (PAREN_L ((names+=IDENTIFIER COMMA)* names+=IDENTIFIER)? COMMA? PAREN_R);
blockBody: block;
expressionBody: ARROW expression;

block: (statement SEMI?) | (CURLY_L (statement SEMI?)* CURLY_R);

statement:
    SEMI #EmptyStatement
    | expression #ExpressionStatement
    | annotations=annotation* (FINAL | LET) (variableDeclaration COMMA)* variableDeclaration #VariableDeclarationStatement
    | RET expression #ReturnStatement
    | THROW expression #ThrowStatement
    | FOR PAREN_L (FINAL|LET)? as=IDENTIFIER COLON in=expression PAREN_R block #ForStatement
    | main=ifBlock (ELSE elseIf+=ifBlock)* elseBlock? #IfStatement
    | WHILE PAREN_L condition=expression PAREN_R block #WhileStatement
    | DO block WHILE PAREN_L condition=expression PAREN_R #DoWhileStatement
    | TRY tryBlock=block ((catchBlock finallyBlock?) | (catchBlock? finallyBlock) | (catchBlock finallyBlock))? #TryStatement
;

catchBlock: CATCH PAREN_L name=IDENTIFIER PAREN_R block;
finallyBlock: FINALLY block;
ifBlock: IF PAREN_L condition=expression PAREN_R block;
elseBlock: ELSE block;

// controlFlowStatement: ;
variableDeclaration: name=IDENTIFIER (assignmentOperator expression)?;

expression:
    IDENTIFIER #IdentifierExpression
    | (DOUBLE | HEX | INT) #NumericLiteralExpression
    | string #StringLiteralExpression
    | (TRUE | FALSE) #BooleanLiteralExpression
    | NULL #NullLiteralExpression
    | THIS #ThisExpression
    | SUPER DOT member=IDENTIFIER #SuperExpression
    | NEW type=expression PAREN_L ((args+=expression COMMA)* args+=expression)? COMMA? PAREN_R #NewExpression
    | expression DOT IDENTIFIER #MemberExpression
    | left=expression assignmentOperator right=expression #AssignmentExpression
    | left=expression binaryOperator right=expression #BinaryExpression
    | candidate=expression IS type=expression #IsExpression
    | unaryOperator expression #UnaryPrefixExpression
    | expression unaryOperator #UnaryPostfixExpression
    | condition=expression QUESTION yes=expression COLON no=expression #TernaryExpression
    | callee=expression PAREN_L ((args+=expression COMMA)* args+=expression)? COMMA? PAREN_R #InvocationExpression
    | SQUARE_L lower=expression DOT DOT upper=expression SQUARE_R #InclusiveRangeExpression
    | SQUARE_L lower=expression DOT DOT DOT upper=expression SQUARE_R #ExclusiveRangeExpression
    | target=expression SQUARE_L index=expression SQUARE_R #SetIndexerExpression
    | SQUARE_L ((expression COMMA?)* expression)? COMMA? SQUARE_R #SetLiteralExpression
    | CURLY_L ((pairs+=dictionaryKeyValuePair COMMA)* pairs+=dictionaryKeyValuePair)? COMMA? CURLY_R #DictionaryLiteralExpression
    | functionBody #FunctionExpression
    | PAREN_L expression PAREN_R #ParenthesizedExpression
;

dictionaryKeyValuePair: key=expression COLON value=expression;

string:
    STRING #SimpleString
    | RAW_STRING #RawString
;

assignmentOperator: EQUALS | ASSIGNMENT_OPERATOR;
unaryOperator: UNARY_OPERATOR | UNARY_OPERATOR;
binaryOperator: ARITHMETIC_OPERATOR | LT | GT | BOOLEAN_OPERATOR;