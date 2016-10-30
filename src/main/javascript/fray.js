import antlr4 from 'antlr4';

export default class Fray {
    /**
     * Interpreter/compiler with an emphasis on code transformation and reflection.
     * @constructor
     */
    constructor() {}

    /**
     * Compiles a string of Fray code into JavaScript code.
     * @param text {String}
     */
    compile(text) {
        // Todo: Just call the Java CLI, make a Gulp plugin :)
        var chars = new antlr4.InputStream(text);
        var lexer = new FrayLexer(chars);
        var tokens  = new antlr4.CommonTokenStream(lexer);
        var parser = new FrayParser(tokens);
        parser.buildParseTrees = true;
        return parser.compilationUnit();
    }
}