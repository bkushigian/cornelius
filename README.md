```
:'######:::'#######::'########::'##::: ##:'########:'##:::::::'####:'##::::'##::'######::
'##... ##:'##.... ##: ##.... ##: ###:: ##: ##.....:: ##:::::::. ##:: ##:::: ##:'##... ##:
 ##:::..:: ##:::: ##: ##:::: ##: ####: ##: ##::::::: ##:::::::: ##:: ##:::: ##: ##:::..::
 ##::::::: ##:::: ##: ########:: ## ## ##: ######::: ##:::::::: ##:: ##:::: ##:. ######::
 ##::::::: ##:::: ##: ##.. ##::: ##. ####: ##...:::: ##:::::::: ##:: ##:::: ##::..... ##:
 ##::: ##: ##:::: ##: ##::. ##:: ##:. ###: ##::::::: ##:::::::: ##:: ##:::: ##:'##::: ##:
. ######::. #######:: ##:::. ##: ##::. ##: ########: ########:'####:. #######::. ######::
:......::::.......:::..:::::..::..::::..::........::........::....:::.......::::......:::
```

# Cornelius
Cornelius is an equivalent mutant detection framework for Java. Written in Rust, 
Cornelius uses the [egg](https://github.com/mwillsey/egg) Egraph framework to
efficiently detect equivalent and redundant mutants.

## Installing Cornelius
Cornelius is built using `cargo`. Make sure you have [`cargo`
installed](https://doc.rust-lang.org/cargo/getting-started/installation.html).

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
To invoke the Cornelius framework on a file, run `./cornelius.sh
path/to/File.java`. This will:
1. create a temporary directory
2. invoke Major to mutate the Java file
3. regularize and serialize the original program and each of its mutants
4. run a rewrite system to detect equivalent and redundant mutants
5. output discovered equivalence classes to disk

Each method in the input file is treated as its own subject.

## Sample Inputs
Cornelius has several sample input programs in `serializer/tests/subjects/`.
For instance, running 


