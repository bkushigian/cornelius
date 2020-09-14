```
:'######:::'#######::'########::'##::: ##:'########:'##:::::::'####:'##::::'##::'######::
:##... ##:'##.... ##: ##.... ##: ###:: ##: ##.....:: ##:::::::. ##:: ##:::: ##:'##... ##:
:##:::..:: ##:::: ##: ##:::: ##: ####: ##: ##::::::: ##:::::::: ##:: ##:::: ##: ##:::..::
:##::::::: ##:::: ##: ########:: ## ## ##: ######::: ##:::::::: ##:: ##:::: ##:. ######::
:##::::::: ##:::: ##: ##.. ##::: ##. ####: ##...:::: ##:::::::: ##:: ##:::: ##::..... ##:
:##::: ##: ##:::: ##: ##::. ##:: ##:. ###: ##::::::: ##:::::::: ##:: ##:::: ##:'##::: ##:
:'######::. #######:: ##:::. ##: ##::. ##: ########: ########:'####:. #######::. ######::
:......::::.......:::..:::::..::..::::..::........::........::....:::.......::::......:::
```

# Cornelius
Cornelius is an equivalent mutant detection framework for Java. Cornelius uses
the [egg](https://github.com/mwillsey/egg) Egraph framework to efficiently
detect equivalent and redundant mutants. Cornelius is currently outfitted to
work automatically with the [Major mutation
framework](https://mutation-testing.org/).

## Installing Cornelius
To install Cornelius you'll need an up to date version of
[`cargo`](https://doc.rust-lang.org/cargo/getting-started/installation.html) and
Java 8 (Java 8 is required for Major which generates mutants. If mutants are
already generated, you can ignore this dependency).

Once you have `cargo` installed, run `./init.sh` from the root of the directory.
This will build a release version of Cornelius and set up the `framework/`
directory with various libraries that Cornelius needs for mutation and
serialization:

- The [Major mutation framework](https://mutation-testing.org): Cornelius
  uses Major to mutate Java sources
- [Java AST Regularizer](https://github.com/bkushigian/ast-regularizer): This
  _regularizes_ a Java AST. This means that control flow is simplified so that

  * There is exactly one return per method
  * There are no break or continue statements
  * Null checks are done explicitly
  
  So far not all of these are implemented in the regularizer yet (but will be
  soon). Also, Cornelius will eventually do implicit regularization since
  explicitly constructing the AST seems to be expensive.
- [Serialization](./serialization) serializes a regularized AST into a PEG and
  outputs it to a file

## Running Cornelius
To invoke the Cornelius framework on a Java source file, run
`JAVA_HOME=/path/to/java-8-home ./cornelius.sh path/to/File.java`. This will:

1. create a temporary directory
2. invoke Major to mutate the Java file
3. regularize and serialize the original program and each of its mutants
4. run a rewrite system to detect equivalent and redundant mutants
5. output discovered equivalence classes to disk

Each method in the input file is treated as its own subject.

## Sample Inputs
Cornelius has several sample input programs in `serializer/tests/subjects/`.

``` sh
$ ./cornelius.sh serializer/tests/subjects/triangle/Triangle.java
Created temp working directory /var/folders/90/zz7ry2qd3m12wlsvjwr37kvh0000gn/T/cornelius-.2fh5oVQV
Generating mutants for /private/var/folders/90/zz7ry2qd3m12wlsvjwr37kvh0000gn/T/cornelius-.2fh5oVQV/Triangle.java
Generated 116 mutants (147 ms)
Regularizing subject /private/var/folders/90/zz7ry2qd3m12wlsvjwr37kvh0000gn/T/cornelius-.2fh5oVQV/Triangle.java to /private/var/folders/90/zz7ry2qd3m12wlsvjwr37kvh0000gn/T/cornelius-.2fh5oVQV/regularized

....................................................................................................................
================================================================================
[+] Visiting method signature: Triangle@classify(int,int,int)
[+] Source file: Triangle.java
[+] Name: classify(int,int,int)
Done creating XML File at: /Users/benku/Projects/cornelius/framework/scripts/subjects.xml
Serialized subjects file: /var/folders/90/zz7ry2qd3m12wlsvjwr37kvh0000gn/T/cornelius-.2fh5oVQV/Triangle.xml
    Finished release [optimized] target(s) in 0.23s
Reading from path /var/folders/90/zz7ry2qd3m12wlsvjwr37kvh0000gn/T/cornelius-.2fh5oVQV/Triangle.xml
Equiv Classes:
Writing equivalence classes to /var/folders/90/zz7ry2qd3m12wlsvjwr37kvh0000gn/T/cornelius-.2fh5oVQV/equiv-classes:
    /var/folders/90/zz7ry2qd3m12wlsvjwr37kvh0000gn/T/cornelius-.2fh5oVQV/equiv-classes/Triangle@classify(int,int,int).equiv-class
base: /Users/benku/Projects/cornelius
Linking to equivalence classes: /Users/benku/Projects/cornelius/Triangle-equiv-classes
```

To list the discovered equivalence classes, run:

``` sh
$ cat Triangle-equiv-classes/Triangle@classify\(int,int,int\).equiv-class | grep " "
21 22 24 25
105 111 113 116
91 97 99
48 54 58 64 68
77 83 85
28 32
35 39
3 6 10 13 17
```

Each of these numbers corresponds to the _mutant id_ generated by Major, and
each line corresponds to a set of mutants discovered to be equivalent to one
another. Thus, the first line means that mutants 21, 22, 23, and 24 are all
semantically equivalent to one another. This means that Cornelius has discovered
them to be in the same _redundancy class_.

A mutant id of 0 is reserved for the original program: the set of discovered
equivalent mutants are those mutants on the same line as the original program's
mutant id, `0`. The above run did not discover any equivalent mutants.

Triangle, as well as several of the other programs in
`serializer/tests/subjects/`, have ground truth computed by hand in a `gt` file.
Triangle's ground truth is:

``` sh
$ cat serializer/tests/subjects/triangle/gt | grep " "
0 40 69 75 104
103 115
105 107 108 111 113 116
21 22 24 25
23 77 79 80 83 85 88
28 29 32
3 6 10 13 17
35 38 39
42 44 45 48 50 51 54 58 60 61 64 68 70
73 74
76 87
91 93 94 97 99 102
```

