grammar FrayInterpolation;

SIMPLE_STRING: ((~'%')|('\\%'))+;
ID_STRING: '%' [A-Za-z_] [A-Za-z0-9_]*;
COMPLEX_STRING: '%{' ~('}'|'\n')* '}';

string: interpolationElement*;

interpolationElement:
    SIMPLE_STRING #NoInterpolation
    | ID_STRING #IdentifierInterpolation
    | COMPLEX_STRING #ComplexInterpolation
;