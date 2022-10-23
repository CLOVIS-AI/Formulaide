(() => {
    config.devServer = Object.assign(
        {},
        config.devServer || {},
        {server: "https"}
    );
})();
