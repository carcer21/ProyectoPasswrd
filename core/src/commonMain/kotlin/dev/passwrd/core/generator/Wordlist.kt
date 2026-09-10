package dev.passwrd.core.generator

/**
 * Lista corta embebida para passphrases estilo Diceware. No pretende ser la lista EFF
 * completa (7776 palabras) — con 128 palabras y 5 términos ya da ~35 bits, suficiente
 * como opción "fácil de teclear" junto al generador de password aleatoria (fuente real de
 * entropía en la mayoría de casos).
 */
internal val WORDLIST = listOf(
    "abrir", "acero", "actor", "agua", "aire", "ala", "alba", "alce", "alfil", "alga",
    "alma", "alto", "ancla", "andar", "anexo", "anillo", "arbol", "arco", "arena", "arpa",
    "atlas", "atomo", "audaz", "aula", "auto", "avion", "azul", "banco", "barco", "base",
    "bici", "boca", "bosque", "brazo", "brisa", "bruma", "cable", "campo", "canoa", "carbon",
    "carta", "casa", "cedro", "cera", "cesta", "cielo", "cifra", "cine", "circo", "clave",
    "clima", "cobre", "coche", "codo", "cofre", "coral", "corcho", "cresta", "cripta", "cubo",
    "danza", "dardo", "delta", "diente", "disco", "duna", "eco", "elipse", "enero", "enigma",
    "erizo", "escudo", "esfera", "esfinge", "espiga", "estrella", "faro", "feria", "fibra", "flauta",
    "flecha", "fondo", "fuego", "gaita", "gala", "garra", "gato", "gema", "geo", "girasol",
    "globo", "grano", "grieta", "grifo", "grillo", "hacha", "hebra", "hiedra", "hilo", "horno",
    "huella", "humo", "iman", "indice", "isla", "jarra", "jazmin", "joya", "jungla", "lago",
    "lanza", "libro", "linea", "lince", "llave", "lluvia", "lobo", "luna", "luz", "madera",
    "malla", "manto", "mapa", "mar", "marco", "matiz", "menta", "mesa", "metal", "mineral",
)
