# Changelog

## Wednesday, February 26, by Ben

* Added some language features to `SimpleLanguage`
* Added rewrite rules
* Moved `structs.rs` to `exprs.rs` since these are all data structures related
  to our `expr` sublanguage
* Refactored stuff out of `main.rs` to `exprs.rs`
* Cleaned up code a bit
* Renamed this file to `CHANGELOG.markdown`

## Tuesday, Feb 25, by Yihong

* Add `subjects.xml` under `xmls/` to test the xml file format works.
* Refactor the parser, encapsulate `Subject` and other datatypes under `structs.rs`.
* `id` of `Subject` now is `u32`, instead of `String`.
* There is now two factory method for `Subject`: `make` is the previous `make_subject`, `from_file` parses the xml file to read an array of `Subjects`.
