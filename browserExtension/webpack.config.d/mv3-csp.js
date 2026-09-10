// MV3 prohíbe 'unsafe-eval' en el CSP del service worker/popup. El devtool 'eval' por
// defecto del build de desarrollo de Kotlin/Wasm envuelve cada módulo en eval(...) para
// generar source maps rápido — rompe la extensión con "EvalError: Evaluating a string as
// JavaScript violates the CSP". 'source-map' genera un .map aparte, sin eval en runtime.
config.devtool = 'source-map';
