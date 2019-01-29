
Lenguajes de alto nivel como Java incluyen un *garbage collector* que evita
todo tipo de leaks lo que hace la programación mas llevadera.

Aunque es verdad en cierto grado, es un mito que pinta un escenario demasiado
feliz, libre de problemas, que no es real.

Considere el siguiente ejemplo de Java:

```java
$ cat FdLeak.java
<...>
public class FdLeak {
    public static void main(String args[]) {
        for (int i = 0; i < 2048; ++i) {
            try {
                FileInputStream istream = new FileInputStream("FdLeak.java");
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}
```

El programa abre reiteradas veces el mismo archivo creando múltiples
objetos ``FileInputStream istream`` sin cerrarlos explícitamente.

Al no ser guardados en ningun otro lado, cada iteración del ``for`` crea
un *nuevo* objeto y *desreferencia* al anterior convirtiendolo en basura
o memoria sin liberar.

El *garbage collector* podrá entonces reclarmar y liberar la memoria.

Sin embargo la memoria no es el único recurso:
al abrir un archivo el sistema operativo reserva estructuras que también
deben ser liberadas.

Estas no pueden ser vistas directamente por encontrarse en el espacio de
memoria del *kernel* pero son referenciadas a traves de un identificador
conocido como *file descriptor*.

Por cuestiones de seguridad el sistema operativo permite poner límites en la
cantidad máxima de *file descriptors* que un programa puede tener llamando
a ``ulimit``.

Por ejemplo, podemos limitar a un máximo de 2048 *file descriptors*
ejecutando:

```shell
$ ulimit -Hn 2048
```

Veamos entonces como se comporta nuestro programa en Java:

```shell
$ javac FdLeak.java

$ java -Xmn512m -Xmx700m -verbose:gc FdLeak
java.io.FileNotFoundException: FdLeak.java (Too many open files)
<...>
```

Por qué el programa no cierra los archivos y falla con una excepción?

Java implementa un *garbage collector generacional*: segmenta el espacio
de memoria en dos con limites 512 MB y 700 MB, corresponsientes a las
opciones de ejecución ``-Xmn512m`` y ``-Xmx700m``.

Los *allocs* de memoria se realizan en el primer segmento
(conocido como *nursery*) y cuando este se llena se ejecuta
una recolección parcial: todos los objetos basura son liberados
y los que no son movidos al segundo segmento.

Pero en nuestro caso la mayoría de los objetos creados son meros objetos
``FileInputStream`` que ocupan poca memoria.

Al no llenarse el primer segmento de 512 MB, el *garbage collector*
nunca se ejecuta y por lo tanto nunca cierra los archivos **ni libera
los _file descriptors_** lo que termina en un error.

En cambio, si ejecutamos reservando 512 KB en vez de 512 MB vemos
que el programa termina correctamente:

```shell
$ java -Xmn512k -Xmx700m -verbose:gc FdLeak
[GC (Allocation Failure)  <...>]
<...>
```

Los mensajes ``GC (Allocation Failure)`` son mensajes de *debug* debido
a la opción adicional ``-verbose:gc`` y nos indican que el
*garbage collector* fue ejecutado, lo que implica el cierre de los
archivos y por lo tanto el programa nunca alcanza el límite de los
2048 *file descriptors*.

Efectivamente un *garbage collector* se encarga de reclamar y liberar
la memoria automáticamente por lo que evitan *memory leaks* pero no
consideran **otros tipos de recursos**, incluso mas importantes que la memoria.

