# fray

The interpreter willy include:
- Control flow
    - break
    - continue
- Private variables
- Functions should be closures
- Expressions
  - Assignment
  - Identifiers
    - Simple and Prefixed
  - Indexers
  - Numbers
    - Long
  - Literals
    - Dictionary
  - Operators
    - Unary
      - Prefix: !, ++, -- (turn the latter two into += and -= with transformer)
      - Postfix: ++, --
    - Binary
      - Arithmetic: -, /, ^
      - Boolean: !=, <, <=, >, >=
  - Strings
    - Raw
    - Escaping
- Imported and exported need to use separate symbol tables...
    - Also, it's not properly exporting top-level variables :(
- Hoist functions


*All* additional functionality can hopefully be
implemented in Fray, in the form of libraries.


# Todo
- Custom stack traces
- Source maps :)
- Get hashmap of symbols in scope
    - Print like in Node, with curses too
- Tree shaker
    - Just run this through asset pipeline :)
- Access members through `getField` instead of directly touching symbol table
    - `Extensible` shim
    
## Dead Code Removal

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
5. Suggestions
    - Defined/updated (count updates) but not accessed
    - final definitions of null are redundant
    - Dict/Set never queried
6. Safe delete :)

## Static Analysis

1. Detecting usages should be straightforward.
    - Should probably be in a `List<ParserRuleContext> usages`...
    - Zero usages: Suggest removing
    - One usage: Suggest making into a local variable
        - Only suggest this if variable is referenced in a child scope
2. Detect non-existent member references - should also be easy :)

## Compiler

1. Tree-shake
    - This means build a new file, only including what we used
        - Not just removing dead code
        - Should be relatively easy for Fray following dead code removal :)
2. Transform libraries into static objects
    - Use an `Extensible`
        - Perhaps include a variant that sets everything `final`?
3. Minify
    - Rename all identifiers to the shortest available variant
4. Transpile
    - Use a visitor
    
### Compile to JS?

foo.fray

```fray
class Foo {
    let bar;
    
    constructor(bar) {
        this.bar = bar;
    }
    
    fn greet() => 'Hello, %{this.bar}!';
}

fn main() => new Foo('world').greet();
```

foo.js (ES5)

```js
(function() {
    function Foo(bar) {
        this.bar = bar;
    }
    
    Foo.prototype.greet = function() {
        return 'Hello, ' + this.bar + '!';
    }
    
    function main() {
        new Foo('world').greet();
    }
    
    return main([]);
})();

```

foo.js (ES6)

```js
class Foo {
    constructor(bar) {
        this.bar = bar;
    }
    
    greet() {
        return `Hello, ${bar}!`;
    }
}

function main() {
    new Foo('world').greet();
}

main([]);
```

ES6 compilation would be nice, but wouldn't work in the browser out-of-the-box.