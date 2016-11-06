'use babel';
import childProcess from 'child_process';
import {CompositeDisposable} from 'atom';
import packageConfig from './config-schema.json';
import net from 'net';

// Todo: Run in interpreter...
const Fray = {
    config: packageConfig,

    activate(state) {
        this.getProps();
        this.subscriptions = new CompositeDisposable();

        this.subscriptions.add(
            atom.commands.add('atom-workspace', {
                'atom-fray:compile-js': this.compileJs.bind(this),
                'atom-fray:start-analyzer': this.startAnalyzer.bind(this)
            })
        );

        this.startAnalyzer();
    },

    deactivate() {
        if (this.analyzer != null) {
            this.analyzer.kill();
        }
    },

    compileJs() {
        // Todo: Enable other languages eventually...
        var editor = atom.workspace.getActivePaneItem();
        var file = editor.buffer.file;
        var filePath = file.path;
        var newFile = filePath.replace(/\.fray$/, '.js');
        var compiler = childProcess.spawn(this.getProps().compilerPath, [
            '--compile=js',
            this.props.enableVerboseCompilerOutput ? '--verbose' : '',
            `--out=${newFile}`,
            filePath
        ]);

        var success = true;

        compiler.on('close', () => {
            if (success)
                atom.notifications.addSuccess(`Successfully compiled to ${newFile}.`);
        });

        compiler.on('error', e => {
            success = false;
            atom.notifications.addError(`Could not start Fray compiler at path '${this.props.compilerPath}'.`);
        });
    },

    getProps() {
        return this.props = atom.config.get('atom-fray') || {
                compilerPath: 'fray',
                port: 0,
                enableVerboseCompilerOutput: false
            };
    },

    provide() {
        const self = this;

        return {
            selector: '.source.fray',
            disableForSelector: '.source.fray .comment',
            inclusionPriority: 1,
            excludeLowerPriority: true,

            getSuggestions({editor, bufferPosition, scopeDescriptor, prefix, activatedManually}) {
                return new Promise((resolve, reject) => {
                    var editor = atom.workspace.getActivePaneItem();
                    var file = editor.buffer.file;
                    var filePath = file.path;

                    const parseSuggestions = (buf, flag) => {
                        const str = buf.toString();
                        const suggestions = [];

                        for (const line of str.split('\n')) {
                            if (line.length) {
                                const match = /([A-Za-z][A-Za-z0-9_]*):([A-Za-z][A-Za-z0-9_]*)/.exec(line);

                                if (match) {
                                    console.log(`Completion option: ${match[0]}`);
                                    const suggestion = {text: match[1].trim(), type: 'variable'};

                                    // Todo: https://github.com/atom/autocomplete-plus/wiki/Provider-API
                                    const type = match[2].trim();

                                    if (type === 'Function') {
                                        suggestion.type = 'function';
                                    } else if (type === 'Type') {
                                        suggestion.type = 'type';
                                    }

                                    suggestions.push(suggestion);
                                }
                            }
                        }

                        if (flag !== false)
                            return resolve(suggestions);
                        else return suggestions;
                    };

                    if (self.analyzer) {
                        // Get completion from the server
                        const socket = new net.Socket();

                        socket.connect(self.getProps().port, () => {
                            socket.write(34);
                            socket.write(bufferPosition.row);
                            socket.write(bufferPosition.column);
                            socket.write(filePath);
                            socket.write('\n');
                        });

                        socket.on('data', buf => {
                            const suggestions = parseSuggestions(buf, false);
                            socket.destroy();
                            return resolve(suggestions);
                        });

                        socket.on('error', (e) => {
                            atom.notifications.addError('Could not obtain code completion information from analysis server.');
                            return reject(e);
                        });
                    }
                    else {
                        // Call executable
                        childProcess.exec(
                            `${self.getProps().compilerPath} --row=${bufferPosition.row} --column=${bufferPosition.column} --code-completion=${filePath}`,
                            (err, stdout) => {
                                if (err) {
                                    atom.notifications.addError('Could not obtain code completion information from Fray executable.');
                                    return reject(err);
                                } else return parseSuggestions(stdout);
                            });
                    }
                });
            }
        };
    },

    startAnalyzer() {
        if (this.analyzer) {
            try {
                this.analyzer.kill();
            } catch (e) {
                atom.notifications.addError('Could not kill existing Fray analyzer process.');
                return;
            }
        }

        this.analyzer = childProcess.spawn(this.props.compilerPath, [
            `--port=${this.props.port}`,
            this.props.enableVerboseCompilerOutput ? '--verbose' : ''
        ]);

        let nData = 0;

        this.analyzer.stdout.on('data', () => {
            if (nData++ <= 0) {
                atom.notifications.addSuccess(`Fray analysis server listening on port ${this.getProps().port}`);
            }
        });

        this.analyzer.on('close', () => {
            this.analyzer = null;
        });

        this.analyzer.on('error', e => {
            console.error(e);
            atom.notifications.addError(`Could not start Fray analyzer at path '${this.props.compilerPath}'.`);
            this.analyzer = null;
        });
    }
};

export default Fray;
