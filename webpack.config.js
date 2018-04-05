const path = require('path');

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
                test: /\.scss$/,
                use: [
                    {
                        loader: 'style-loader', // inject CSS to page
                    },
                    {
                        loader: 'css-loader', // translate CSS into CommonJS modules
                    },
                    {
                        loader: 'postcss-loader', // run post-css actions
                        options: {
                            plugins: function () { // post-css plugins, can be exported to postcss.config.js
                                return [
                                    require('precss'),
                                    require('autoprefixer')
                                ];
                            }
                        }
                    },
                    {
                        loader: 'sass-loader', // compile Sass to CSS
                        options: {
                            includePaths: ["src/main/scss"]
                        }
                    }
                ]
            },
            {
                test: /\.js$/,
                exclude: /node_modules/,
                query: {
                    presets: ['react', 'es2015', 'stage-1']
                },
                loader: "babel-loader"
            }
        ]
    }
};
