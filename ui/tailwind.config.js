module.exports = {
    purge: {
        enabled: true,
        content: [
            './kotlin/*.js',
        ]
    },
    darkMode: false, // or 'media' or 'class'
    theme: {
        extend: {},
    },
    variants: {
        extend: {
            padding: ['first', 'last'],
            margin: ['first', 'last'],
        },
    },
    plugins: [],
}
