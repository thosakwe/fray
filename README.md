# fray

The interpreter will only include:
- Controls
- Imports
- Loops
  - do-while, while
  - foreach (for only if necessary)
- Functions
- Expressions
  - [i..j]
  - Assignment
  - Function Expressions
  - Identifiers
    - Simple and Prefixed
  - Indexers
  - Invocations
  - Numbers
    - Double
    - Hex
    - Int
    - Long
  - Literals
    - Boolean
    - Dictionary
    - Set
  - Parentheses
  - Operators
    - Unary
      - Prefix: !, ++, --
      - Postfix: ++, --
    - Binary
      - Aritmetic: +, -, *, /, %, ^
      - Assignment: +=, -=, *=, /=, %=, ^=, =
      - Boolean: ==, !=, <, <=, >, >=, &&, ||
  - Strings
    - Raw
    - Simple
- String Interpolation


*All* additional functionality can hopefully be
implemented in Fray, in the form of libraries.
