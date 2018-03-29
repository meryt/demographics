var path = require('path');

module.exports = {
    entry: './src/main/js/app.js',
    devtool: 'sourcemaps',
    cache: true,
    output: {
        path: __dirname,
        filename: './src/main/resources/static/built/bundle.js'
    },
    module: {
        rules: [
            {
                test: path.join(__dirname, '.'),
                query: {
                    presets: ['react', 'es2015', 'stage-1']
                },
                loader: "babel-loader"
            }
        ]
    }
};
