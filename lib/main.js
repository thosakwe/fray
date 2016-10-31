'use babel';
import childProcess from 'child_process';
import {CompositeDisposable} from 'atom';
import packageConfig from './config-schema.json';

const Fray = {
    config: packageConfig,

    activate(state) {
        this.props = atom.config.get('atom-fray') || {
                compilerPath: 'fray',
                port: 0,
                enableVerboseCompilerOutput: false
            };

        this.subscriptions = new CompositeDisposable();

        this.subscriptions.add(
            atom.commands.add('atom-workspace', {
                'atom-fray:compile-js': this.compileJs.bind(this)
            })
        );

        this.analyzer = childProcess.spawn(this.props.compilerPath, [
            `--port=${this.props.port}`,
            this.props.enableVerboseCompilerOutput ? '--verbose' : ''
        ]);

        this.analyzer.on('close', () => {
            this.analyzer = null;
        });

        this.analyzer.on('error', e => {
            console.error(e);
            atom.notifications.addError(`Could not start Fray analyzer at path '${this.props.compilerPath}'.`);
            this.analyzer = null;
        });
    },

    deactivate() {
        if (this.analyzer != null) {
            this.analyzer.kill();
        }
    },

    compileJs() {
        // Todo: Compile JS
        var editor = atom.workspace.getActivePaneItem();

        if (editor) {
            var file = editor.buffer.file;

            if (file) {
                var filePath = file.path;
                var newFile = filePath.replace(/\.fray$/, '.js');
                var compiler = childProcess.spawn(this.props.compilerPath, [
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
            }
        }
    }
};

export default Fray;
