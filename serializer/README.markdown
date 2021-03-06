# CSEParserish

Parse a Java AST and its associated mutants and output XML.

## Importing into IntelliJ
This should be imported as a Maven project. This should be fairly self
explanatory.

Once everything is imported, go to the maven tab on the right hand side of you
screen, click it, and then click the circular arrows to download dependencies.
This should take a minute or so depending on your connectivity.

## Running
Eventually we'll get the Maven configuration to be easy to run from command
line, but we can run from IntelliJ for now. To do this you will need to create a
new Run Configuration. The _easiest_ way to do this is:

1. Open `serializer.SubjectSerializer.jav` from the left pane in IntelliJ. You can find it
   by navigating the path `CSEParserish/src/main/java/serializer.SubjectSerializer`

2. Navigate to the `main` method in `serializer.SubjectSerializer`

3. Right click the text `main` and select "Run SubjectSerialize.main"

   This will run the `main` method but an error will be reported and the program
   will exit. This is because no arguments have been passed to `main`. To pass
   arguments, you need to edit the configuration that was just generated by
   running the program.  To do this:

4. Click on the dropdown menu on the top of IntelliJ that should now say
   `serializer.SubjectSerializer`, located between the build and run icons, and select the
   "Edit Configurations".

5. This will bring up a new window that has several text boxes: paste

   ```
   ../subjects/exprs/Exprs.java ../subjects/exprs/mutants ../subjects/exprs/mutants.log
   ```
   into the text box labeled **Program Arguments** and click **ok**.
6. Click the **Run** button (green arrow). Everything should run now.



