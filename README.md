# Cornelius

## Installing Cornelius
Cornelius is written in Rust and is built using `cargo`. Make sure you have
[`cargo` installed](https://doc.rust-lang.org/cargo/getting-started/installation.html).

Once you have `cargo` installed, run `init.sh`. This will build a release
version of Cornelius and set up the `framework/` directory with various
libraries that Cornelius needs for mutation and serialization:

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
- [Serialization](./serialization) serializes a regularized AST into a PEG

## Running Cornelius
Run `./cornelius.sh path/to/File.java`. This will create a temporary directory
and will mutate the class. Then Cornelius will regularize and serialize the
original program and each of its mutants. Once this is done, Cornelius will run
equality saturation on the serialized PEGs.

Each method in the input file will be treated as its own subject.


