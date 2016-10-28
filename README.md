# fray

The interpreter will only include:
- Controls
- Imports
- Loops
  - do-while, while
  - foreach (for only if necessary)
- Functions
- Expressions
  - [i..j], [i...j]
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


# Todo
- Custom stack traces
- Source maps :)
- Get hashmap of symbols in scope
    - Print like in Node, with curses too
- Tree shaker
    - Just run this through asset pipeline :)
    
## Tree Shaker

*Should remove all unused functions and variables intuitively.*

1. Use a visitor, organize all declarations into scopes
    - Declarations include:
        - Function/Variable declarations (top-level or otherwise)
        - Members declared on classes
    - Each symbol should have a flag, `used`, defaults to `false`.
    - Each symbol should also have a `source` pointing to its declaration.
2. Using another visitor, go through all possible usages
3. Mark symbols as `used` only if they are actually used.
4. Recurse through symbols, and for any that are not used, remove their
declarations. :)

## Static Analysis

1. Detecting usages should be straightforward.
    - Should probably be in a `List<ParserRuleContext> usages`...
    - Zero usages: Suggest removing
    - One usage: Suggest making into a local variable
        - Only suggest this if variable is referenced in a child scope
2. Detect non-existent member references - should also be easy :)