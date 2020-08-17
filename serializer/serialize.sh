
classpath="target/classes:$HOME/.m2/repository/com/github/javaparser/javaparser-core/3.15.11/javaparser-core-3.15.11.jar"

# Serialize Misc
dir="../subjects/misc"
name="Misc.java"
echo
echo "Serializing $name to ../cornelius/xmls/misc.xml..."
echo
echo "Timing for $name:"
time java -classpath "$classpath" SubjectSerializer $dir/$name $dir/mutants $dir/mutants.log > /dev/null
mv subjects.xml ../cornelius/xmls/misc.xml

echo
echo "------------------"

#Serialize Exprs
dir="../subjects/exprs"
name="Exprs.java"
echo
echo
echo "Serializing $name to ../cornelius/xmls/exprs.xml"
echo
echo "Timing for $name:"
time java -classpath "$classpath" SubjectSerializer $dir/$name $dir/mutants $dir/mutants.log > /dev/null
mv subjects.xml ../cornelius/xmls/exprs.xml

echo
echo "------------------"

#Serialize TriangleExpr
dir="../subjects/triangle-expr"
name="TriangleExpr.java"
echo
echo
echo "Serializing $name to ../cornelius/xmls/triangle-expr.xml"
echo "Timing for $name:"
time java -classpath "$classpath" SubjectSerializer $dir/$name $dir/mutants $dir/mutants.log > /dev/null
mv subjects.xml ../cornelius/xmls/triangle-expr.xml
