function Exception(message) {
    this.message = message;
}

Exception.prototype.str = function() {
    return 'Runtime exception: ' + this.message;
};